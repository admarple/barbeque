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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class S3SecretClientIntegrationTest {
    public static final String TEST_BUCKET_NAME = "org.admarple.barbeque.test";

    AmazonS3 s3;
    S3SecretClient s3SecretClient;
    ObjectMapper mapper;

    SecretMetadata secretMetadata;
    VersionMetadata versionMetadata;
    Secret secret;

    @BeforeClass
    public static void setupBucketInS3() {
        S3SecretClientIntegrationTest test = new S3SecretClientIntegrationTest();
        test.setupClients();

        if ( ! test.s3.doesBucketExist(TEST_BUCKET_NAME)) {
            CreateBucketRequest request = new CreateBucketRequest(TEST_BUCKET_NAME);
            test.s3.createBucket(request);
        }
    }

    @Before
    public void setup() {
        setupClients();
        setupSecretInS3();
    }

    @After
    public void teardown() {
        emptyBucket();
    }

    private void setupClients() {
        s3 = new AmazonS3Client();
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        s3SecretClient = new S3SecretClient(s3, TEST_BUCKET_NAME, mapper);
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
        }
        VersionListing list = s3.listVersions(new ListVersionsRequest().withBucketName(TEST_BUCKET_NAME));
        for (S3VersionSummary s : list.getVersionSummaries()) {
            s3.deleteVersion(TEST_BUCKET_NAME, s.getKey(), s.getVersionId());
        }
    }

    @Test
    public void testPut() {
        // PUT is already done as part of setupSecretsInS3()
        String metadataKey = secretMetadata.getSecretId() + Secret.SEPARATOR + S3SecretClient.METADATA_PREFIX;
        assertThat(s3.doesObjectExist(TEST_BUCKET_NAME, metadataKey), is(true));

        String versionKey = secretMetadata.getSecretId() + Secret.SEPARATOR + versionMetadata.getVersion();
        assertThat(s3.doesObjectExist(TEST_BUCKET_NAME, versionKey), is(true));
    }

    @Test
    public void testFetchMetadata() {
        assertThat(s3SecretClient.fetchMetadata(secretMetadata.getSecretId()), equalTo(secretMetadata));
    }

    @Test
    public void testFetchSecret() {
        assertThat(s3SecretClient.fetchMetadata(secretMetadata.getSecretId(), versionMetadata.getVersion()), equalTo(versionMetadata));
        assertThat(s3SecretClient.fetchSecret(secretMetadata, versionMetadata), equalTo(secret));
    }

    @Test
    public void testFetchSecretNonexistent() {
        versionMetadata.setVersion(versionMetadata.getVersion().add(BigInteger.ONE));

        assertThat(s3SecretClient.fetchSecret(secretMetadata, versionMetadata), is(nullValue()));
    }

}
