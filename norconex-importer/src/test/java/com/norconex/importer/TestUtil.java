/* Copyright 2010-2019 Norconex Inc.
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
package com.norconex.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.Doc;

public final class TestUtil {

    private static final String BASE_PATH =
         "src/site/resources/examples/books/alice-in-wonderland-book-chapter-1";

    private TestUtil() {
        super();
    }

    public static String getContentAsString(Doc doc)
            throws IOException {
        return IOUtils.toString(doc.getInputStream(), StandardCharsets.UTF_8);
    }

    public static File getAlicePdfFile() {
        return new File(BASE_PATH + ".pdf");
    }
    public static File getAliceDocxFile() {
        return new File(BASE_PATH + ".docx");
    }
    public static File getAliceZipFile() {
        return new File(BASE_PATH + ".zip");
    }
    public static File getAliceHtmlFile() {
        return new File(BASE_PATH + ".html");
    }
    public static File getAliceTextFile() {
        return new File(BASE_PATH + ".txt");
    }
    public static Importer getTestConfigImporter() throws IOException {
        ImporterConfig config = new ImporterConfig();
        try (InputStream is =
                TestUtil.class.getResourceAsStream("test-config.xml");
                Reader r = new InputStreamReader(is)) {
            new XML(r).populate(config);
        }
        return new Importer(config);
    }
}
