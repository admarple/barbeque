package com.github.admarple.barbeque.lambda;

import lombok.Data;
import lombok.experimental.Wither;

@Data
@Wither
public class RevokePermissionResponse {
    private String detail;
}
