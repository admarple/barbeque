package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.admarple.barbeque.SecretException;
import lombok.AllArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
public class JsonConverter<T> implements DynamoDBTypeConverter<String, T> {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    public final Class<T> targetType;

    @Override
    public String convert(final T role) {
        try {
            return MAPPER.writeValueAsString(role);
        } catch (JsonProcessingException e) {
            throw new SecretException("Unable to convert " + role, e);
        }
    }
    @Override
    public T unconvert(final String string) {
        try {
            return MAPPER.readValue(string, targetType);
        } catch (IOException e) {
            throw new SecretException("Unable to unconvert " + string, e);
        }
    }
}