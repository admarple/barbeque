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
        versionMetadata.setActivation(Instant.now().minusSeconds(10));
        versionMetadata.setExpiration(versionMetadata.getActivation().plusSeconds(60));
    }

    @Test
    public void testIsActive() {
        assertThat(versionMetadata.isActive(), is(true));
    }

    @Test
    public void testIsActiveNullExpiration() {
        versionMetadata.setExpiration(null);

        assertThat(versionMetadata.isActive(), is(true));
    }

    @Test
    public void testIsActiveExpired() {
        versionMetadata.setExpiration(versionMetadata.getExpiration().minusSeconds(120));
        versionMetadata.setActivation(versionMetadata.getActivation().minusSeconds(120));

        assertThat(versionMetadata.isActive(), is(false));
    }

    @Test
    public void testIsActiveEarly() {
        versionMetadata.setExpiration(versionMetadata.getExpiration().plusSeconds(120));
        versionMetadata.setActivation(versionMetadata.getActivation().plusSeconds(120));

        assertThat(versionMetadata.isActive(), is(false));
    }
}
