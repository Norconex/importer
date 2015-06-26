/* Copyright 2010-2015 Norconex Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.parser.impl.FallbackParser;
import com.norconex.importer.parser.impl.quattro.QuattroProParser;
import com.norconex.importer.parser.impl.wordperfect.WordPerfectParser;
import com.norconex.importer.parser.impl.xfdl.XFDLParser;
import com.norconex.importer.response.ImporterResponse;

/**
 * <p>Generic document parser factory.  It uses Apacke Tika for <i>most</i> of 
 * its supported content types.  For unknown
 * content types, it falls back to Tika generic media detector/parser.</p>
 * 
 * <h3>Ignoring content types:</h3>
 * <p>As of version 2.0.0, you can "ignore" content-types so they do not get
 * parsed.  Unparsed documents will be sent as is to the post handlers 
 * and the calling application.   Use caution when using that feature since
 * post-parsing handlers (or applications) usually expect text-only content for 
 * them to execute properly.  Unless you really know what you are doing, <b> 
 * avoid excluding binary content types from parsing.</b></p>
 * 
 * <h3>Embedded documents:</h3>
 * <p>For documents containing embedded documents (e.g. zip files), the default 
 * behavior of this treat them as a single document, merging all
 * embedded documents content and metadata into the parent document.
 * As of version 2.0.0, you can tell this parser to "split" embedded
 * documents to have them treated as if they were individual documents.  When
 * split, each embedded documents will go through the entire import cycle, 
 * going through your handlers and even this parser again
 * (just like any regular document would).  The resulting 
 * {@link ImporterResponse} should then contain nested documents, which in turn,
 * might contain some (tree-like structure).</p>
 * 
 * <h3>Optical character recognition (OCR):</h3>
 * <p>Starting with version 2.1.0, you can configure this parser to use the
 * <b><a href="https://code.google.com/p/tesseract-ocr/">Tesseract</a></b> 
 * open-source OCR application to extract text out of images
 * or documents containing embedded images (e.g. PDF).  Supported image
 * formats are TIFF, PNG, JPEG, GIF, and BMP.</p>
 * 
 * <p>To enable this feature, you must 
 * first download and install a copy of Tesseract appropriate for 
 * your platform (supported are Linux, Windows, Mac and other platforms).  
 * It will only be activated once you configure the path to its install 
 * location.  
 * Default language detection is for English. To support additional or 
 * different languages,
 * you can provide a list of three-letter ISO language codes supported
 * by Tesseract. These languages must be part of your Tesseract installation.
 * You can <a href="https://code.google.com/p/tesseract-ocr/downloads/list">
 * download additional languages</a> form the Tesseract web site.</p>
 * 
 * <p>When enabled, OCR is attempted on all supported image formats.  To
 * limit OCR to a subset of document content types, configure the corresponding 
 * content-types (e.g. application/pdf, image/tiff, image/png, etc.).</p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;documentParserFactory 
 *          class="com.norconex.importer.parser.GenericDocumentParserFactory" 
 *          splitEmbedded="(false|true)" &gt;
 *      &lt;ocr path="(path to Tesseract OCR software install)"&gt;
 *          &lt;languages&gt;
 *              (optional coma-separated list of Tesseract languages)
 *          &lt;/languages&gt;
 *          &lt;contentTypes&gt;
 *              (optional regex matching content types to limit OCR on)
 *          &lt;/contentTypes&gt;
 *      &lt;/ocr&gt;
 *      &lt;ignoredContentTypes&gt;
 *          (optional regex matching content types to ignore for parsing, 
 *           i.e., not parsed.)
 *      &lt;/ignoredContentTypes&gt;
 *  &lt;/documentParserFactory&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see Pattern
 */
@SuppressWarnings("nls")
public class GenericDocumentParserFactory 
        implements IDocumentParserFactory, IXMLConfigurable {

    private static final Logger LOG = 
            LogManager.getLogger(GenericDocumentParserFactory.class);
    
    private final Map<ContentType, IDocumentParser> namedParsers = 
            new HashMap<>();
    private IDocumentParser fallbackParser;

    private String ignoredContentTypesRegex;
    private OCRConfig ocrConfig;
    private boolean splitEmbedded;
    
    private boolean parsersAreUpToDate = false;
    
    /**
     * Creates a new document parser factory of the given format.
     */
    public GenericDocumentParserFactory() {
        super();
    }
    
    /**
     * Gets the OCR configuration.
     * @return the ocrConfig
     */
    public OCRConfig getOCRConfig() {
        return ocrConfig;
    }
    /**
     * Sets the OCR configuration.
     * @param ocrConfig the ocrConfig to set
     */
    public synchronized void setOCRConfig(OCRConfig ocrConfig) {
        if (!Objects.equals(this.ocrConfig, ocrConfig)) {
            parsersAreUpToDate = false;
        }
        this.ocrConfig = ocrConfig;
    }

    /**
     * Gets a parser based on content type, regardless of document reference
     * (ignoring it).
     */
    @Override
    public final IDocumentParser getParser(
            String documentReference, ContentType contentType) {
        // If ignoring content-type, do not even return a parser
        if (contentType != null 
                && StringUtils.isNotBlank(ignoredContentTypesRegex)
                && contentType.toString().matches(ignoredContentTypesRegex)) {
            return null;
        }
        
        ensureParsersAreUpToDate();
        IDocumentParser parser = namedParsers.get(contentType);
        if (parser == null) {
            return fallbackParser;
        }
        return parser;
    }

    /**
     * Gets the regular expression matching content types to ignore
     * (i.e. do not perform parsing on them).
     * @return regular expression
     */
    public String getIgnoredContentTypesRegex() {
        return ignoredContentTypesRegex;
    }
    /**
     * sets the regular expression matching content types to ignore
     * (i.e. do not perform parsing on them).
     * @param ignoredContentTypesRegex regular expression
     */
    public void setIgnoredContentTypesRegex(String ignoredContentTypesRegex) {
        this.ignoredContentTypesRegex = ignoredContentTypesRegex;
    }

    /**
     * Gets whether embedded documents should be split to become "standalone"
     * distinct documents.
     * @return <code>true</code> if parser should split embedded documents.
     */
    public boolean isSplitEmbedded() {
        return splitEmbedded;
    }
    /**
     * Sets whether embedded documents should be split to become "standalone"
     * distinct documents.
     * @param splitEmbedded <code>true</code> if parser should split 
     *                      embedded documents.
     */
    public synchronized void setSplitEmbedded(boolean splitEmbedded) {
        if (this.splitEmbedded != splitEmbedded) {
            parsersAreUpToDate = false;
        }
        this.splitEmbedded = splitEmbedded;
    }

    /**
     * Creates associations between specific content types and the parsers
     * that should be used to parse them.  Content types not having 
     * a parser explicitly associated by this method will be parsed by the 
     * fall-back parser (if not ignore and a fall-back parser exists).
     * by this method 
     * @return association between content types and parsers
     * @since 2.1.0
     */
    protected Map<ContentType, IDocumentParser> createNamedParsers() {
        Map<ContentType, IDocumentParser> parsers = new HashMap<>();
        
        // Word Perfect
        IDocumentParser wp = new WordPerfectParser();
        parsers.put(ContentType.valueOf("application/wordperfect"), wp);
        parsers.put(ContentType.valueOf("application/wordperfect6.0"), wp);
        parsers.put(ContentType.valueOf("application/wordperfect6.1"), wp);
        parsers.put(ContentType.valueOf("application/x-corel-wordperfect"), wp);
        parsers.put(ContentType.valueOf("application/wordperfect5.1"), wp);
        parsers.put(ContentType.valueOf("application/vnd.wordperfect"), wp);
        
        // PureEdge XLDF
        parsers.put(
                ContentType.valueOf("application/vnd.xfdl"), new XFDLParser());
              
        // Quattro Pro:
        parsers.put(ContentType.valueOf(
                "application/x-quattro-pro"), new QuattroProParser());
        
        return parsers;
    }
    
    /**
     * Creates a parser that will act as the default parser when no 
     * associated parser is found for any given content type.
     * @return document parser
     * @since 2.1.0
     */
    protected IDocumentParser createFallbackParser() {
        FallbackParser parser = new FallbackParser();
        parser.setSplitEmbedded(splitEmbedded);
        parser.setOCRConfig(ocrConfig);
        return parser;
    }
    
    private synchronized void ensureParsersAreUpToDate() {
        if (!parsersAreUpToDate) {
            namedParsers.clear();
            Map<ContentType, IDocumentParser> parsers = createNamedParsers();
            if (parsers == null) {
                LOG.info("No named parsers created.");
            } else {
                namedParsers.putAll(parsers);
            }
            fallbackParser = createFallbackParser();
            if (fallbackParser == null) {
                LOG.info("No fallback parser.");
            }
            parsersAreUpToDate = true;

            validateOCRInstall();
        }
    }
    
    //TODO Should this be a generic utility method?
    //TODO Validate languagues in config matches those installed.
    //TODO Print out Tesseract version and path on startup?
    private void validateOCRInstall() {
        if (ocrConfig == null) {
            LOG.debug("OCR parsing is disabled.");
            return;
        }
        
        if (StringUtils.isBlank(ocrConfig.getPath())) {
            LOG.error("Parser OCR configuration supplied without a path.");
        } else {
            String exePath = ocrConfig.getPath();
            if(!exePath.endsWith(File.separator)) {
                exePath += File.separator;
            }
            File exeFile = new File(exePath + (System.getProperty(
                    "os.name").startsWith("Windows") 
                                    ? "tesseract.exe" : "tesseract"));
            if (!exeFile.exists()) {
                LOG.error("OCR path specified but the Tesseract executable "
                        + "was not found: " + exeFile.getAbsolutePath());
            } else if (!exeFile.isFile()) {
                LOG.error("OCR path does not point to a file: "
                        + exeFile.getAbsolutePath());
            } else {
                LOG.info("OCR parsing is enabled.");
            }
        }
    }
    
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
            setIgnoredContentTypesRegex(xml.getString(
                    "ignoredContentTypes", getIgnoredContentTypesRegex()));
            setSplitEmbedded(xml.getBoolean(
                    "[@splitEmbedded]", isSplitEmbedded()));
            Configuration ocrXml = xml.subset("ocr");
            OCRConfig ocrConfig = null;
            if (!ocrXml.isEmpty()) {
                ocrConfig = new OCRConfig();
                ocrConfig.setPath(ocrXml.getString("[@path]"));
                ocrConfig.setLanguages(ocrXml.getString("languages"));
                ocrConfig.setContentTypes(ocrXml.getString("contentTypes"));
            }
            setOCRConfig(ocrConfig);
        } catch (ConfigurationException e) {
            throw new IOException("Cannot load XML.", e);
        }
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter xml = new EnhancedXMLStreamWriter(out);
            xml.writeStartElement("tagger");
            xml.writeAttribute("class", getClass().getCanonicalName());
            xml.writeAttribute("splitEmbedded", 
                    Boolean.toString(isSplitEmbedded()));
            if (ignoredContentTypesRegex != null) {
                xml.writeStartElement("ignoredContentTypes");
                xml.writeCharacters(ignoredContentTypesRegex);
                xml.writeEndElement();
            }
            if (ocrConfig != null) {
                xml.writeStartElement("ocr");
                xml.writeAttributeString("path", ocrConfig.getPath());
                xml.writeElementString("languages", ocrConfig.getLanguages());
                xml.writeElementString("contentTypes", ocrConfig.getContentTypes());
                xml.writeEndElement();
            }
            xml.writeEndElement();
            xml.flush();
            xml.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

}
