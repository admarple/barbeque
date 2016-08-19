package com.github.admarple.barbeque;

import lombok.Data;

@Data
public class CredentialPair implements Secret {
    private String principal;
    private String credential;
}
