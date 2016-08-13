package org.admarple.barbeue.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RotateSecretHandler implements RequestHandler<RotateSecretRequest, RotateSecretResponse>,
        RequestStreamHandler {

    ObjectMapper mapper;

    @Override
    public RotateSecretResponse handleRequest(RotateSecretRequest request, Context context) {
        // validate request

        // lock on the new version

        // confirm that the old version exists

        // PUT the new credential

        // Update the version metadata of the previous version (expiration)

        // Update the secretMetadata (current version)

        // release lock
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        mapper = objectMapper();
        RotateSecretRequest request = mapper.readValue(inputStream, RotateSecretRequest.class);
        RotateSecretResponse response = handleRequest(request, context);
        mapper.writeValue(outputStream, response);
    }

    /**
     * For the sake of expediency, start with dumb DI.  Eventually, I'd like to replace this with Spring Boot.
     */
    private ObjectMapper objectMapper() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    public static void setupLambda() {

    }
}
