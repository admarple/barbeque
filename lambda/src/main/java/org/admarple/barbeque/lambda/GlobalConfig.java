package org.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;
import lombok.Getter;

@Data
@DynamoDBTable(tableName = "BBQLambdaUtil")
public class GlobalConfig {
    @Getter(onMethod = @__({@DynamoDBHashKey}))
    private final String id = "GlobalConfig";
    @Getter(onMethod = @__({@DynamoDBAttribute}))
    private String secretBucketName;

    public static GlobalConfig load(DynamoDBMapper dynamoDBMapper) {
        return dynamoDBMapper.load(new GlobalConfig());
    }
}
