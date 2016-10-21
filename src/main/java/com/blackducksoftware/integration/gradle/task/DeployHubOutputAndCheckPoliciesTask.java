package com.blackducksoftware.integration.gradle.task;

import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES_ERROR;
import static com.blackducksoftware.integration.build.Constants.CREATE_HUB_OUTPUT_ERROR;
import static com.blackducksoftware.integration.build.Constants.DEPLOY_HUB_OUTPUT_AND_CHECK_POLICIES_FINISHED;
import static com.blackducksoftware.integration.build.Constants.DEPLOY_HUB_OUTPUT_AND_CHECK_POLICIES_STARTING;
import static com.blackducksoftware.integration.build.Constants.DEPLOY_HUB_OUTPUT_ERROR;

import java.io.IOException;
import java.net.URISyntaxException;

import org.gradle.api.GradleException;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.MissingUUIDException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

public class DeployHubOutputAndCheckPoliciesTask extends HubTask {
    private long hubScanStartedTimeout = 300;

    private long hubScanFinishedTimeout = 300;

    @Override
    public void performTask() {
        logger.info(String.format(DEPLOY_HUB_OUTPUT_AND_CHECK_POLICIES_STARTING, getBdioFilename()));

        try {
            PLUGIN_HELPER.createHubOutput(getProject(), getHubProjectName(), getHubVersionName(), getOutputDirectory());
        } catch (final IOException e) {
            throw new GradleException(String.format(CREATE_HUB_OUTPUT_ERROR, e.getMessage()), e);
        }

        final HubServerConfig hubServerConfig = getHubServerConfigBuilder().build();
        final RestConnection restConnection;
        try {
            restConnection = new CredentialsRestConnection(hubServerConfig);
            PLUGIN_HELPER.deployHubOutput(new Slf4jIntLogger(logger), restConnection, getOutputDirectory(),
                    getHubProjectName());
        } catch (IllegalArgumentException | URISyntaxException | BDRestException | EncryptionException | IOException
                | ResourceDoesNotExistException e) {
            throw new GradleException(String.format(DEPLOY_HUB_OUTPUT_ERROR, e.getMessage()), e);
        }

        try {
            PLUGIN_HELPER.waitForHub(restConnection, getHubProjectName(), getHubVersionName(), getHubScanStartedTimeout(),
                    getHubScanFinishedTimeout());
            final PolicyStatusItem policyStatusItem = PLUGIN_HELPER.checkPolicies(restConnection, getHubProjectName(),
                    getHubVersionName());
            handlePolicyStatusItem(policyStatusItem);
        } catch (IllegalArgumentException | URISyntaxException | BDRestException | IOException
                | ProjectDoesNotExistException | HubIntegrationException | MissingUUIDException | UnexpectedHubResponseException e) {
            throw new GradleException(String.format(CHECK_POLICIES_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(DEPLOY_HUB_OUTPUT_AND_CHECK_POLICIES_FINISHED, getBdioFilename()));
    }

    public long getHubScanStartedTimeout() {
        return hubScanStartedTimeout;
    }

    public void setHubScanStartedTimeout(long hubScanStartedTimeout) {
        this.hubScanStartedTimeout = hubScanStartedTimeout;
    }

    public long getHubScanFinishedTimeout() {
        return hubScanFinishedTimeout;
    }

    public void setHubScanFinishedTimeout(long hubScanFinishedTimeout) {
        this.hubScanFinishedTimeout = hubScanFinishedTimeout;
    }

}
