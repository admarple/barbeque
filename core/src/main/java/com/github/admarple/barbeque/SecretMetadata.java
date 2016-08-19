package com.github.admarple.barbeque;

import lombok.Data;

import java.math.BigInteger;

@Data
public class SecretMetadata {
    String secretId;
    Class<? extends Secret> secretClass;
    BigInteger currentVersion;
}
