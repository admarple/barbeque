package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class ListRolesHandler extends JsonRequestStreamHandler<ListRolesRequest, ListRolesResponse> {
    @Override
    ListRolesResponse handleRequestInternal(ListRolesRequest request, Context context) {
        List<Role> roles = roleRepository().getRoles().stream()
                .filter(role -> role.getRoleName().startsWith(request.getRoleNamePrefix()))
                .collect(toList());
        return new ListRolesResponse().withRoles(roles);
    }
}
