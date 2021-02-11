/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package ch.wellernet.intellij.plugins.xraycucumber.ui;

import ch.wellernet.intellij.plugins.xraycucumber.model.ServiceParameters;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

@UtilityClass
public class ServiceParametersUtils {
    final String XRAY_CUCUMBER_JSON = "xray-cucumber.json";

    ServiceParameters prepareServiceParameters(Project project, VirtualFile serviceParametersFile) throws IOException {
        ServiceParameters serviceParameters = load(serviceParametersFile);
        serviceParameters = ServiceParametersUtils.retrieveCredentialsFromStoreIfUndefined(serviceParameters);
        if (serviceParameters.username() == null || serviceParameters.password() == null) {
            serviceParameters = requestJiraCredentialsFormUser(project, serviceParameters);
        }
        return serviceParameters;
    }

    private ServiceParameters load(VirtualFile serviceParametersFile) throws IOException {
        try (InputStream inputStream = serviceParametersFile.getInputStream()) {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
            return objectMapper.readValue(inputStream, ServiceParameters.class);
        }
    }

    private ServiceParameters retrieveCredentialsFromStoreIfUndefined(ServiceParameters serviceParameters) {
        return Optional.ofNullable(PasswordSafe.getInstance().get(createCredentialAttributes(serviceParameters.url())))
                .map(c -> serviceParameters.toBuilder()
                        .username(serviceParameters.username() == null ? c.getUserName() : serviceParameters.username())
                        .password(serviceParameters.password() == null ? c.getPasswordAsString() : serviceParameters.password())
                        .build())
                .orElse(serviceParameters);
    }

    private ServiceParameters requestJiraCredentialsFormUser(Project project, ServiceParameters serviceParameters) {
        JiraCredentialsDialog jiraCredentialsDialog = new JiraCredentialsDialog(project, serviceParameters);
        if (jiraCredentialsDialog.showAndGet()) {
            serviceParameters = jiraCredentialsDialog.getUpdatedServiceParameters();
            if (jiraCredentialsDialog.storeCredentials()) {
                ServiceParametersUtils.storeCredentials(serviceParameters);
            } else {
                ServiceParametersUtils.deleteCredentials(serviceParameters);
            }
            return serviceParameters;
        }
        return null;
    }

    void storeCredentials(ServiceParameters serviceParameters) {
        Credentials credentials = new Credentials(serviceParameters.username(), serviceParameters.password());
        PasswordSafe.getInstance().set(createCredentialAttributes(serviceParameters.url()), credentials);
    }

    void deleteCredentials(ServiceParameters serviceParameters) {
        PasswordSafe.getInstance().set(createCredentialAttributes(serviceParameters.url()), null);
    }

    boolean storeByDefault() {
        return PasswordSafe.getInstance().isRememberPasswordByDefault();
    }

    private CredentialAttributes createCredentialAttributes(URL jiraUrl) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("Jira", jiraUrl.toExternalForm()));
    }
}
