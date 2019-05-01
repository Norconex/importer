/* Copyright 2015-2019 Norconex Inc.
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

import org.junit.jupiter.api.Test;

import com.norconex.importer.ImporterException;


public class WPOfficeParserTest extends AbstractParserTest {

    //--- Quattro Pro ----------------------------------------------------------
    @Test
    public void test_WPOffice_QuattroPro_qpw()
            throws IOException, ImporterException {
        testParsing("/parser/wordperfect/quattropro.qpw",
                "application/x-quattro-pro; version=9",
                "Misc\\. relative references", "qpw", "Spreadsheet");
    }

    //--- Word Perfect ---------------------------------------------------------
    @Test
    public void test_WPOffice_WordPerfect_wpd()
            throws IOException, ImporterException {
        testParsing("/parser/wordperfect/wordperfect.wpd",
                "application/vnd.wordperfect; version=6.x",
                "test test", "wpd", "Word Processor");
    }
}
