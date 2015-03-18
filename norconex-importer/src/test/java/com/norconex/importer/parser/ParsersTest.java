/* Copyright 2015 Norconex Inc.
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

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;

//TODO create a CSV file with all the test data?
public class ParsersTest extends AbstractParserTest {

    public static final String DEFAULT_CONTENT_REGEX = 
            "Hey Norconex, this is a test\\.";
    
//    private static final Logger LOG = 
//            LogManager.getLogger(ParsersTest.class);
    
    @Before
    public void before() {
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.INFO);
        logger.setAdditivity(false);
        logger.addAppender(new ConsoleAppender(
                new PatternLayout("%-5p [%C{1}] %m%n"), 
                ConsoleAppender.SYSTEM_OUT));
    }
    
    
//    @Test
//    public void test_WordPerfect_QuattroPro_qpw() 
//            throws IOException, ImporterException {
//        testParsing("/parser/wordperfect/quattropro.qpw",
//                "application/x-quattro-pro", "Sheet\\sC.*68", "qpw", "TODO");
//    }

    //--- Microsoft Word -------------------------------------------------------
    // OOXML formats:
    @Test
    public void test_MSOffice_Word_docx() throws Exception {
        testParsing("/parser/msoffice/word.docx",
                "application/vnd.openxmlformats-officedocument."
                        + "wordprocessingml.document",
                DEFAULT_CONTENT_REGEX, "docx", "Word Processor");
    }
    @Test
    public void test_MSOffice_Word_docm() throws Exception {
        testParsing("/parser/msoffice/word.docm",
                "application/vnd.ms-word.document.macroenabled.12",
                DEFAULT_CONTENT_REGEX, "docm", "Word Processor");
    }
    @Test
    public void test_MSOffice_Word_dotm() throws Exception {
        testParsing("/parser/msoffice/word.dotm",
                "application/vnd.ms-word.template.macroenabled.12",
                DEFAULT_CONTENT_REGEX, "dotm", "Word Processor");
    }
    @Test
    public void test_MSOffice_Word_dotx() throws Exception {
        testParsing("/parser/msoffice/word.dotx",
                "application/vnd.openxmlformats-officedocument."
                        + "wordprocessingml.template",
                DEFAULT_CONTENT_REGEX, "dotx", "Word Processor");
    }
    // OLE formats:
    @Test
    public void test_MSOffice_Word_doc() throws Exception {
        testParsing("/parser/msoffice/word.doc",
                "application/msword",
                DEFAULT_CONTENT_REGEX, "doc", "Word Processor");
    }
    @Test
    public void test_MSOffice_Word_dot() throws Exception {
        testParsing("/parser/msoffice/word.dot",
                "application/msword",
                DEFAULT_CONTENT_REGEX, "doc", "Word Processor");
    }

    //--- Microsoft PowerPoint -------------------------------------------------
    // OOXML formats:
    @Test
    public void test_MSOffice_PowerPoint_pptx() throws Exception {
        testParsing("/parser/msoffice/powerpoint.pptx",
                "application/vnd.openxmlformats-officedocument."
                        + "presentationml.presentation",
                DEFAULT_CONTENT_REGEX, "pptx", "Presentation");
    }
    @Test
    public void test_MSOffice_PowerPoint_pptm() throws Exception {
        testParsing("/parser/msoffice/powerpoint.pptm",
                "application/vnd.ms-powerpoint.presentation.macroenabled.12",
                DEFAULT_CONTENT_REGEX, "pptm", "Presentation");
    }
    @Test
    public void test_MSOffice_PowerPoint_potm() throws Exception {
        testParsing("/parser/msoffice/powerpoint.potm",
                "application/vnd.ms-powerpoint.template.macroenabled.12",
                DEFAULT_CONTENT_REGEX, "potm", "Presentation");
    }
    @Test
    public void test_MSOffice_PowerPoint_potx() throws Exception {
        testParsing("/parser/msoffice/powerpoint.potx",
                "application/vnd.openxmlformats-officedocument"
                        + ".presentationml.template",
                DEFAULT_CONTENT_REGEX, "potx", "Presentation");
    }
    @Test
    public void test_MSOffice_PowerPoint_ppsm() throws Exception {
        testParsing("/parser/msoffice/powerpoint.ppsm",
                "application/vnd.ms-powerpoint.slideshow.macroenabled.12",
                DEFAULT_CONTENT_REGEX, "ppsm", "Presentation");
    }
    @Test
    public void test_MSOffice_PowerPoint_ppsx() throws Exception {
        testParsing("/parser/msoffice/powerpoint.ppsx",
                "application/vnd.openxmlformats-officedocument"
                        + ".presentationml.slideshow",
                DEFAULT_CONTENT_REGEX, "ppsx", "Presentation");
    }
    
    // OLE formats:
    @Test
    public void test_MSOffice_PowerPoint_ppt() throws Exception {
        testParsing("/parser/msoffice/powerpoint.ppt",
                "application/vnd.ms-powerpoint",
                DEFAULT_CONTENT_REGEX, "ppt", "Presentation");
    }
    @Test
    public void test_MSOffice_PowerPoint_pot() throws Exception {
        testParsing("/parser/msoffice/powerpoint.pot",
                "application/vnd.ms-powerpoint",
                DEFAULT_CONTENT_REGEX, "ppt", "Presentation");
    }
    @Test
    public void test_MSOffice_PowerPoint_pps() throws Exception {
        testParsing("/parser/msoffice/powerpoint.pps",
                "application/vnd.ms-powerpoint",
                DEFAULT_CONTENT_REGEX, "ppt", "Presentation");
    }
    
}
