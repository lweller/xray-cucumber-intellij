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
import ch.wellernet.intellij.plugins.xraycucumber.service.XrayCucumberService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Paths;

public class DownloadTestsFromJiraAction extends AnAction {

    public static final String TITLE = "Downloading Cucumber Xray Tests from Jira";

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();

        FileDocumentManager.getInstance().saveAllDocuments();
        VirtualFile serviceParametersFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (serviceParametersFile == null) {
            NotificationUtils.notifyError("this action requires a valid " +
                    ServiceParametersUtils.XRAY_CUCUMBER_JSON + " file", project);
            return;
        }

        ServiceParameters serviceParameters;
        try {
            serviceParameters = ServiceParametersUtils.prepareServiceParameters(project, serviceParametersFile);
        } catch (IOException exception) {
            NotificationUtils.notifyError(exception.getMessage(), project);
            return;
        }

        if(serviceParameters == null) {
            return;
        }

        VirtualFile outputDir = serviceParametersFile.getParent();

        downloadXrayCucumberTests(project, outputDir, serviceParameters);
    }

    private void downloadXrayCucumberTests(Project project, VirtualFile outputDir, ServiceParameters serviceParameters) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, TITLE) {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                XrayCucumberService xrayCucumberService = new XrayCucumberService(HttpClients.createDefault());
                ProgressIndicatorAdapter progressReporter = new ProgressIndicatorAdapter(progressIndicator, project);
                xrayCucumberService.downloadXrayCucumberTests(serviceParameters, Paths.get(outputDir.getPath()), progressReporter);
                outputDir.refresh(true, true);
                if (progressReporter.authenticationFailure() != null) {
                    ServiceParametersUtils.deleteCredentials(serviceParameters);
                    NotificationUtils.notifyError(progressReporter.authenticationFailure() + "<br>Stored credentials have been removed.", project);
                }
            }
        });
    }

    @Override
    public void update(AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean visible = file != null && file.getName().equals(ServiceParametersUtils.XRAY_CUCUMBER_JSON);
        event.getPresentation().setEnabledAndVisible(visible);
    }
}
