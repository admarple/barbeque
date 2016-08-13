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
    private int refreshBeforeExpirySeconds = 30;

    private SecretMetadata secretMetadata;
    private VersionMetadata currentVersionMetadata;
    private T secret;
    private String lastErrorMessage;
    private volatile Instant nextFetchTime;


    public SecretManager(SecretClient client, String secretId, Class<T> clazz) {
        this.secretClient = client;
        this.secretId = secretId;
        this.clazz = clazz;
    }

    public T getSecret() {
        if (needsRefresh()) {
            synchronized (this) {
                if (needsRefresh()) {
                    try {
                        refresh();
                    } catch (RuntimeException e) {
                        log.warn("Error refreshing {}, will continue using existing secret (if it exists)", secretId, e);
                    }
                }
            }
        }
        return hopefullySecret();
    }

    private boolean needsRefresh() {
        if (nextFetchTime == null || nextFetchTime.isBefore(Instant.now())) {
            log.debug("Refreshing {}", secretId);
            return true;
        }

        return false;
    }

    private void refresh() {
        secretMetadata = secretClient.fetchMetadata(secretId);
        if (secretMetadata.getSecretClass() != clazz) {
            lastErrorMessage = String.format("Secret type %s is not expected type %s", secretMetadata.getSecretClass(), clazz);
            nextFetchTime = Instant.now().plusSeconds(refreshIntervalSeconds);
            throw new SecretException(lastErrorMessage);
        }
        currentVersionMetadata = secretClient.fetchMetadata(secretId, secretMetadata.getCurrentVersion());
        secret = (T) secretClient.fetchSecret(secretMetadata, currentVersionMetadata);

        nextFetchTime = Instant.now().plusSeconds(refreshIntervalSeconds);
        Instant refreshBeforeExpiration = currentVersionMetadata.getExpiration().minusSeconds(refreshBeforeExpirySeconds);
        if (refreshBeforeExpiration.isBefore(nextFetchTime)) {
            log.debug("Next refresh of {}{}{} scheduled at {} to avoid expiration",
                    secretId, Secret.SEPARATOR, currentVersionMetadata.getVersion(), refreshBeforeExpiration);
            nextFetchTime = refreshBeforeExpiration;
        }
        lastErrorMessage = null;
    }

    private T hopefullySecret() {
        if (secret == null) {
            throw new SecretException(lastErrorMessage);
        }
        return secret;
    }
}
