package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;
import lombok.Getter;

@Data
@DynamoDBTable(tableName = "BBQLambdaUtil")
public class Lock {
    @Getter(onMethod = @__({@DynamoDBHashKey}))
    private String id;
    @Getter(onMethod = @__({@DynamoDBAttribute}))
    private String owner;
    @Getter(onMethod = @__({@DynamoDBAttribute}))
    private Long creationSeconds;
    @Getter(onMethod = @__({@DynamoDBAttribute}))
    private Long expirationSeconds;
}
