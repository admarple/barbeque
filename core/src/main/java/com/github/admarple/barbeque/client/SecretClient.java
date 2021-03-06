package com.github.admarple.barbeque.client;

import com.github.admarple.barbeque.Secret;
import com.github.admarple.barbeque.VersionMetadata;
import com.github.admarple.barbeque.SecretException;
import com.github.admarple.barbeque.SecretMetadata;

import java.math.BigInteger;

public interface SecretClient {
    /**
     * Fetch metadata for the given secret
     * @param secretId
     * @return
     */
    SecretMetadata fetchMetadata(String secretId);

    /**
     * Fetch metadata for the given version of the given secret.
     * @param secretId
     * @param version
     * @return
     */
    VersionMetadata fetchMetadata(String secretId, BigInteger version);

    /**
     * Fetch the contents of the given secret and version.
     * @param secretMetadata
     * @param versionMetadata
     * @return
     */
    Secret fetchSecret(SecretMetadata secretMetadata, VersionMetadata versionMetadata);

    /**
     * Fetch the contents of the latest version of the given secret
     * @param secretMetadata
     * @throws SecretException if the secret has expired
     * @return
     */
    default Secret fetchSecret(SecretMetadata secretMetadata) {
        VersionMetadata versionMetadata = fetchMetadata(secretMetadata.getSecretId(), secretMetadata.getCurrentVersion());
        if ( ! versionMetadata.isActive()) {
            throw new SecretException("Secret has expired: " + secretMetadata);
        }
        return fetchSecret(secretMetadata, versionMetadata);
    }

    void putMetadata(SecretMetadata secretMetadata);

    void putSecret(SecretMetadata secretMetadata, VersionMetadata versionMetadata, Secret secret);
}
