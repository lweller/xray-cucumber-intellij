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
import java.nio.file.Path;

public class UploadTestsToJiraAction extends AnAction {

    public static final String TITLE = "Uploading Cucumber Tests to Jira";

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getProject();

        FileDocumentManager.getInstance().saveAllDocuments();
        VirtualFile featureFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (featureFile == null) {
            NotificationUtils.notifyError("this action requires a valid feature file", project);
            return;
        }

        VirtualFile serviceParametersFile = featureFile.findFileByRelativePath("../" + ServiceParametersUtils.XRAY_CUCUMBER_JSON);
        if (serviceParametersFile == null) {
            NotificationUtils.notifyError("this action requires a valid " + ServiceParametersUtils.XRAY_CUCUMBER_JSON
                    + " file located in same directory as feature file", project);
            return;
        }

        ServiceParameters serviceParameters;
        try {
            serviceParameters = ServiceParametersUtils.prepareServiceParameters(project, serviceParametersFile);
        } catch (IOException exception) {
            NotificationUtils.notifyError(exception.getMessage(), project);
            return;
        }

        if (serviceParameters == null) {
            return;
        }

        VirtualFile inputDir = serviceParametersFile.getParent();

        uploadXrayCucumberTests(project, inputDir,serviceParameters);
    }

    private void uploadXrayCucumberTests(Project project, VirtualFile inputDir, ServiceParameters serviceParameters){
        ProgressManager.getInstance().run(new Task.Backgroundable(project, TITLE) {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                XrayCucumberService xrayCucumberService = new XrayCucumberService(HttpClients.createDefault());
                ProgressIndicatorAdapter progressReporter = new ProgressIndicatorAdapter(progressIndicator, project);
                VirtualFile[] featureList = inputDir.getChildren();
                Path zipFilePath;
                try {
                    zipFilePath = xrayCucumberService.zipCucumberTests(progressReporter,inputDir, featureList);
                }catch(Exception exception){
                    NotificationUtils.notifyError(exception.getMessage(), project);
                    return;
                }
                xrayCucumberService.uploadXrayCucumberTestZip(serviceParameters, zipFilePath, progressReporter);
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
