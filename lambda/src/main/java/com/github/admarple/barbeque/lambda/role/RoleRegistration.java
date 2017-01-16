package com.github.admarple.barbeque.lambda.role;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Role;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
public interface RoleRegistration {
    List<Role> getRoles(AmazonIdentityManagement iam);
}
