package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.github.admarple.barbeque.SecretException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * LockManager uses DynamoDB as a distributed lock.  It obtains a lock by writing a
 * {@link Lock} row for the desired lock with both the holder of the lock, and
 * an expiration time for the lock.  If the lock is already held, it checks to see
 * if it can expire the previous lock.
 *
 * Distributed locks based on expiration require all entities attempting to acquire
 * the lock to agree on time.  LockManager gets this from the following assumptions:
 *   1. Clients are at most 15 minutes skewed.  This is enforced by AWS (clients more
 *      than 15 minutes skewed will not be able to successfully authenticate).
 *   2. Clients will not attempt to perform work for longer than the duration of the
 *      lock.  This is enforced by setting a Lambda execution duration of 15 minutes
 *      or less (the default is 5 minutes).
 * With these two assumptions, a conditional PUT where the condition of:
 *   existing.expiration + MAX_SKEW < new.creation
 */
@Slf4j
public class LockManager {
    private static final int MAX_SKEW_SECONDS = 15 * 60;

    DynamoDBMapper dynamoDBMapper;
    DynamoDBMapperConfig config;

    public LockManager(DynamoDBMapper mapper) {
        this.dynamoDBMapper = mapper;
        this.config = new DynamoDBMapperConfig.Builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .build();
    }

    private String getLockId(String id) {
        return "lock:" + id;
    }

    public Lock getLock(String id, String owner, long expirationSeconds) {
        Lock lock = new Lock();
        lock.setId(getLockId(id));
        lock.setOwner(owner);
        // Expiration-based locking  does not work in a distributed setting without clock synchronization
        lock.setExpirationSeconds(expirationSeconds);
        DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression()
                .withConditionalOperator(ConditionalOperator.OR)
                .withExpectedEntry("id", new ExpectedAttributeValue().withExists(false))
                .withExpectedEntry("expiration", new ExpectedAttributeValue()
                        .withComparisonOperator(ComparisonOperator.LT)
                        .withValue(new AttributeValue()
                                .withN(Long.toString(Instant.now().plusSeconds(MAX_SKEW_SECONDS).getEpochSecond()))
                        )
                );

        try {
            dynamoDBMapper.save(lock, saveExpression);
            return lock;
        } catch (ConditionalCheckFailedException e) {
            log.info("Lock is already held by another client");
            throw new SecretException("Lock is already held by another client", e);
        }
    }

    public void release(Lock lock) {
        dynamoDBMapper.delete(lock);
    }
}
