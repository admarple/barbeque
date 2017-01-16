package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.admarple.barbeque.lambda.role.RoleRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class JsonRequestStreamHandler<S, T> implements RequestStreamHandler {
    private ObjectMapper mapper;
    private RoleRepository roleRepository;
    private AmazonIdentityManagement iam;
    private DynamoDBMapper dynamoDBMapper;

    abstract T handleRequestInternal(S request, Context context);

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        S request = objectMapper().readValue(inputStream, new TypeReference<S>(){});
        T response = handleRequestInternal(request, context);
        objectMapper().writeValue(outputStream, response);
    }

    /**
     * For the sake of expediency, start with dumb DI.  Eventually, I'd like to replace this with Spring Boot.
     */
    public ObjectMapper objectMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
        }
        return mapper;
    }

    AmazonIdentityManagement iam() {
        if (iam == null) {
            iam = new AmazonIdentityManagementClient();
        }
        return iam;
    }

    DynamoDBMapper dynamoDBMapper() {
        if (dynamoDBMapper == null) {
            dynamoDBMapper = new DynamoDBMapper(new AmazonDynamoDBClient());
        }
        return dynamoDBMapper;
    }

    RoleRepository roleRepository() {
        if (roleRepository == null) {
            roleRepository = new RoleRepository(iam(), dynamoDBMapper());
        }
        return roleRepository;
    }
}
