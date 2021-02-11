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

package ch.wellernet.intellij.plugins.xraycucumber;

import ch.wellernet.intellij.plugins.xraycucumber.model.FileReplacementBehaviour;
import ch.wellernet.intellij.plugins.xraycucumber.model.ServiceParameters;
import ch.wellernet.intellij.plugins.xraycucumber.service.ProgressReporter;
import ch.wellernet.intellij.plugins.xraycucumber.service.XrayCucumberService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class XrayCucumberServiceTests {

    private final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());

    private final HttpClient httpClient = mock(HttpClient.class);

    private final ProgressReporter progressReporter = mock(ProgressReporter.class);

    private final ServiceParameters serviceParameters = ServiceParameters.builder()
            .url(new URL("https://issues.example.com"))
            .projectKey("TEST")
            .username("mickeymouse")
            .password("daisy")
            .filterId(42L)
            .fileReplacementBehaviour(FileReplacementBehaviour.ASK)
            .build();

    private final Path outputDir = fileSystem.getPath("target/cucumber-tests");

    private final Path featureFile = fileSystem.getPath("target/cucumber-tests/mynew.feature");

    private final XrayCucumberService xrayCucumberService = new XrayCucumberService(httpClient);

    XrayCucumberServiceTests() throws MalformedURLException {
    }

    @Test
    void downloadXrayCucumberTests_successful() throws IOException {
        setupHttpResponse(HttpStatus.SC_OK, ContentType.APPLICATION_OCTET_STREAM);

        // act
        xrayCucumberService.downloadXrayCucumberTests(serviceParameters, outputDir, progressReporter);

        // assert
        assertThat(outputDir.resolve("mycool.feature")).exists();
        assertThat(outputDir.resolve("mycool.feature")).isNotEmptyFile();
        verify(progressReporter).reportProgress(any(), eq(0.));
        verify(progressReporter).reportSuccess(any());
        verifyNoMoreInteractions(progressReporter);
    }

    @Test
    void downloadXrayCucumberTests_featureAlreadyExists_shallNotOverride() throws IOException {
        ServiceParameters serviceParametersWithKeepExistingFiles = serviceParameters.toBuilder()
                .fileReplacementBehaviour(FileReplacementBehaviour.KEEP_EXISTING)
                .build();
        setupHttpResponse(HttpStatus.SC_OK, ContentType.APPLICATION_OCTET_STREAM);
        Files.createDirectories(outputDir);
        Files.createFile(outputDir.resolve("mycool.feature"));

        // act
        xrayCucumberService.downloadXrayCucumberTests(serviceParametersWithKeepExistingFiles, outputDir, progressReporter);

        // assert
        assertThat(outputDir.resolve("mycool.feature")).isEmptyFile();
        verify(progressReporter).reportProgress(any(), eq(0.));
        verify(progressReporter).reportSuccess(any());
        verifyNoMoreInteractions(progressReporter);
    }

    @Test
    void downloadXrayCucumberTests_featureAlreadyExists_shallOverride() throws IOException {
        ServiceParameters serviceParametersWithReplaceExistingFiles = serviceParameters.toBuilder()
                .fileReplacementBehaviour(FileReplacementBehaviour.REPLACE)
                .build();
        setupHttpResponse(HttpStatus.SC_OK, ContentType.APPLICATION_OCTET_STREAM);
        Files.createDirectories(outputDir);
        Files.createFile(outputDir.resolve("mycool.feature"));

        // act
        xrayCucumberService.downloadXrayCucumberTests(serviceParametersWithReplaceExistingFiles, outputDir, progressReporter);

        // assert
        assertThat(outputDir.resolve("mycool.feature")).isNotEmptyFile();
        verify(progressReporter).reportProgress(any(), eq(0.));
        verify(progressReporter).reportSuccess(any());
        verifyNoMoreInteractions(progressReporter);
    }

    @Test
    void downloadXrayCucumberTests_MissingFilterId() {
        ServiceParameters serviceParametersWithoutFilterId = serviceParameters.toBuilder().filterId(null).build();

        // act
        xrayCucumberService.downloadXrayCucumberTests(serviceParametersWithoutFilterId, outputDir, progressReporter);

        // assert
        verify(progressReporter).reportError(any(), any());
        verifyNoMoreInteractions(progressReporter);
    }


    @Test
    void downloadXrayCucumberTests_serverError() throws IOException {
        setupHttpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ContentType.TEXT_PLAIN);

        // act
        xrayCucumberService.downloadXrayCucumberTests(serviceParameters, outputDir, progressReporter);

        // assert
        verify(progressReporter).reportError(any(), any());
        verifyNoMoreInteractions(progressReporter);
    }

    @Test
    void downloadXrayCucumberTests_invalidContentType() throws IOException {
        setupHttpResponse(HttpStatus.SC_OK, ContentType.TEXT_HTML);

        // act
        xrayCucumberService.downloadXrayCucumberTests(serviceParameters, outputDir, progressReporter);

        // assert
        verify(progressReporter).reportError(any(), any());
        verifyNoMoreInteractions(progressReporter);
    }

    @Test
    void downloadXrayCucumberTests_authenticationFailed() throws IOException {
        setupHttpResponse(HttpStatus.SC_UNAUTHORIZED, ContentType.TEXT_PLAIN);

        // act
        xrayCucumberService.downloadXrayCucumberTests(serviceParameters, outputDir, progressReporter);

        // assert
        verify(progressReporter).reportAuthenticationError(any());
        verifyNoMoreInteractions(progressReporter);
    }

    @Test
    void uploadXrayCucumberTests_successful() throws IOException {
        setupHttpResponse(HttpStatus.SC_OK, ContentType.APPLICATION_JSON);
        Files.createDirectories(outputDir);
        Files.createFile(featureFile);

        // act
        xrayCucumberService.uploadXrayCucumberTest(serviceParameters, featureFile, progressReporter);

        // assert
        verify(progressReporter).reportSuccess(any());
        verifyNoMoreInteractions(progressReporter);
    }

    private void setupHttpResponse(int httpStatus, ContentType contentType) throws IOException {
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(httpStatus);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(TestData.sampleTestDownloadZip(TestData.MYCOOL_FEATURE_ZIP));
        when(httpEntity.getContentType()).thenReturn(new BasicHeader("content-type", contentType.getMimeType()));
    }
}