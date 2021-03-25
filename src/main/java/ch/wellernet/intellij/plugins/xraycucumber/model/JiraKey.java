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

package ch.wellernet.intellij.plugins.xraycucumber.model;

import lombok.Value;
import lombok.experimental.Accessors;
import lombok.val;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

@Value(staticConstructor = "of")
@Accessors(fluent = true)
public class JiraKey {

    private static final Pattern PATTERN = Pattern.compile("([A-Z]+)-(\\d+)");

    public static JiraKey valueOf(String key) {
        val matcher = PATTERN.matcher(key);
        if(!matcher.matches()) {
            throw new IllegalArgumentException(String.format("%s is not a valid Jira key.", key));
        }
        return JiraKey.of(matcher.group(1), Long.parseLong(matcher.group(2)));
    }

    @Nonnull
    String project;

    long id;

    public String toString() {
        return String.format("%s-%s", project, id);
    }
}
