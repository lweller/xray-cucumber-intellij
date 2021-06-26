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
import ch.wellernet.intellij.plugins.xraycucumber.model.ServiceParameters;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.UnhandledException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
public class XrayCucumberService {

    public static final String REST_ENDPOINT_EXPORT_TEST = "/rest/raven/1.0/export/test";
    public static final String REST_ENDPOINT_IMPORT_FEATURE = "/rest/raven/1.0/import/feature";

    private final HttpClient httpClient;

    /**
     * @see "https://docs.getxray.app/display/XRAY/Exporting+Cucumber+Tests+-+REST"
     */
    public void downloadXrayCucumberTests(ServiceParameters serviceParameters, Path outputDir, ProgressReporter progressReporter) {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        try {
            HttpUriRequest request = buildDownloadRequest(serviceParameters);
            HttpEntity httpEntity = executeRequest(request);
            verifyContentType(httpEntity, ContentType.APPLICATION_OCTET_STREAM);
            try (ZipInputStream zipInputStream = new ZipInputStream(httpEntity.getContent())) {
                extractFilesFromZip(progressReporter, serviceParameters.fileReplacementBehaviour(), zipInputStream, outputDir, nullSafeProgressReporter);
            }
        } catch (AuthenticationException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportAuthenticationError(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException | URISyntaxException | IOException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportError(e.getMessage(), e));
        }
    }

    /**
     * @see "https://docs.getxray.app/display/XRAY/Importing+Cucumber+Tests+-+REST"
     */
    public void uploadXrayCucumberTest(ServiceParameters serviceParameters, Path featureFile, ProgressReporter progressReporter) {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        try {
            HttpUriRequest request = buildFeatureUploadRequest(serviceParameters, featureFile);
            HttpEntity httpEntity = executeRequest(request);
            verifyContentType(httpEntity, ContentType.APPLICATION_JSON);
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportSuccess("uploaded successfully " + featureFile));
        } catch (AuthenticationException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportAuthenticationError(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException | URISyntaxException | IOException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportError(e.getMessage(), e));
        }
    }

    public void uploadXrayCucumberTestZip(ServiceParameters serviceParameters, Path zipFilePath, ProgressReporter progressReporter) {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        try {
            HttpUriRequest request = buildZipUploadRequest(serviceParameters, zipFilePath);
            HttpEntity httpEntity = executeRequest(request);
            verifyContentType(httpEntity, ContentType.APPLICATION_JSON);
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportSuccess("uploaded successfully " + zipFilePath));
        } catch (AuthenticationException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportAuthenticationError(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException | URISyntaxException | IOException e) {
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportError(e.getMessage(), e));
        }
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

    private HttpUriRequest buildZipUploadRequest(ServiceParameters serviceParameters, Path zipFile)
            throws AuthenticationException, URISyntaxException, IOException {
        String projectKey = Optional.ofNullable(serviceParameters.projectKey())
                .orElseThrow(() -> new IllegalArgumentException("projectKey is required to download cucumber tests"));
        URIBuilder uriBuilder = new URIBuilder(serviceParameters.url() + REST_ENDPOINT_IMPORT_FEATURE)
                .addParameter("projectKey", projectKey);
        HttpPost request = new HttpPost(uriBuilder.build());
        addAuthentication(serviceParameters, request);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", Files.newInputStream(zipFile),
                        ContentType.APPLICATION_JSON, zipFile.getFileName().toString())
                .build();
        request.setEntity(entity);
        return request;
    }

    private void addAuthentication(ServiceParameters serviceParameters, HttpUriRequest request) throws AuthenticationException {
        String userName = Optional.ofNullable(serviceParameters.username()).orElseThrow(() -> new AuthenticationException("user is required"));
        String password = Optional.ofNullable(serviceParameters.password()).orElseThrow(() -> new AuthenticationException("password is required"));
        UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(userName, password);
        request.addHeader(new BasicScheme().authenticate(usernamePasswordCredentials, request, null));
    }

    private HttpEntity executeRequest(HttpUriRequest request) throws AuthenticationException, IOException {
        HttpResponse httpResponse = httpClient.execute(request);
        HttpEntity httpEntity = httpResponse.getEntity();
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            throw new AuthenticationException("Jira refused authentication (HTTP 401)");
        }
        if (statusCode != HttpStatus.SC_OK) {
            ContentType contentType = ContentType.getOrDefault(httpEntity);
            String message = "unexpected error";
            if (contentType.getMimeType().equals(ContentType.TEXT_PLAIN.getMimeType())) {
                message = EntityUtils.toString(httpEntity);
            }
            throw new IllegalStateException(message + " (HTTP " + statusCode + ")");
        }
        return httpEntity;
    }

    private void extractFilesFromZip(ProgressReporter progressReporter, FileReplacementBehaviour fileReplacementBehaviour, ZipInputStream zipInputStream, Path outputDir, Optional<ProgressReporter> nullSafeProgressReporter) throws IOException {
        int testCount = 0;
        Files.createDirectories(outputDir);
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            // as we total numbers of entries is not know in advance, estimate it as exponential increasing
            double completionRatio = testCount / Math.pow(10, testCount % 10);
            String fileName = entry.getName();
            nullSafeProgressReporter
                    .ifPresent((reporter -> reporter.reportProgress("extraction " + fileName, completionRatio)));
            extractFileFromZip(progressReporter, fileReplacementBehaviour, outputDir, zipInputStream, entry);
            testCount++;
        }
        int totalTestCount = testCount;
        nullSafeProgressReporter.ifPresent(reporter -> reporter.reportSuccess("extracted successfully " + totalTestCount + " cucumber tests"));
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

    public Path zipCucumberTests(ProgressReporter progressReporter,VirtualFile outputLocation,VirtualFile[] featureFiles) {
        Optional<ProgressReporter> nullSafeProgressReporter = Optional.ofNullable(progressReporter);
        File zipFile;
        zipFile = new File(outputLocation.getPath()+File.separator+outputLocation.getName()+".zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipOutputStream.setLevel(ZipOutputStream.STORED);

            for (VirtualFile f : featureFiles) {
                if (Objects.equals(f.getExtension(), "feature")) {
                    ZipEntry entry = new ZipEntry(f.getName());
                    zipOutputStream.putNextEntry(entry);
                    zipOutputStream.write(f.contentsToByteArray());
                    zipOutputStream.closeEntry();
                }
            }
        }catch(Exception e){
            if (!nullSafeProgressReporter.isPresent()) {
                throw new UnhandledException(e);
            }
            nullSafeProgressReporter.ifPresent(reporter -> reporter.reportAuthenticationError(e.getMessage()));
        }
        return zipFile.toPath();
    }
}

