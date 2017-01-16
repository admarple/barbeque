package com.github.admarple.barbeque.lambda.role;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PrefixRoleRegistration implements RoleRegistration {
    String roleNamePrefix;

    @Override
    public List<Role> getRoles(AmazonIdentityManagement iam) {
        List<Role> roles = new ArrayList<>();
        ListRolesRequest listRolesRequest = new ListRolesRequest().withPathPrefix(getRoleNamePrefix());
        ListRolesResult listRolesResult;
        do {
            listRolesResult = iam.listRoles(listRolesRequest);
            roles.addAll(listRolesResult.getRoles());
            listRolesRequest = listRolesRequest.withMarker(listRolesResult.getMarker());
        } while (listRolesResult.isTruncated());
        return roles;
    }
}
