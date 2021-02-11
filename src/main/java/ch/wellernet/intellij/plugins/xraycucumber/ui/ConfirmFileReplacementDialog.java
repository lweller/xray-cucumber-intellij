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

import ch.wellernet.intellij.plugins.xraycucumber.model.FileReplacementBehaviour;
import ch.wellernet.intellij.plugins.xraycucumber.model.FileReplacementDecision;
import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.nio.file.Path;

public class ConfirmFileReplacementDialog extends DialogWrapper {
    private JPanel rootPanel;
    private JLabel messageLabel;
    private JCheckBox useForAllCheckBox;

    public ConfirmFileReplacementDialog(@Nullable Project project, Path file) {
        super(project);
        init();
        setTitle("Replace Local Copy");
        messageLabel.setText("Are you sure to replace the local copy of '" + file + "' ?");
        myOKAction.putValue(Action.NAME, CommonBundle.getYesButtonText());
        myOKAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Y);
        myCancelAction.putValue(Action.NAME, CommonBundle.getNoButtonText());
        myCancelAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
    }

    FileReplacementDecision getFileReplacementDecision() {
        boolean replaceExistingFile = getExitCode() == OK_EXIT_CODE;
        FileReplacementBehaviour fileReplacementBehaviour;
        if (useForAllCheckBox.isSelected()) {
            fileReplacementBehaviour = replaceExistingFile ?
                    FileReplacementBehaviour.REPLACE :
                    FileReplacementBehaviour.KEEP_EXISTING;
        } else {
            fileReplacementBehaviour = FileReplacementBehaviour.ASK;
        }
        return new FileReplacementDecision(fileReplacementBehaviour, replaceExistingFile);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootPanel;
    }
}
