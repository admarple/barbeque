package org.admarple.barbeue.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretException;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;
import org.admarple.barbeque.client.SecretClient;
import org.admarple.barbeque.client.s3.S3SecretClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.UUID;

@Slf4j
public class RotateSecretHandler implements RequestHandler<RotateSecretRequest, RotateSecretResponse>,
        RequestStreamHandler {
    private ObjectMapper mapper;
    private AmazonS3 amazonS3;
    private DynamoDBMapper dynamoDBMapper;
    private LockManager lockManager;
    private SecretClient secretClient;

    @Override
    public RotateSecretResponse handleRequest(RotateSecretRequest request, Context context) {
        String lambdaId = UUID.randomUUID().toString();

        // lock on the new version
        Lock lock = lockManager().getLock(request.getSecretId(), lambdaId, context.getRemainingTimeInMillis() / 1000);
        try {
            log.info("Checking that the secret exists, or the first version is being created");
            SecretMetadata existingSecretMetadata = checkAndGetExistingMetadata(request);
            checkTypes(request, existingSecretMetadata);
            VersionMetadata oldVersionMetadata = checkAndGetExistingVersionMetadata(request);
            checkNotYetRotated(request.getSecretId(), request.getVersion());

            log.info("Writing new secret");
            VersionMetadata newVersionMetadata = request.getVersionMetadata();
            secretClient().putSecret(existingSecretMetadata, newVersionMetadata, request.getNewSecret());

            log.info("Updating expiration on previous version");
            Secret oldSecret = secretClient().fetchSecret(existingSecretMetadata, oldVersionMetadata);
            oldVersionMetadata.setExpiration(newVersionMetadata.getActivation().plusSeconds(request.getOverlapSeconds()));
            secretClient().putSecret(existingSecretMetadata, oldVersionMetadata, oldSecret);

            log.info("Making new version the current version");
            existingSecretMetadata.setCurrentVersion(request.getVersion());
            secretClient().putMetadata(existingSecretMetadata);
        } finally {
            lockManager().release(lock);
        }

        return new RotateSecretResponse();
    }

    private SecretMetadata checkAndGetExistingMetadata(RotateSecretRequest request) {
        SecretMetadata existingSecretMetadata = secretClient().fetchMetadata(request.getSecretId());
        if (existingSecretMetadata == null) {
            if (isCreation(request)) {
                log.info("Creating secret {}");
                existingSecretMetadata = request.getSecretMetadata();
            } else {
                throw new SecretException("Secret does not exist, but rotation was requested");
            }
        }
        return existingSecretMetadata;
    }

    private VersionMetadata checkAndGetExistingVersionMetadata(RotateSecretRequest request) {
        BigInteger oldVersion = request.getVersion().subtract(BigInteger.ONE);
        if (isCreation(request)) {
            return request.getVersionMetadata();
        }

        VersionMetadata oldVersionMetadata = secretClient().fetchMetadata(request.getSecretId(), oldVersion);
        if (oldVersionMetadata == null) {
            throw new SecretException("Previous version does not exist, but rotation was requested");
        }
        return oldVersionMetadata;
    }

    private boolean isCreation(RotateSecretRequest request) {
        return request.getCurrentVersion().equals(request.getVersion())
                && request.getVersion().equals(BigInteger.ONE);
    }

    private void checkNotYetRotated(String secretId, BigInteger version) {
        if (secretClient().fetchMetadata(secretId, version) != null) {
            throw new SecretException("New secret version already exists");
        }
    }

    private void checkTypes(RotateSecretRequest request, SecretMetadata existingSecretMetadata) {
        if (existingSecretMetadata.getSecretClass() != request.getSecretMetadata().getSecretClass()) {
            throw new SecretException("New secret type does not match existing type");
        }
        if ( ! existingSecretMetadata.getSecretClass().isInstance(request.getNewSecret())) {
            throw new SecretException("New secret does is not of expected type");
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        mapper = objectMapper();
        RotateSecretRequest request = mapper.readValue(inputStream, RotateSecretRequest.class);
        RotateSecretResponse response = handleRequest(request, context);
        mapper.writeValue(outputStream, response);
    }

    /**
     * For the sake of expediency, start with dumb DI.  Eventually, I'd like to replace this with Spring Boot.
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    private DynamoDBMapper dynamoDBMapper() {
        if (dynamoDBMapper == null) {
            dynamoDBMapper = new DynamoDBMapper(new AmazonDynamoDBClient());
        }
        return dynamoDBMapper;
    }

    private LockManager lockManager() {
        if (lockManager == null) {
            lockManager = new LockManager(dynamoDBMapper());
        }
        return lockManager;
    }

    private AmazonS3 amazonS3() {
        if (amazonS3 == null) {
            amazonS3 = new AmazonS3Client();
        }
        return amazonS3;
    }

    private SecretClient secretClient() {
        if (secretClient == null) {
            GlobalConfig globalConfig = GlobalConfig.load(dynamoDBMapper());
            secretClient = new S3SecretClient(amazonS3(), globalConfig.getSecretBucketName(), objectMapper());
        }
        return secretClient;
    }
}
