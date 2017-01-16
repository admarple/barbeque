package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.github.admarple.barbeque.lambda.role.PrefixRoleRegistration;
import com.github.admarple.barbeque.lambda.role.RoleRepository;

public class RegisterRoleHandler extends JsonRequestStreamHandler<RegisterRoleRequest, RegisterRoleResponse> {
    private RoleRepository roleRepository;
    private AmazonIdentityManagement iam;
    private DynamoDBMapper dynamoDBMapper;

    @Override
    RegisterRoleResponse handleRequestInternal(RegisterRoleRequest request, Context context) {
        PrefixRoleRegistration prefixRoleRegistration = new PrefixRoleRegistration();
        prefixRoleRegistration.setRoleNamePrefix(request.getRoleNamePrefix());
        roleRepository().registerRole(prefixRoleRegistration);
        return new RegisterRoleResponse();
    }


}
