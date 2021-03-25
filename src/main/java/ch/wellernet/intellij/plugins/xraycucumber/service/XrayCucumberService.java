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

import ch.wellernet.intellij.plugins.xraycucumber.model.FileReplacementBehaviour;
import ch.wellernet.intellij.plugins.xraycucumber.model.JiraIssue;
import ch.wellernet.intellij.plugins.xraycucumber.model.JiraIssueReference;
import ch.wellernet.intellij.plugins.xraycucumber.model.JiraIssueType;
import ch.wellernet.intellij.plugins.xraycucumber.model.JiraKey;
import ch.wellernet.intellij.plugins.xraycucumber.model.JiraProject;
import ch.wellernet.intellij.plugins.xraycucumber.model.ServiceParameters;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang.UnhandledException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RequiredArgsConstructor
public class XrayCucumberService {

    public static final String REST_ENDPOINT_EXPORT_TEST = "/rest/raven/1.0/export/test";
    public static final String REST_ENDPOINT_IMPORT_FEATURE = "/rest/raven/1.0/import/feature";
    public static final String REST_ENDPOINT_ISSUE = "/rest/api/2/issue";

    private final HttpClient httpClient;

    /**
     * @see "https://docs.getxray.app/display/XRAY/Exporting+Cucumber+Tests+-+REST"
     */
    public void downloadXrayCucumberTests(ServiceParameters serviceParameters, Path outputDir, ProgressReporter progressReporter) {
        executeAndHandleExceptions(progressReporter,
                () -> buildDownloadRequest(serviceParameters),
                httpResponse -> extractDownloadedXrayCucumberTests(serviceParameters, progressReporter, outputDir, httpResponse));
    }

    private HttpUriRequest buildDownloadRequest(ServiceParameters serviceParameters)
            throws AuthenticationException, URISyntaxException {
        Long filterId = Optional.ofNullable(serviceParameters.filterId())
                .orElseThrow(() -> new IllegalArgumentException("filterId is required to download cucumber tests"));
        URIBuilder uriBuilder = new URIBuilder(serviceParameters.url() + REST_ENDPOINT_EXPORT_TEST)
                .addParameter("filter", String.valueOf(filterId))
                .addParameter("fz", String.valueOf(true));
        HttpUriRequest request = new HttpGet(uriBuilder.build());
        addAuthentication(serviceParameters, request);
        return request;
    }

    @Nullable
    private Object extractDownloadedXrayCucumberTests(ServiceParameters serviceParameters, ProgressReporter progressReporter, Path outputDir, HttpResponse httpResponse)
            throws IOException {
        val httpEntity = httpResponse.getEntity();
        verifyContentType(httpEntity, ContentType.APPLICATION_OCTET_STREAM);
        try (ZipInputStream zipInputStream = new ZipInputStream(httpEntity.getContent())) {
            val nullSafeProgressReporter = Optional.ofNullable(progressReporter);
            int testCount = 0;
            Files.createDirectories(outputDir);
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // as we total numbers of entries is not know in advance, estimate it as exponential increasing
                double completionRatio = testCount / Math.pow(10, testCount % 10);
                String fileName = entry.getName();
                nullSafeProgressReporter
                        .ifPresent((reporter -> reporter.reportProgress("extraction " + fileName, completionRatio)));
                extractFileFromZip(progressReporter, serviceParameters.fileReplacementBehaviour(), outputDir, zipInputStream, entry);
                testCount++;
            }
            int totalTestCount = testCount;
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportSuccess("extracted successfully " + totalTestCount + " cucumber tests"));
        }
        return null;
    }

    /**
     * @see "https://docs.getxray.app/display/XRAY/Importing+Cucumber+Tests+-+REST"
     */
    public void uploadXrayCucumberTest(ServiceParameters serviceParameters, Path featureFile, ProgressReporter progressReporter) {
        executeAndHandleExceptions(progressReporter,
                () -> buildFeatureUploadRequest(serviceParameters, featureFile),
                httpResponse -> handleUploadXrayCucumberTestResponse(progressReporter, featureFile, httpResponse.getEntity()));
    }

    private HttpUriRequest buildFeatureUploadRequest(ServiceParameters serviceParameters, Path featureFile)
            throws AuthenticationException, URISyntaxException, IOException {
        String projectKey = Optional.ofNullable(serviceParameters.projectKey())
                .orElseThrow(() -> new IllegalArgumentException("projectKey is required to download cucumber tests"));
        URIBuilder uriBuilder = new URIBuilder(serviceParameters.url() + REST_ENDPOINT_IMPORT_FEATURE)
                .addParameter("projectKey", projectKey);
        HttpPost request = new HttpPost(uriBuilder.build());
        addAuthentication(serviceParameters, request);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", Files.newInputStream(featureFile), ContentType.APPLICATION_JSON, featureFile.getFileName().toString())
                .build();
        request.setEntity(entity);
        return request;
    }

    private Void handleUploadXrayCucumberTestResponse(ProgressReporter progressReporter, Path featureFile, HttpEntity httpEntity) {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        verifyContentType(httpEntity, ContentType.APPLICATION_JSON);
        nullSafeProgressReporter.ifPresent(reporter -> reporter.reportSuccess("uploaded successfully " + featureFile));
        return null;
    }

    public Optional<JiraIssueReference> createNewJiraTestIssue(ServiceParameters serviceParameters, ProgressReporter progressReporter, String summary) {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        Optional<JiraIssueReference> jiraIssueReference =
                createJiraTestIssue(serviceParameters, progressReporter, summary)
                        .flatMap(ref -> updateJiraTestIssue(serviceParameters, progressReporter, ref.key(), summary));
        nullSafeProgressReporter.ifPresent(reporter ->
                jiraIssueReference.ifPresent(
                        ref -> reporter.reportSuccess(String.format("created successfully Jira Test %s", jiraIssueReference.get().key()))));

        return jiraIssueReference;
    }

    @Nonnull
    private Optional<JiraIssueReference> createJiraTestIssue(ServiceParameters serviceParameters, ProgressReporter progressReporter, String summary) {
        return executeAndHandleExceptions(progressReporter,
                () -> buildJiraTestIssueCreationRequest(serviceParameters, summary),
                httpResponse -> handleJiraIssueResponse(progressReporter, httpResponse));
    }

    @Nonnull
    private Optional<? extends JiraIssueReference> updateJiraTestIssue(ServiceParameters serviceParameters, ProgressReporter progressReporter, JiraKey key, String summary) {
        return executeAndHandleExceptions(progressReporter,
                () -> buildJiraTestIssueUpdateRequest(serviceParameters, key, summary),
                httpResponse -> handleJiraIssueResponse(progressReporter, httpResponse));
    }

    @Nonnull
    private HttpPost buildJiraTestIssueCreationRequest(ServiceParameters serviceParameters, String summary) throws AuthenticationException, JsonProcessingException {
        val request = new HttpPost(serviceParameters.url() + REST_ENDPOINT_ISSUE);
        addAuthentication(serviceParameters, request);
        val jiraIssue = JiraIssue.builder()
                .field("summary", summary)
                .field("project", JiraProject.of(serviceParameters.projectKey()))
                .field("issuetype", JiraIssueType.of("Test"))
                .build();
        HttpEntity entity = new StringEntity(createObjectMapper().writeValueAsString(jiraIssue), ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        return request;
    }

    @Nonnull
    private HttpPut buildJiraTestIssueUpdateRequest(ServiceParameters serviceParameters, JiraKey key, String summary) throws AuthenticationException, JsonProcessingException {
        val request = new HttpPut(serviceParameters.url() + REST_ENDPOINT_ISSUE + "/" + key);
        addAuthentication(serviceParameters, request);
        val jiraIssue = JiraIssue.builder()
                .fields(Optional.ofNullable(serviceParameters.additionalTestFieldValues()).orElse(Collections.emptyMap()))
                .field("summary", summary)
                .build();
        HttpEntity entity = new StringEntity(createObjectMapper().writeValueAsString(jiraIssue), ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        return request;
    }

    @Nullable
    private JiraIssueReference handleJiraIssueResponse(ProgressReporter progressReporter, HttpResponse response) throws IOException {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            verifyContentType(response.getEntity(), ContentType.APPLICATION_JSON);
            return createObjectMapper().convertValue(response.getEntity().getContent(), JiraIssueReference.class);
        } else {
            String responseMessage = EntityUtils.toString(response.getEntity());
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportSuccess(String.format("Jira operation failed: %s", responseMessage)));
            return null;
        }
    }

    private void addAuthentication(ServiceParameters serviceParameters, HttpUriRequest request) throws AuthenticationException {
        String userName = Optional.ofNullable(serviceParameters.username()).orElseThrow(() -> new AuthenticationException("user is required"));
        String password = Optional.ofNullable(serviceParameters.password()).orElseThrow(() -> new AuthenticationException("password is required"));
        UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(userName, password);
        request.addHeader(new BasicScheme().authenticate(usernamePasswordCredentials, request, null));
    }

    @Nonnull
    private <T> Optional<T> executeAndHandleExceptions(ProgressReporter progressReporter, HttpRequestSupplier httpRequestSupplier, HttpResponseHandler<T> httpResponseHandler) {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        try {
            HttpResponse httpResponse = httpClient.execute(httpRequestSupplier.get());
            HttpEntity httpEntity = httpResponse.getEntity();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                throw new AuthenticationException("Jira refused authentication (HTTP 401)");
            }
            if (statusCode != HttpStatus.SC_OK) {
                ContentType contentType = ContentType.getOrDefault(httpEntity);
                String message = "unexpected error";
                if (contentType.getMimeType().equals(ContentType.TEXT_PLAIN.getMimeType()) ||
                        contentType.getMimeType().equals(ContentType.APPLICATION_JSON.getMimeType())) {
                    message = EntityUtils.toString(httpEntity);
                }
                throw new IllegalStateException(message + " (HTTP " + statusCode + ")");
            }
            return Optional.ofNullable(httpResponseHandler.handle(httpResponse));
        } catch (AuthenticationException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportAuthenticationError(e.getMessage()));
        } catch (URISyntaxException | IllegalArgumentException | IllegalStateException | IOException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportError(e.getMessage(), e));
        }
        return Optional.empty();
    }

    private void verifyContentType(HttpEntity httpEntity, ContentType expectedContentType) {
        ContentType contentType = ContentType.getOrDefault(httpEntity);
        if (!contentType.getMimeType().equals(expectedContentType.getMimeType())) {
            throw new IllegalStateException("expected " + expectedContentType.getMimeType() + " but received " + contentType);
        }
    }

    private void extractFileFromZip(ProgressReporter progressReporter, FileReplacementBehaviour fileReplacementBehaviour, Path outputDir, ZipInputStream zipInputStream, ZipEntry entry) throws IOException {
        Path featureFileName = outputDir.resolve(entry.getName());
        if (Files.exists(featureFileName)
                && !replaceLocalCopy(progressReporter, fileReplacementBehaviour, featureFileName)) {
            return;
        }
        try (OutputStream outputStream = Files.newOutputStream(featureFileName)) {
            int len;
            byte[] buffer = new byte[1024];
            while ((len = zipInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        }
        zipInputStream.closeEntry();
    }

    private boolean replaceLocalCopy(ProgressReporter progressReporter, FileReplacementBehaviour fileReplacementBehaviour, Path featureFileName) {
        if (fileReplacementBehaviour == FileReplacementBehaviour.ASK) {
            return progressReporter.askToReplaceExistingFile(featureFileName).isReplaceExistingFile();
        }
        return fileReplacementBehaviour == FileReplacementBehaviour.REPLACE;
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
    }

    @FunctionalInterface
    private interface HttpRequestSupplier {
        HttpUriRequest get() throws URISyntaxException, AuthenticationException, IOException;
    }

    @FunctionalInterface
    private interface HttpResponseHandler<T> {
        T handle(HttpResponse httpResponse) throws IOException;
    }

}

