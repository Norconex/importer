/* Copyright 2010-2014 Norconex Inc.
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

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.norconex.commons.lang.map.Properties;

public class ImportHandlerTest {

    private Importer importer;
    private Properties metadata;

    @Before
    public void setUp() throws Exception {
        importer = TestUtil.getTestConfigImporter();
        metadata = new Properties();
    }

    @After
    public void tearDown() throws Exception {
        importer = null;
        metadata = null;
    }
    
    @Test
    public void testHandlers() throws IOException, ImporterException {
        FileInputStream is = new FileInputStream(TestUtil.getAliceHtmlFile());
        importer.importDocument(is, metadata, "alice.html");
        is.close();

        // Test Constant
        Assert.assertEquals("Lewis Carroll", metadata.getString("Author"));
    }
}
