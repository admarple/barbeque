package org.admarple.barbeque.client.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretException;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;
import org.admarple.barbeque.client.SecretClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Map;

@Slf4j
@Setter // To support Mockito's @Spy
@NoArgsConstructor // To support Mockito's @Spy
@AllArgsConstructor
public class S3SecretClient implements SecretClient {
    static final String METADATA_PREFIX = "_metadata";

    private AmazonS3 s3;
    private String bucketName;
    private ObjectMapper mapper;

    private <T> T fetchContents(String key, Class<T> clazz) {
        try {
            if (s3.doesObjectExist(bucketName, key)) {
                try (S3Object object = s3.getObject(bucketName, key)) {
                    return mapper.readValue(object.getObjectContent(), clazz);
                }
            } else {
                return null;
            }
        } catch (AmazonClientException | IOException e) {
            log.warn("Error reading {}:{} from S3", bucketName, key, e);
            throw new SecretException(String.format("Error reading %s:%s from S3", bucketName, key), e);
        }
    }

    private String getMetadataKey(String secretId) {
        return StringUtils.join(Secret.SEPARATOR, secretId, METADATA_PREFIX);
    }

    private String getVersionKey(String secretId, BigInteger version) {
        return StringUtils.join(Secret.SEPARATOR, secretId, version.toString());
    }

    @Override
    public SecretMetadata fetchMetadata(String secretId) {
        String key = getMetadataKey(secretId);
        return fetchContents(key, SecretMetadata.class);
    }

    @Override
    public VersionMetadata fetchMetadata(String secretId, BigInteger version) {
        String key = getVersionKey(secretId, version);
        try {
            if (s3.doesObjectExist(bucketName, key)) {
                return convertMetadata(s3.getObjectMetadata(bucketName, key));
            } else {
                return null;
            }
        } catch (AmazonClientException | IOException e) {
            log.warn("Error reading metadata for {}:{} from S3", bucketName, key, e);
            throw new SecretException(String.format("Error reading metadata for %s:%s from S3", bucketName, key), e);
        }
    }

    /**
     * {@link VersionMetadata} is stored in {@link ObjectMetadata#getUserMetadata()}.  We let
     * Jackson do the conversion from Map<String, String> and then to VersionMetadata.
     *
     * @param s3Metadata
     * @return
     * @throws IOException
     */
    VersionMetadata convertMetadata(ObjectMetadata s3Metadata) throws IOException {
        String mapAsJson = mapper.writeValueAsString(s3Metadata.getUserMetadata());
        return mapper.readValue(mapAsJson, VersionMetadata.class);
    }

    /**
     * Reverse the conversion from {@link #convertMetadata(ObjectMetadata)}.
     *
     * @param versionMetadata
     * @return
     * @throws IOException
     */
    ObjectMetadata convertMetadata(VersionMetadata versionMetadata) throws IOException {
        String versionAsJson = mapper.writeValueAsString(versionMetadata);
        Map<String, String> versionAsMap = mapper.readValue(versionAsJson, new TypeReference<Map<String, String>>() {});
        ObjectMetadata s3Metadata = new ObjectMetadata();
        s3Metadata.setUserMetadata(versionAsMap);
        return s3Metadata;
    }

    @Override
    public Secret fetchSecret(SecretMetadata secretMetadata, VersionMetadata versionMetadata) {
        String key = getVersionKey(secretMetadata.getSecretId(), versionMetadata.getVersion());
        return fetchContents(key, secretMetadata.getSecretClass());
    }

    @Override
    public void putMetadata(SecretMetadata secretMetadata) {
        String key = getMetadataKey(secretMetadata.getSecretId());
        try {
            s3.putObject(bucketName, key, mapper.writeValueAsString(secretMetadata));
        } catch (AmazonClientException | IOException e) {
            log.warn("Error writing {}:{} to S3", bucketName, key, e);
            throw new SecretException(String.format("Error writing %s:%s to S3", bucketName, key), e);
        }
    }

    @Override
    public void putSecret(SecretMetadata secretMetadata, VersionMetadata versionMetadata, Secret secret) {
        String key = getVersionKey(secretMetadata.getSecretId(), versionMetadata.getVersion());
        try (InputStream stream = new ByteArrayInputStream(mapper.writeValueAsBytes(secret))) {
            ObjectMetadata s3Metadata = convertMetadata(versionMetadata);
            // For now, use S3-managed server-side encryption
            s3Metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, stream, s3Metadata);
            s3.putObject(putRequest);
        } catch (AmazonClientException | IOException e) {
            log.warn("Error writing {}:{} to S3", bucketName, key, e);
            throw new SecretException(String.format("Error writing %s:%s to S3", bucketName, key), e);
        }
    }
}
