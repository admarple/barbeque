package org.admarple.barbeque.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.admarple.barbeque.CredentialPair;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;
import org.admarple.barbeque.util.SetupUtil;
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

        SetupUtil.setupBucket(test.s3, SetupUtil.TEST_BUCKET_NAME);
    }

    @Before
    public void setup() {
        setupClients();
        setupSecretInS3();
    }

    @After
    public void teardown() {
        SetupUtil.emptyBucket(s3, SetupUtil.TEST_BUCKET_NAME);
    }

    private void setupClients() {
        s3 = new AmazonS3Client();
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        s3SecretClient = new S3SecretClient(s3, SetupUtil.TEST_BUCKET_NAME, mapper);
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

    @Test
    public void testPut() {
        // PUT is already done as part of setupSecretsInS3()
        String metadataKey = secretMetadata.getSecretId() + Secret.SEPARATOR + S3SecretClient.METADATA_PREFIX;
        assertThat(s3.doesObjectExist(SetupUtil.TEST_BUCKET_NAME, metadataKey), is(true));

        String versionKey = secretMetadata.getSecretId() + Secret.SEPARATOR + versionMetadata.getVersion();
        assertThat(s3.doesObjectExist(SetupUtil.TEST_BUCKET_NAME, versionKey), is(true));
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
