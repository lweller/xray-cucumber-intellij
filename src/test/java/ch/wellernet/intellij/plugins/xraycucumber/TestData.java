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

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class TestData {
    public static final String MYCOOL_FEATURE_ZIP = "UEsDBBQAAAAIAGtPR1IHh/NBiwAAAMMAAAAOAAAAbXljb29sLmZlYXR1cmU9jb0KwkAQhGsDeYd5AG3EylYQLAQhAetDJ+bwchvvR8nbu9FwW+3Ot3xzpEk5cI/zhIOIwxJgg8YMo2MJOgloGZP1D1xyGCWyroDmRm+ClWIoiVKda0+PE0x8/gypJ7JLdjCJeOXZJx7SwdmO6z/29s0Q9TL+Duo+pV5b62rVqmz+URI/DLARu+0XUEsBAj8AFAAAAAgAa09HUgeH80GLAAAAwwAAAA4AJAAAAAAAAAAgAAAAAAAAAG15Y29vbC5mZWF0dXJlCgAgAAAAAAABABgA3E8Thi/91gHdKBOGL/3WATJf/YUv/dYBUEsFBgAAAAABAAEAYAAAALcAAAAAAA==";

    public static InputStream sampleTestDownloadZip(String base64EncodedData) {
        byte[] data = Base64.decodeBase64(base64EncodedData);
        return new ByteArrayInputStream(data);
    }
}
