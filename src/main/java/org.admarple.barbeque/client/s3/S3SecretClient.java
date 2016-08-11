package org.admarple.barbeque.client.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretException;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;
import org.admarple.barbeque.client.SecretClient;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class S3SecretClient implements SecretClient {
    static final String METADATA_PREFIX = "_metadata";

    private AmazonS3 s3;
    private String bucketName;
    private ObjectMapper mapper;

    private <T> T fetchContents(String key, Class<T> clazz) {
        try (S3Object object = s3.getObject(bucketName, key)) {
            return mapper.readValue(object.getObjectContent(), clazz);
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
    public VersionMetadata fetchMetadata(String secretId, BigInteger version) {
        String key = getVersionKey(secretId, version);
        try {
            return convertMetadata(s3.getObjectMetadata(bucketName, key));
        } catch (AmazonClientException | IOException e) {
            log.warn("Error reading metadata for {}:{} from S3", bucketName, key, e);
            throw new SecretException("Error reading metadata from S3", e);
        }
    }

    /**
     * {@link VersionMetadata} is stored in {@link ObjectMetadata#getUserMetadata()}.  We let
     * Jackson do the conversion from Map<String, String> and then to VersionMetadata.
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
}
