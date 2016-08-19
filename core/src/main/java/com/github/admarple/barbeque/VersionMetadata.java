package com.github.admarple.barbeque;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigInteger;
import java.time.Instant;

@Data
public class VersionMetadata {
    private BigInteger version;
    /**
     * Time at which the secret becomes active.
     *
     * The name "activation" was chosen because S3 metadata expects names that are all lowercase.
     * Updating the field name will require adding {@link com.fasterxml.jackson.annotation.JsonProperty}
     * to continue serializing as "activation".
     */
    private Instant activation;
    /**
     * Time at which the secret ceases to be active.
     *
     * The name "expiration" was chosen for the same reason described for {@link #activation}.
     */
    private Instant expiration;

    @JsonIgnore
    public boolean isActive() {
        return activation.isBefore(Instant.now())
                && (expiration == null || expiration.isAfter(Instant.now()));
    }
}
