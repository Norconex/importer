/* Copyright 2014-2017 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.importer.handler.splitter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.norconex.commons.lang.map.Properties;

/**
 * @since 2.0.0
 */
public class SplittableDocument {

    private final String reference;
    private final InputStream input;
    private final Properties metadata;
    
    public SplittableDocument(
            String reference, InputStream input, Properties metadata) {
        super();
        this.reference = reference;
        this.input = input;
        this.metadata = metadata;
    }

    public String getReference() {
        return reference;
    }
    public InputStream getInput() {
        return input;
    }
    public Reader getReader() {
        return new InputStreamReader(input, StandardCharsets.UTF_8);
    }
    public Properties getMetadata() {
        return metadata;
    }
}
