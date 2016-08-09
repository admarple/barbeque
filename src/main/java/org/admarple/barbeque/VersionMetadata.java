package org.admarple.barbeque;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.math.BigInteger;
import java.time.Instant;

@Data
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
public abstract class VersionMetadata {
    private BigInteger version;
    private Instant activationTime;
    private Instant expirationTime;

    public boolean isActive() {
        return activationTime.isBefore(Instant.now())
                && (expirationTime == null || expirationTime.isAfter(Instant.now()));
    }
}
