package org.admarple.barbeque.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.admarple.barbeque.CredentialPair;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class S3SecretClientIntegrationTest {
    public static final String TEST_BUCKET_NAME = "org.admarple.barbeque.test";

    AmazonS3 s3;
    S3SecretClient s3SecretClient;
    ObjectMapper mapper;

    SecretMetadata secretMetadata;
    VersionMetadata versionMetadata;
    Secret secret;

    @Before
    public void setup() {
        s3 = new AmazonS3Client();
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        s3SecretClient = new S3SecretClient(s3, TEST_BUCKET_NAME, mapper);
    }

    @After
    public void teardown() {
        emptyBucket();
    }

    @BeforeClass
    public static void setupBucketInS3() {
        S3SecretClientIntegrationTest test = new S3SecretClientIntegrationTest();
        test.setup();

        if ( ! test.s3.doesBucketExist(TEST_BUCKET_NAME)) {
            CreateBucketRequest request = new CreateBucketRequest(TEST_BUCKET_NAME);
            test.s3.createBucket(request);
        }
    }

    private void setupSecretInS3() {
        secretMetadata = new SecretMetadata();
        secretMetadata.setSecretClass(CredentialPair.class);
        secretMetadata.setSecretId("S3ClientIntegrationTest");
        secretMetadata.setCurrentVersion(BigInteger.ONE);

        s3SecretClient.putMetadata(secretMetadata);

        versionMetadata = new VersionMetadata();
        versionMetadata.setActivation(Instant.now());
        versionMetadata.setExpiration(versionMetadata.getActivation().plus(10, ChronoUnit.MINUTES));
        versionMetadata.setVersion(secretMetadata.getCurrentVersion());

        CredentialPair pair = new CredentialPair();
        pair.setPrincipal("foo-principal");
        pair.setCredential("foo-credential");
        secret = pair;

        s3SecretClient.putSecret(secretMetadata, versionMetadata, pair);
    }

    /*
     * From http://docs.aws.amazon.com/AmazonS3/latest/dev/delete-or-empty-bucket.html#delete-bucket-sdk-java
     */
    private void emptyBucket() {
        ObjectListing objectListing = s3.listObjects(TEST_BUCKET_NAME);

        while (true) {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                s3.deleteObject(TEST_BUCKET_NAME, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        };
        VersionListing list = s3.listVersions(new ListVersionsRequest().withBucketName(TEST_BUCKET_NAME));
        for (S3VersionSummary s : list.getVersionSummaries()) {
            s3.deleteVersion(TEST_BUCKET_NAME, s.getKey(), s.getVersionId());
        }
    }

    @Test
    public void testPutMetadata() {
        setupSecretInS3();

        String expectedKey = secretMetadata.getSecretId() + Secret.SEPARATOR + S3SecretClient.METADATA_PREFIX;
        assertThat(s3.doesObjectExist(TEST_BUCKET_NAME, expectedKey), is(true));
        assertThat(s3SecretClient.fetchMetadata(secretMetadata.getSecretId()), equalTo(secretMetadata));
    }

    @Test
    public void testPutSecret() {
        setupSecretInS3();

        String expectedKey = secretMetadata.getSecretId() + Secret.SEPARATOR + versionMetadata.getVersion();
        assertThat(s3.doesObjectExist(TEST_BUCKET_NAME, expectedKey), is(true));
        assertThat(s3SecretClient.fetchMetadata(secretMetadata.getSecretId(), versionMetadata.getVersion()), equalTo(versionMetadata));
        assertThat(s3SecretClient.fetchSecret(secretMetadata, versionMetadata), equalTo(secret));
    }

    @Test
    public void testFetchSecret() {
        setupSecretInS3();

        assertThat(s3SecretClient.fetchSecret(secretMetadata), equalTo(secret));
    }

}
