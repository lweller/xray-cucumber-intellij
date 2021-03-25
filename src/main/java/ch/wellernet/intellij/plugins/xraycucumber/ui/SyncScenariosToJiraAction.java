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
import io.cucumber.gherkin.GherkinDocumentBuilder;
import io.cucumber.gherkin.Parser;
import io.cucumber.gherkin.TokenMatcher;
import io.cucumber.messages.IdGenerator;
import lombok.val;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

import static io.cucumber.messages.Messages.GherkinDocument.Feature;

public class SyncScenariosToJiraAction extends AnAction {

    public static final String TITLE = "Synchronize Cucumber Scenarios to Jira";

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

        ProgressManager.getInstance().run(new Task.Backgroundable(project, TITLE) {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                val parser = new Parser<>(new GherkinDocumentBuilder(new IdGenerator.Incrementing()));

                val xrayCucumberService = new XrayCucumberService(HttpClients.createDefault());
                val progressReporter = new ProgressIndicatorAdapter(progressIndicator, project);
                Optional.ofNullable(FileDocumentManager.getInstance().getCachedDocument(featureFile))
                        .ifPresent(featureDocument -> {
                            val featureCode = featureDocument.getCharsSequence().toString();
                            val gherkinDocument =
                                    parser.parse(featureCode, new TokenMatcher()).build();
                            gherkinDocument.getFeature().getChildrenList().stream()
                                    .filter(Feature.FeatureChild::hasScenario)
                                    .map(Feature.FeatureChild::getScenario)
                                    .filter(SyncScenariosToJiraAction.this::hasNoJiraTag)
                                    .forEach(scenario ->
                                            xrayCucumberService.createNewJiraTestIssue(serviceParameters, progressReporter, scenario.getName()).ifPresent(
                                                    jiraIssueReference -> {
                                                        val offset = featureDocument.getLineStartOffset(scenario.getLocation().getLine());
                                                        featureDocument.insertString(offset, String.format("  @%s\n", jiraIssueReference.key()));
                                                    }
                                            ));
                        });
            }
        });
    }

    private boolean hasNoJiraTag(Feature.Scenario scenario) {
        return scenario.getTagsList().stream()
                .noneMatch(tag -> tag.getName().matches("[A-Z]+-\\d+"));
    }

    @Override
    public void update(AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean visible = file != null && file.getName().endsWith(".feature");
        event.getPresentation().setEnabledAndVisible(visible);
    }
}
