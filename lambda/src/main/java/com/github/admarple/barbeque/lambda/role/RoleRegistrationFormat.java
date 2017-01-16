package com.github.admarple.barbeque.lambda.role;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.github.admarple.barbeque.lambda.JsonConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@DynamoDBTypeConverted(converter=RoleRegistrationFormat.Converter.class)
public @interface RoleRegistrationFormat {
    class Converter extends JsonConverter<RoleRegistration> {
        public Converter(Class<RoleRegistration> targetType) {
            super(targetType);
        }
    }
}
