package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.lambda.runtime.Context;

public class GrantPermissionHandler extends JsonRequestStreamHandler<GrantPermissionRequest, GrantPermissionResponse> {
    @Override
    GrantPermissionResponse handleRequestInternal(GrantPermissionRequest request, Context context) {
        roleRepository().grantPermission(request.getSecretMetadata(), request.getRoleArn());
        return new GrantPermissionResponse();
    }
}
