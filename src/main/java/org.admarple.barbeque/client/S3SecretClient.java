package org.admarple.barbeque.client;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretException;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;

import java.io.IOException;
import java.math.BigInteger;

@Slf4j
public class S3SecretClient implements SecretClient {
    static final String METADATA_PREFIX = "_metadata";

    private AmazonS3 s3;
    private String bucketName;
    private ObjectMapper mapper;

    private <T> T fetchContents(String key, Class<T> clazz) {
        try (S3Object object = s3.getObject(bucketName, key)) {
            return mapper.readValue(object.getObjectContent(), clazz);
        } catch (JsonMappingException | JsonParseException e) {
            log.warn("Error parsing contents of {}:{}", bucketName, key, e);
            throw new SecretException("Error parsing contents", e);
        } catch (AmazonClientException | IOException e) {
            log.warn("Error reading {}:{} from S3", bucketName, key, e);
            throw new SecretException("Error reading from S3", e);
        }
    }

    private String getMetadataKey(String secretId) {
        return StringUtils.join(Secret.SEPARATOR, METADATA_PREFIX);
    }

    private String getVersionKey(String secretId, BigInteger version) {
        return StringUtils.join(Secret.SEPARATOR, METADATA_PREFIX);
    }

    @Override
    public SecretMetadata fetchMetadata(String secretId) {
        String key = getMetadataKey(secretId);
        return fetchContents(key, SecretMetadata.class);
    }

    @Override
    public Secret fetchSecret(SecretMetadata secretMetadata, VersionMetadata versionMetadata) {
        String key = getVersionKey(secretMetadata.getSecretId(), versionMetadata.getVersion());
        return fetchContents(key, secretMetadata.getSecretClass());
    }
}
