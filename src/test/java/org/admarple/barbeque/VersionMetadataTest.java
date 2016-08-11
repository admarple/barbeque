package org.admarple.barbeque;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class VersionMetadataTest {
    VersionMetadata versionMetadata;

    @Before
    public void setup() {
        versionMetadata = new VersionMetadata();
        versionMetadata.setVersion(BigInteger.TEN);
        versionMetadata.setActivationTime(Instant.now().minusSeconds(10));
        versionMetadata.setExpirationTime(versionMetadata.getActivationTime().plusSeconds(60));
    }

    @Test
    public void testIsActive() {
        assertThat(versionMetadata.isActive(), is(true));
    }

    @Test
    public void testIsActiveNullExpiration() {
        versionMetadata.setExpirationTime(null);

        assertThat(versionMetadata.isActive(), is(true));
    }

    @Test
    public void testIsActiveExpired() {
        versionMetadata.setExpirationTime(versionMetadata.getExpirationTime().minusSeconds(120));
        versionMetadata.setActivationTime(versionMetadata.getActivationTime().minusSeconds(120));

        assertThat(versionMetadata.isActive(), is(false));
    }

    @Test
    public void testIsActiveEarly() {
        versionMetadata.setExpirationTime(versionMetadata.getExpirationTime().plusSeconds(120));
        versionMetadata.setActivationTime(versionMetadata.getActivationTime().plusSeconds(120));

        assertThat(versionMetadata.isActive(), is(false));
    }
}
