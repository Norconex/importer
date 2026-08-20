/* Copyright 2014-2020 Norconex Inc.
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
package com.norconex.importer.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.ImporterRequest;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.parser.impl.ExternalParser;

public class GenericDocumentParserFactoryTest {

        private static final String SENTIMENT_PARSER =
                        "org.apache.tika.parser.sentiment.SentimentAnalysisParser";

        @Test
        public void testWriteRead() {
                GenericDocumentParserFactory f = new GenericDocumentParserFactory();

                // default read/write
                // XMLConfigurationUtil.assertWriteRead(f);

                // more complex read/write
                f.setIgnoredContentTypesRegex("test");
                EmbeddedConfig emb = f.getParseHints().getEmbeddedConfig();
                emb.setNoExtractContainerContentTypes("noExtractContainerTest");
                emb.setNoExtractEmbeddedContentTypes("noExtractEmbeddedTest");
                emb.setSplitContentTypes(".*");

                OCRConfig ocr = f.getParseHints().getOcrConfig();
                ocr.setContentTypes("ocrContentTypesTest");
                ocr.setLanguages("ocrLanguages");
                ocr.setPath("ocrPath");

                ExternalParser app = new ExternalParser();
                app.setCommand("command.exe");
                f.registerParser(ContentType.BMP, app);
                XML.assertWriteRead(f, "documentParserFactory");
        }

        @Test
        public void testSentimentParserExcludedFromTikaServiceLoading()
                        throws Exception {
                // Tika's SentimentAnalysisParser downloads its model while the
                // service loader instantiates it, i.e. while TikaConfig is
                // being built, in the parser factory constructor. That is
                // before any Importer configuration is read, so it can only be
                // prevented by a service-loader exclusion in tika-config.xml.
                // Dropping it from the chain afterwards is too late: the
                // download (and its stall on a slow network) already happened.
                Assertions.assertDoesNotThrow(
                                () -> Class.forName(SENTIMENT_PARSER),
                                "Sentiment parser missing from classpath; test "
                                                + "would pass vacuously.");

                URL configUrl = getClass().getResource("/tika-config.xml");
                Assertions.assertNotNull(
                                configUrl, "Importer tika-config.xml not found.");
                Assertions.assertTrue(
                                IOUtils.toString(configUrl, StandardCharsets.UTF_8)
                                                .contains(SENTIMENT_PARSER),
                                "tika-config.xml must exclude " + SENTIMENT_PARSER
                                                + " from service loading.");

                Assertions.assertFalse(
                                chainContainsSentimentParser(
                                                new GenericDocumentParserFactory()),
                                "Sentiment parser should be disabled by default.");
        }

        @Test
        public void testSentimentParserUnavailableModelIsNotFatal()
                        throws Exception {
                GenericDocumentParserFactory factory =
                                new GenericDocumentParserFactory();
                factory.loadFromXML(new XML("<documentParserFactory>"
                                + "<sentiment enabled=\"true\" "
                                + "modelPath=\"no-such-sentiment-model.bin\"/>"
                                + "</documentParserFactory>"));

                // An unreachable model must be reported and skipped, never
                // propagated as a failure that stops the crawler.
                Assertions.assertDoesNotThrow(() -> factory.getParser(
                                "n/a", ContentType.TEXT));
                Assertions.assertFalse(
                                chainContainsSentimentParser(factory),
                                "Sentiment parser should be absent when its model "
                                                + "cannot be loaded.");
        }

        private boolean chainContainsSentimentParser(
                        GenericDocumentParserFactory factory) throws Exception {
                Object fallback =
                                FieldUtils.readField(factory, "fallbackParser", true);
                return contains(
                                (CompositeParser) FieldUtils.readField(
                                                fallback, "parser", true));
        }

        private boolean contains(CompositeParser cp) throws Exception {
                Field parsersField = CompositeParser.class.getDeclaredField("parsers");
                parsersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Parser> parserList = (List<Parser>) parsersField.get(cp);
                for (Parser p : parserList) {
                        Parser wrapped = p;
                        while (wrapped instanceof ParserDecorator) {
                                wrapped = ((ParserDecorator) wrapped).getWrappedParser();
                        }
                        if (SENTIMENT_PARSER.equals(wrapped.getClass().getName())) {
                                return true;
                        }
                        if (wrapped instanceof CompositeParser
                                        && contains((CompositeParser) wrapped)) {
                                return true;
                        }
                }
                return false;
        }

        @Test
        public void testIgnoringContentTypes() throws IOException {

                GenericDocumentParserFactory factory = new GenericDocumentParserFactory();
                factory.setIgnoredContentTypesRegex("application/pdf");
                Properties metadata = new Properties();

                ImporterConfig config = new ImporterConfig();
                config.setParserFactory(factory);
                Importer importer = new Importer(config);
                Doc doc = importer.importDocument(
                                new ImporterRequest(TestUtil.getAlicePdfFile().toPath())
                                                .setContentType(ContentType.PDF)
                                                .setMetadata(metadata)
                                                .setReference("n/a"))
                                .getDocument();

                try (InputStream is = doc.getInputStream()) {
                        String output = IOUtils.toString(
                                        is, StandardCharsets.UTF_8).substring(0, 100);
                        output = StringUtils.remove(output, '\n');
                        Assertions.assertTrue(
                                        !StringUtils.isAsciiPrintable(output),
                                        "Non-parsed output expected to be binary.");
                }
        }
}
