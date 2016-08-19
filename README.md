# barbeque
Barbeque is a secret-management solution.  It is built using AWS technologies and intended to be server-less.

# Building Locally
## Build

After running `./gradlew clean build`, you will see jars for all subprojects, as well as a fat-jar
in lambda.  The fat-jar contains the classes intended for use on AWS Lambda, as well as all of their
dependencies, so it can be used as a standalone jar.

## Integration Tests
To run integration tests, you will need to setup AWS credentials.  Create a new IAM user, and add it to your AWS
configuration (see [the AWS docs](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-config-files)).
The user will need full permissions on the test bucket
(see [here](https://github.com/admarple/barbeque/blob/master/src/test/java/org/admarple/barbeque/client/s3/S3SecretClientIntegrationTest.java#L30)
for the bucket name).  The Policy would look like:

```javascript
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:*"
      ],
      "Resource": [
        "arn:aws:s3:::com.github.admarple.barbeque.test",
        "arn:aws:s3:::com.github.admarple.barbeque.test/*"
      ]
    }
  ]
}
```

When running the integration test, you can specify the AWS profile you setup, e.g.

```
./gradlew :core:integrationTest -Paws.profile=barbeque-build-local
```

## Setup

Coming soon, utilities to setup AWS resources to start up Barbeque on your AWS account.