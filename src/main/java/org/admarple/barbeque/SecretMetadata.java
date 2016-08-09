package org.admarple.barbeque;

import lombok.Data;
import lombok.ToString;

import java.math.BigInteger;
import java.util.SortedMap;

@Data
@ToString(exclude = "versionMetadata")
public class SecretMetadata {
    String secretId;
    SortedMap<BigInteger, VersionMetadata> versionMetadata;
    Class<? extends Secret> secretClass;

    public VersionMetadata getCurrentVersion() {
        return versionMetadata.get(versionMetadata.lastKey());
    }
}
