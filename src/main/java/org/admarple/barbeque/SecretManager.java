package org.admarple.barbeque;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.admarple.barbeque.client.SecretClient;

import java.time.Instant;

@Slf4j
public class SecretManager<T extends Secret> {
    private SecretClient secretClient;
    private String secretId;
    private Class<T> clazz;

    @Getter
    @Setter
    private int refreshIntervalSeconds = 900;
    @Getter
    @Setter
    private int refreshBeforeExpirySeconds = 300;

    private SecretMetadata secretMetadata;
    private VersionMetadata currentVersionMetadata;
    private T secret;
    private String lastErrorMessage;
    private Instant lastFetchTime;


    public SecretManager(SecretClient client, String secretId, Class<T> clazz) {
        this.secretClient = client;
        this.secretId = secretId;
        this.clazz = clazz;
    }

    public T getSecret() {
        if (needsRefresh()) {
            refresh();
        }
        return hopefullySecret();
    }

    private boolean needsRefresh() {
        if (secretMetadata == null) {
            return true;
        }

        Instant refreshAfterInterval = lastFetchTime.plusSeconds(refreshIntervalSeconds);
        if (refreshAfterInterval.isBefore(Instant.now())) {
            log.debug("Refreshing after interval");
            return true;
        }

        if (currentVersionMetadata != null) {
            Instant refreshBeforeExpiration = currentVersionMetadata.getExpirationTime().minusSeconds(refreshBeforeExpirySeconds);
            if (refreshBeforeExpiration.isBefore(Instant.now())) {
                log.debug("Refreshing before expiration.");
                return true;
            }
        }

        return false;
    }

    private void refresh() {
        secretMetadata = secretClient.fetchMetadata(secretId);
        if (secretMetadata.getSecretClass() != clazz) {
            lastErrorMessage = String.format("Secret type %s is not expected type %s", secretMetadata.getSecretClass(), clazz);
            lastFetchTime = Instant.now();
            throw new SecretException(lastErrorMessage);
        }
        currentVersionMetadata = secretClient.fetchMetadata(secretId, secretMetadata.getCurrentVersion());
        secret = (T) secretClient.fetchSecret(secretMetadata, currentVersionMetadata);
        lastFetchTime = Instant.now();
        lastErrorMessage = null;
    }

    private T hopefullySecret() {
        if (secret == null) {
            throw new SecretException(lastErrorMessage);
        }
        return secret;
    }
}
