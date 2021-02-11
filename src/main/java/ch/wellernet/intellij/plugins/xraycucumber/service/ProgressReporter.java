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

package ch.wellernet.intellij.plugins.xraycucumber.service;

import ch.wellernet.intellij.plugins.xraycucumber.model.FileReplacementDecision;

import java.nio.file.Path;

public interface ProgressReporter {
    void reportProgress(String message, double completionRatio);

    void reportSuccess(String message);

    void reportAuthenticationError(String message);

    void reportError(String message, Exception exception);

    FileReplacementDecision askToReplaceExistingFile(Path file);
}
