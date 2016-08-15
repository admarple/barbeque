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
