package com.github.admarple.barbeque.lambda.role;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.github.admarple.barbeque.lambda.JsonConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@DynamoDBTypeConverted(converter=RoleRegistrationListFormat.Converter.class)
public @interface RoleRegistrationListFormat {
    class Converter extends JsonConverter<List<RoleRegistration>> {
        public Converter(Class<List<RoleRegistration>> targetType) {
            super(targetType);
        }
    }
}
