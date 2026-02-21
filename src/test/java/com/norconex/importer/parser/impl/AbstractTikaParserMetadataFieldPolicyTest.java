/* Copyright 2015-2026 Norconex Inc.
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
package com.norconex.importer.parser.impl;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;

class AbstractTikaParserMetadataFieldPolicyTest {

    @Test
    void testDefaultPolicyIsBoth() {
        TestableTikaParser parser = new TestableTikaParser();
        Assertions.assertEquals(
                AbstractTikaParser.MetadataFieldPolicy.BOTH,
                parser.getMetadataFieldPolicy());
    }

    @Test
    void testLegacyPolicyStripsDcPrefix() {
        TestableTikaParser parser = new TestableTikaParser();
        parser.setMetadataFieldPolicy(AbstractTikaParser.MetadataFieldPolicy.LEGACY);

        Metadata tikaMeta = new Metadata();
        tikaMeta.set("dc:title", "My Title");

        Properties target = new Properties();
        parser.transfer(tikaMeta, target);

        Assertions.assertEquals("My Title", target.getString("title"));
        Assertions.assertNull(target.getString("dc:title"));
    }

    @Test
    void testPreservePolicyKeepsDcPrefix() {
        TestableTikaParser parser = new TestableTikaParser();
        parser.setMetadataFieldPolicy(AbstractTikaParser.MetadataFieldPolicy.PRESERVE);

        Metadata tikaMeta = new Metadata();
        tikaMeta.set("dc:title", "My Title");

        Properties target = new Properties();
        parser.transfer(tikaMeta, target);

        Assertions.assertEquals("My Title", target.getString("dc:title"));
        Assertions.assertNull(target.getString("title"));
    }

    @Test
    void testBothPolicyPublishesBothDcAndLegacyNames() {
        TestableTikaParser parser = new TestableTikaParser();
        parser.setMetadataFieldPolicy(AbstractTikaParser.MetadataFieldPolicy.BOTH);

        Metadata tikaMeta = new Metadata();
        tikaMeta.set("dc:title", "My Title");
        tikaMeta.set("author", "Jane");

        Properties target = new Properties();
        parser.transfer(tikaMeta, target);

        Assertions.assertEquals("My Title", target.getString("dc:title"));
        Assertions.assertEquals("My Title", target.getString("title"));
        Assertions.assertEquals("Jane", target.getString("author"));
    }

    private static class TestableTikaParser extends AbstractTikaParser {
        TestableTikaParser() {
            super(new AutoDetectParser());
        }

        void transfer(Metadata tikaMeta, Properties target) {
            addTikaMetadataToImporterMetadata(tikaMeta, target);
        }
    }
}
