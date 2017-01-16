package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersioned;
import com.github.admarple.barbeque.lambda.role.RoleRegistration;
import com.github.admarple.barbeque.lambda.role.RoleRegistrationListFormat;
import lombok.Data;
import lombok.Getter;

import java.math.BigInteger;
import java.util.List;

@Data
@DynamoDBTable(tableName = "BBQLambdaUtil")
public class GlobalConfig {
    @Getter(onMethod = @__({@DynamoDBHashKey}))
    private final String id = "GlobalConfig";
    @Getter(onMethod = @__({@DynamoDBAttribute}))
    private String secretBucketName;
    @Getter(onMethod = @__({@DynamoDBAttribute, @RoleRegistrationListFormat}))
    private List<RoleRegistration> roleRegistrations;
    @Getter(onMethod = @__({@DynamoDBVersioned}))
    private BigInteger version;

    /**
     * This setter does nothing.  It exists to appease {@link DynamoDBMapper},
     * which expects a setter.  Since there is only one GlobalConfig, the id
     * should not (and cannot) be changed.
     */
    public void setId(String ignored) {}

    public static GlobalConfig load(DynamoDBMapper dynamoDBMapper) {
        return dynamoDBMapper.load(new GlobalConfig());
    }
}
