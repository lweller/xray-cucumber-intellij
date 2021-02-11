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

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationUtils {
    private static final NotificationGroup NORMAL_NOTIFICATION_GROUP =
            new NotificationGroup("Xray Cucumber Notification", NotificationDisplayType.BALLOON, false);
    private static final NotificationGroup ERROR_NOTIFICATION_GROUP =
            new NotificationGroup("Xray Cucumber Error Notification", NotificationDisplayType.STICKY_BALLOON, false);

    void notifySuccess(String message, Project project) {
        ApplicationManager.getApplication().invokeLater(() ->
                NORMAL_NOTIFICATION_GROUP
                        .createNotification(message, NotificationType.INFORMATION)
                        .notify(project));
    }

    void notifyError(String message, Project project) {
        ApplicationManager.getApplication().invokeLater(() ->
                ERROR_NOTIFICATION_GROUP
                        .createNotification(message, NotificationType.ERROR)
                        .notify(project));
    }
}
