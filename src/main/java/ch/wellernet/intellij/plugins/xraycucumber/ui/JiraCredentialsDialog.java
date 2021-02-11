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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JiraCredentialsDialog extends DialogWrapper {
    private JTextField usernameTextField;
    private JPasswordField passwordPasswordField;
    private JCheckBox storeCredentialsCheckBox;
    private JPanel rootPanel;

    private final ServiceParameters serviceParameters;

    public JiraCredentialsDialog(@Nullable Project project, ServiceParameters serviceParameters) {
        super(project);
        init();
        setTitle("Credential for " + serviceParameters.url().toExternalForm());
        this.serviceParameters = serviceParameters;
        usernameTextField.setText(serviceParameters.username());
        passwordPasswordField.setText(serviceParameters.password());
        storeCredentialsCheckBox.setSelected(ServiceParametersUtils.storeByDefault());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootPanel;
    }

    public ServiceParameters getUpdatedServiceParameters() {
        return serviceParameters.toBuilder()
                .username(usernameTextField.getText())
                .password(String.copyValueOf(passwordPasswordField.getPassword()))
                .build();
    }

    public boolean storeCredentials() {
        return storeCredentialsCheckBox.isSelected();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return usernameTextField.getText().isEmpty() ? usernameTextField : passwordPasswordField;
    }
}
