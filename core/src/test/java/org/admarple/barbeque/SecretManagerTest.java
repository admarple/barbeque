package org.admarple.barbeque;

import org.admarple.barbeque.client.SecretClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SecretManagerTest {
    SecretManager secretManager;

    @Mock
    SecretClient secretClient;

    SecretMetadata secretMetadata;
    VersionMetadata versionMetadata;
    Secret secret;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        secretMetadata = new SecretMetadata();
        secretMetadata.setSecretId("secret-id");
        secretMetadata.setSecretClass(CredentialPair.class);
        secretMetadata.setCurrentVersion(BigInteger.TEN);

        versionMetadata = new VersionMetadata();
        versionMetadata.setVersion(secretMetadata.getCurrentVersion());
        versionMetadata.setActivation(Instant.now());
        versionMetadata.setExpiration(versionMetadata.getActivation().plusSeconds(3600));

        CredentialPair pair = new CredentialPair();
        pair.setPrincipal("foo-principal");
        pair.setCredential("foo-credential");
        secret = pair;

        secretManager = new SecretManager(secretClient, secretMetadata.getSecretId(), CredentialPair.class);

        when(secretClient.fetchMetadata(secretMetadata.getSecretId())).thenReturn(secretMetadata);
        when(secretClient.fetchMetadata(secretMetadata.getSecretId(), secretMetadata.getCurrentVersion()))
                .thenReturn(versionMetadata);
        when(secretClient.fetchSecret(secretMetadata, versionMetadata)).thenReturn(secret);
    }

    @Test
    public void testGetSecret() throws Exception {
        Secret result = secretManager.getSecret();

        assertThat(result, equalTo(secret));
        verify(secretClient).fetchMetadata(secretMetadata.getSecretId());
        verify(secretClient).fetchMetadata(secretMetadata.getSecretId(), secretMetadata.getCurrentVersion());
        verify(secretClient).fetchSecret(secretMetadata, versionMetadata);
    }

    @Test
    public void testGetSecretNoRefreshNeeded() throws Exception {
        secretManager.getSecret();
        Secret result = secretManager.getSecret();

        assertThat(result, equalTo(secret));
        verify(secretClient, times(1)).fetchMetadata(secretMetadata.getSecretId());
        verify(secretClient, times(1)).fetchMetadata(secretMetadata.getSecretId(), secretMetadata.getCurrentVersion());
        verify(secretClient, times(1)).fetchSecret(secretMetadata, versionMetadata);
    }

    @Test
    public void testGetSecretPastInterval() throws Exception {
        secretManager.setRefreshIntervalSeconds(5);
        secretManager.getSecret();
        Thread.sleep(5_000L);

        Secret result = secretManager.getSecret();

        assertThat(result, equalTo(secret));
        verify(secretClient, times(2)).fetchMetadata(secretMetadata.getSecretId());
        verify(secretClient, times(2)).fetchMetadata(secretMetadata.getSecretId(), secretMetadata.getCurrentVersion());
        verify(secretClient, times(2)).fetchSecret(secretMetadata, versionMetadata);
    }

    @Test
    public void testGetSecretExpirationTooClose() throws Exception {
        versionMetadata.setExpiration(Instant.now().plusSeconds(secretManager.getRefreshBeforeExpirySeconds() - 1));
        secretManager.getSecret();

        Secret result = secretManager.getSecret();

        assertThat(result, equalTo(secret));
        verify(secretClient, times(2)).fetchMetadata(secretMetadata.getSecretId());
        verify(secretClient, times(2)).fetchMetadata(secretMetadata.getSecretId(), secretMetadata.getCurrentVersion());
        verify(secretClient, times(2)).fetchSecret(secretMetadata, versionMetadata);
    }

    private static class TestSecret implements Secret {}

    @Test
    public void testGetSecretWrongType() throws Exception {
        secretMetadata.setSecretClass(TestSecret.class);

        for (int i = 0; i < 2; i ++) {
            try {
                secretManager.getSecret();
                fail();
            } catch (SecretException ignored) {
            } catch (Exception e) {
                fail();
            }
        }

        verify(secretClient, times(1)).fetchMetadata(secretMetadata.getSecretId());
        verifyNoMoreInteractions(secretClient);
    }
}
