package org.admarple.barbeque.lambda.util;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import org.admarple.barbeque.lambda.GlobalConfig;
import org.admarple.barbeque.util.SetupUtil;

public class LambdaSetupUtil {
    /**
     * Setup DynamoDB on the account administering Barbeque.  Barbeque uses only a single table for
     * global config and shared resource locks.
     * @param dynamoDB
     * @param bucketName
     * @throws InterruptedException
     */
    public static void setupDynamo(AmazonDynamoDB dynamoDB, String bucketName) throws InterruptedException {
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(dynamoDB);
        CreateTableRequest createTableRequest = dynamoDBMapper.generateCreateTableRequest(GlobalConfig.class);
        try {
            dynamoDB.describeTable(createTableRequest.getTableName());
        } catch (ResourceNotFoundException e) {
            createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
            Table table = new DynamoDB(dynamoDB).createTable(createTableRequest);
            table.waitForActive();
        }

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setSecretBucketName(bucketName);
        dynamoDBMapper.save(globalConfig);
    }

    public static void setupDynamo(AmazonDynamoDB dynamoDB) throws InterruptedException {
        setupDynamo(dynamoDB, SetupUtil.TEST_BUCKET_NAME);
    }

    /**
     * Tear down DynamoDB on the acount administering Barbeque.  This method will revoke all locks
     * and render Barbeque non-functional on this account.
     * @param dynamoDB
     * @param bucketName
     * @throws InterruptedException
     */
    public static void emptyDynamo(AmazonDynamoDB dynamoDB, String bucketName) throws InterruptedException {
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(dynamoDB);
        DeleteTableRequest deleteTableRequest = dynamoDBMapper.generateDeleteTableRequest(GlobalConfig.class);
        Table table = new DynamoDB(dynamoDB).getTable(deleteTableRequest.getTableName());
        table.delete();
        table.waitForDelete();
    }

    public static void emptyDynamo(AmazonDynamoDB dynamoDB) throws InterruptedException {
        emptyDynamo(dynamoDB, SetupUtil.TEST_BUCKET_NAME);
    }
}
