package com.github.admarple.barbeque.lambda.role;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.CreatePolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.DeletePolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyResult;
import com.amazonaws.services.identitymanagement.model.GetPolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyVersionResult;
import com.amazonaws.services.identitymanagement.model.ListPolicyVersionsRequest;
import com.amazonaws.services.identitymanagement.model.ListPolicyVersionsResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Role;
import com.github.admarple.barbeque.SecretMetadata;
import com.github.admarple.barbeque.lambda.GlobalConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class RoleRepository {
    AmazonIdentityManagement iam;
    DynamoDBMapper dynamoDBMapper;

    public List<Role> getRoles() {
        List<Role> roles = new ArrayList<>();
        GlobalConfig globalConfig = GlobalConfig.load(dynamoDBMapper);
        for (RoleRegistration roleRegistration : globalConfig.getRoleRegistrations()) {
            roles.addAll(roleRegistration.getRoles(iam));
        }
        return roles;
    }

    public void registerRole(RoleRegistration roleRegistration) {
        GlobalConfig globalConfig = GlobalConfig.load(dynamoDBMapper);
        globalConfig.getRoleRegistrations().add(roleRegistration);
        dynamoDBMapper.save(globalConfig);
    }

    public void grantPermission(SecretMetadata secretMetadata, String roleArn) {
        log.info("Attempting to grant permissions to {} for role {}", secretMetadata.getSecretId(), roleArn);
        GlobalConfig globalConfig = GlobalConfig.load(dynamoDBMapper);

        String policyArn = bbqPolicyArn(roleArn);
        Policy policy = getExistingOrEmptyPolicy(policyArn);

        policy.getStatements().add(new Statement(Statement.Effect.Allow)
                .withActions(S3Actions.GetObject)
                .withResources(resourceForSecret(secretMetadata, globalConfig)));

        deleteOldPolicyVersion(policyArn);

        iam.createPolicyVersion(new CreatePolicyVersionRequest()
                .withPolicyArn(policyArn)
                .withPolicyDocument(policy.toJson())
                .withSetAsDefault(true)
        );
        log.info("Granted permissions to {} for role {}", secretMetadata.getSecretId(), roleArn);
    }

    public void revokePermission(SecretMetadata secretMetadata, String roleArn) {
        log.info("Attempting to revoke permissions to {} for role {}", secretMetadata.getSecretId(), roleArn);
        GlobalConfig globalConfig = GlobalConfig.load(dynamoDBMapper);

        String policyArn = bbqPolicyArn(roleArn);
        Policy policy = getExistingOrEmptyPolicy(policyArn);

        boolean deleted = policy.getStatements().removeIf(statement ->
                statement.getActions().size() == 1
                        && statement.getActions().contains(S3Actions.GetObject)
                        && statement.getResources().size() == 1
                        && statement.getResources().contains(resourceForSecret(secretMetadata, globalConfig)));
        if ( ! deleted) {
            log.info("Existing Policy does not contain a Barbeque-managed statement granting permission");
        }

        deleteOldPolicyVersion(policyArn);

        iam.createPolicyVersion(new CreatePolicyVersionRequest()
                .withPolicyArn(policyArn)
                .withPolicyDocument(policy.toJson())
                .withSetAsDefault(true)
        );
        log.info("Revoked permissions to {} for role {}", secretMetadata.getSecretId(), roleArn);
    }

    private Policy getExistingOrEmptyPolicy(String policyArn) {
        GetPolicyResult getPolicyResult;
        try {
            getPolicyResult = iam.getPolicy(new GetPolicyRequest().withPolicyArn(policyArn));
        } catch (NoSuchEntityException e) {
            log.info("Policy {} does not exist", policyArn);
            return new Policy();
        }

        String versionId = getPolicyResult.getPolicy().getDefaultVersionId();
        try {
            GetPolicyVersionResult getPolicyVersionResult = iam.getPolicyVersion(new GetPolicyVersionRequest()
                    .withPolicyArn(policyArn)
                    .withVersionId(versionId));
            return Policy.fromJson(getPolicyVersionResult.getPolicyVersion().getDocument());
        } catch (NoSuchEntityException e) {
            log.info("Version {} for policy {} does not exist", versionId, policyArn);
            return new Policy();
        }
    }

    /**
     * IAM only support a limited number of versions per policy, so we delete the oldest version that is not the default.
     *
     * @param policyArn
     */
    private void deleteOldPolicyVersion(String policyArn) {
        ListPolicyVersionsResult listPolicyVersionsResult = iam.listPolicyVersions(new ListPolicyVersionsRequest()
                .withPolicyArn(policyArn));
        listPolicyVersionsResult.getVersions().stream()
                .filter(version -> !version.isDefaultVersion())
                .sorted(Comparator.comparing(v -> v.getCreateDate()))
                .findFirst()
                .ifPresent(oldestVersion -> {
                    log.info("Attempting to delete old policy version {} for policy {}", oldestVersion, policyArn);
                    iam.deletePolicyVersion(new DeletePolicyVersionRequest()
                            .withPolicyArn(policyArn)
                            .withVersionId(oldestVersion.getVersionId()));
                    log.info("Deleted old policy version {} for policy {}", oldestVersion, policyArn);
                });
    }

    private String bbqPolicyArn(String roleArn) {
        return "barbeque_" + roleArn;
    }

    private String secretResourcePattern(String secretId) {
        return secretId + "*";
    }

    private Resource resourceForSecret(SecretMetadata secretMetadata, GlobalConfig globalConfig) {
        return new S3ObjectResource(
                globalConfig.getSecretBucketName(),
                secretResourcePattern(secretMetadata.getSecretId()));
    }
}
