package com.github.admarple.barbeque.lambda;

import com.github.admarple.barbeque.SecretMetadata;
import lombok.Data;

@Data
public class GrantPermissionRequest {
    private SecretMetadata secretMetadata;
    private String roleArn;
}
