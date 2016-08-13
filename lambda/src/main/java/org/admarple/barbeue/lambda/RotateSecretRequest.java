package org.admarple.barbeue.lambda;

import lombok.Data;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;

@Data
public class RotateSecretRequest {
    private SecretMetadata secretMetadata;
    private VersionMetadata versionMetadata;
    private Secret newSecret;
    private Integer overlapSeconds;
}
