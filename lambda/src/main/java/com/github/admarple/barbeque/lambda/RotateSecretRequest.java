package com.github.admarple.barbeque.lambda;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.admarple.barbeque.Secret;
import com.github.admarple.barbeque.VersionMetadata;
import lombok.Data;
import com.github.admarple.barbeque.SecretMetadata;

import java.math.BigInteger;

@Data
public class RotateSecretRequest {
    private SecretMetadata secretMetadata;
    private VersionMetadata versionMetadata;
    private Secret newSecret;
    private Integer overlapSeconds;

    @JsonIgnore
    public String getSecretId() {
        return secretMetadata != null ? secretMetadata.getSecretId() : null;
    }

    @JsonIgnore
    public BigInteger getCurrentVersion() {
        return secretMetadata != null ? secretMetadata.getCurrentVersion() : null;
    }

    @JsonIgnore
    public BigInteger getVersion() {
        return versionMetadata != null ? versionMetadata.getVersion() : null;
    }
}
