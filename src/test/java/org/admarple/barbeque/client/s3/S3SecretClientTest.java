package org.admarple.barbeque.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.admarple.barbeque.VersionMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.math.BigInteger;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class S3SecretClientTest {
    S3SecretClient s3SecretClient;
    ObjectMetadata s3Metadata;
    VersionMetadata versionMetadata;

    @Mock
    AmazonS3 s3;
    @Spy
    ObjectMapper mapper;
    String bucketName;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mapper.findAndRegisterModules();
        bucketName = "foo-bucket";

        s3SecretClient = new S3SecretClient(s3, bucketName, mapper);

        versionMetadata = new VersionMetadata();
        versionMetadata.setVersion(BigInteger.TEN);
        versionMetadata.setActivationTime(Instant.now());
        versionMetadata.setExpirationTime(versionMetadata.getActivationTime().plusSeconds(10));

        s3Metadata = new ObjectMetadata();
    }

    @Test
    public void testConvertMetadataRoundTrip() throws Exception {
        ObjectMetadata s3Metadata = s3SecretClient.convertMetadata(versionMetadata);
        VersionMetadata afterRoundTrip = s3SecretClient.convertMetadata(s3Metadata);

        assertThat(afterRoundTrip, equalTo(versionMetadata));
    }
}
