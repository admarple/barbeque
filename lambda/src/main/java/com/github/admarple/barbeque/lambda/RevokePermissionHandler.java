package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.lambda.runtime.Context;

public class RevokePermissionHandler extends JsonRequestStreamHandler<RevokePermissionRequest, RevokePermissionResponse> {

    @Override
    RevokePermissionResponse handleRequestInternal(RevokePermissionRequest request, Context context) {
        roleRepository().revokePermission(request.getSecretMetadata(), request.getRoleArn());
        return new RevokePermissionResponse();
    }
}
