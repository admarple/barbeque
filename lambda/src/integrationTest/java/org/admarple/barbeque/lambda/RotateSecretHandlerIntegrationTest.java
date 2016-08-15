package org.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import org.admarple.barbeque.CredentialPair;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretException;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;
import org.admarple.barbeque.client.s3.S3SecretClient;
import org.admarple.barbeque.lambda.util.LambdaSetupUtil;
import org.admarple.barbeque.util.SetupUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.math.BigInteger;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RotateSecretHandlerIntegrationTest {
    @Spy // To be able to inject an SecretClient with a long delay when testing locking
    RotateSecretHandler handler;
    RotateSecretRequest request;
    @Mock
    Context context;
    @Spy
    S3SecretClient s3SecretClient;

    @BeforeClass
    public static void setupBucketInS3() {
        SetupUtil.setupBucket(new AmazonS3Client());
    }

    @Before
    public void setup() throws InterruptedException {
        MockitoAnnotations.initMocks(this);
        when(context.getRemainingTimeInMillis()).thenReturn(3_000_000);

        request = new RotateSecretRequest();

        SecretMetadata secretMetadata = new SecretMetadata();
        secretMetadata.setCurrentVersion(BigInteger.ONE);
        secretMetadata.setSecretClass(CredentialPair.class);
        secretMetadata.setSecretId(this.getClass().getCanonicalName());
        request.setSecretMetadata(secretMetadata);

        VersionMetadata versionMetadata = new VersionMetadata();
        versionMetadata.setVersion(BigInteger.ONE);
        versionMetadata.setActivation(Instant.now());
        versionMetadata.setExpiration(versionMetadata.getActivation().plusSeconds(300));
        request.setVersionMetadata(versionMetadata);

        CredentialPair newSecret = new CredentialPair();
        newSecret.setPrincipal("principal1");
        newSecret.setCredential("credential1");
        request.setNewSecret(newSecret);

        request.setOverlapSeconds(30);

        s3SecretClient.setBucketName(SetupUtil.TEST_BUCKET_NAME);
        s3SecretClient.setMapper(handler.objectMapper());
        s3SecretClient.setS3(handler.amazonS3());

        LambdaSetupUtil.setupDynamo(new AmazonDynamoDBClient());
    }

    @After
    public void teardown() throws Exception {
        SetupUtil.emptyBucket(new AmazonS3Client());
        LambdaSetupUtil.emptyDynamo(new AmazonDynamoDBClient());
    }

    @Test
    public void testHandleRequest() {
        handler.handleRequest(request, context);
    }

    @Test(expected = SecretException.class)
    public void testHandRequestDoubleCreate() {
        handler.handleRequest(request, context);
        handler.handleRequest(request, context);
    }

    /**
     * Spin up two threads, the first of which will hold the lock for a while.  The second thread should
     * not perform any updates.
     * @throws Exception
     */
    @Test
    public void testHandleRequestLocking() throws Exception {
        when(handler.secretClient()).thenReturn(s3SecretClient);
        doAnswer(invocationOnMock -> {
            Thread.sleep(10_000L); // The first attempt will hold the lock for 10 seconds
            return invocationOnMock.callRealMethod();
        })
                .doCallRealMethod() // The second attempt will
                .when(s3SecretClient).putSecret(any(SecretMetadata.class), any(VersionMetadata.class), any(Secret.class));

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        AtomicInteger secretExceptions = new AtomicInteger(0);
        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                try {
                    handler.handleRequest(request, context);
                } catch (SecretException e) {
                    secretExceptions.incrementAndGet();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(secretExceptions.get(), equalTo(1));
        verify(s3SecretClient, times(1)).putSecret(any(SecretMetadata.class), any(VersionMetadata.class), eq(request.getNewSecret()));
    }

    @Test
    public void testHandleRequestLockDeletedOnException() {
        when(handler.secretClient()).thenReturn(s3SecretClient);
        doThrow(new SecretException("Should release lock"))
                .doCallRealMethod()
                .when(s3SecretClient).putSecret(any(SecretMetadata.class), any(VersionMetadata.class), any(Secret.class));

        try {
            handler.handleRequest(request, context);
            fail();
        } catch (SecretException e) {
            assertThat(e.getMessage(), equalTo("Should release lock"));
        } catch (Exception e) {
            fail();
        }
        handler.handleRequest(request, context);
    }
}
