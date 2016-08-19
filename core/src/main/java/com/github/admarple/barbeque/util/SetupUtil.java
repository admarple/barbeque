package com.github.admarple.barbeque.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;

public class SetupUtil {
    public static final String TEST_BUCKET_NAME = "com.github.admarple.barbeque.test";

    public static void setupBucket(AmazonS3 s3, String bucketName) {
        if ( ! s3.doesBucketExist(TEST_BUCKET_NAME)) {
            CreateBucketRequest request = new CreateBucketRequest(TEST_BUCKET_NAME);
            s3.createBucket(request);
        }
    }

    public static void setupBucket(AmazonS3 s3) {
        setupBucket(s3, TEST_BUCKET_NAME);
    }

    /*
     * From http://docs.aws.amazon.com/AmazonS3/latest/dev/delete-or-empty-bucket.html#delete-bucket-sdk-java
     */
    public static void emptyBucket(AmazonS3 s3, String bucketName) {
        ObjectListing objectListing = s3.listObjects(bucketName);

        while (true) {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                s3.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        VersionListing list = s3.listVersions(new ListVersionsRequest().withBucketName(bucketName));
        for (S3VersionSummary s : list.getVersionSummaries()) {
            s3.deleteVersion(bucketName, s.getKey(), s.getVersionId());
        }
    }

    public static void emptyBucket(AmazonS3 s3) {
        emptyBucket(s3, TEST_BUCKET_NAME);
    }
}
