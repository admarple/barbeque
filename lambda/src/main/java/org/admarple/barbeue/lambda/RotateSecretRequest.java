package org.admarple.barbeue.lambda;

import lombok.Data;
import lombok.experimental.Delegate;
import org.admarple.barbeque.Secret;
import org.admarple.barbeque.SecretMetadata;
import org.admarple.barbeque.VersionMetadata;

@Data
public class RotateSecretRequest {
    @Delegate
    private SecretMetadata secretMetadata;
    @Delegate
    private VersionMetadata versionMetadata;
    private Secret newSecret;
    private Integer overlapSeconds;
}
