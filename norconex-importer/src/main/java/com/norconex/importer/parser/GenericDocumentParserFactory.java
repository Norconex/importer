/* Copyright 2010-2018 Norconex Inc.
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
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.tika.parser.jdbc.SQLite3Parser;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.parser.impl.FallbackParser;
import com.norconex.importer.parser.impl.xfdl.XFDLParser;
import com.norconex.importer.response.ImporterResponse;

/**
 * <p>Generic document parser factory.  It uses Apacke Tika for <i>most</i> of
 * its supported content types.  For unknown
 * content types, it falls back to Tika generic media detector/parser.</p>
 *
 * <p>As of 2.6.0, it is possible to register your own parsers.</p>
 *
 * <h3>Ignoring content types:</h3>
 * <p>You can "ignore" content-types so they do not get
 * parsed. Unparsed documents will be sent as is to the post handlers
 * and the calling application.   Use caution when using that feature since
 * post-parsing handlers (or applications) usually expect text-only content for
 * them to execute properly.  Unless you really know what you are doing, <b>
 * avoid excluding binary content types from parsing.</b></p>
 *
 * <h3>Character encoding:</h3>
 * <p>Parsing a document also attempts to detect the character encoding
 * (charset) of the extracted text to converts it to UTF-8. When ignoring
 * content-types, the character encoding conversion to UTF-8 cannot
 * take place and your documents will likely retain their original encoding.
 * </p>
 *
 * <h3>Embedded documents:</h3>
 * <p>For documents containing embedded documents (e.g. zip files), the default
 * behavior of this treat them as a single document, merging all
 * embedded documents content and metadata into the parent document.
 * You can tell this parser to "split" embedded
 * documents to have them treated as if they were individual documents.  When
 * split, each embedded documents will go through the entire import cycle,
 * going through your handlers and even this parser again
 * (just like any regular document would).  The resulting
 * {@link ImporterResponse} should then contain nested documents, which in turn,
 * might contain some (tree-like structure). As of 2.6.0, this is enabled by
 * specifying a regular expression to match content types of container
 * documents you want to "split".
 * </p>
 *
 * <p>In addition, since 2.6.0 you can control which embedded documents you
 * do not want extracted from their containers, as well as which documents
 * containers you do not want to extract their embedded documents.
 * </p>
 *
 * <h3>Optical character recognition (OCR):</h3>
 * <p>You can configure this parser to use the
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
 *         class="com.norconex.importer.parser.GenericDocumentParserFactory"&gt;
 *
 *      &lt;ocr path="(path to Tesseract OCR software install)"&gt;
 *          &lt;languages&gt;
 *              (optional coma-separated list of Tesseract languages)
 *          &lt;/languages&gt;
 *          &lt;contentTypes&gt;
 *              (optional regex matching content types to limit OCR on)
 *          &lt;/contentTypes&gt;
 *      &lt;/ocr&gt;
 *
 *      &lt;ignoredContentTypes&gt;
 *          (optional regex matching content types to ignore for parsing,
 *           i.e., not parsed)
 *      &lt;/ignoredContentTypes&gt;
 *
 *      &lt;embedded&gt;
 *          &lt;splitContentTypes&gt;
 *              (optional regex matching content types of containing files
 *               you want to "split" and have their embedded documents
 *               treated as individual documents)
 *          &lt;/splitContentTypes&gt;
 *          &lt;noExtractEmbeddedContentTypes&gt;
 *              (optional regex matching content types of embedded files you do
 *               not want to extract from containing documents, regardless of
 *               the container content type)
 *          &lt;/noExtractEmbeddedContentTypes&gt;
 *          &lt;noExtractContainerContentTypes&gt;
 *              (optional regex matching content types of containing files you
 *               do not want to see their embedded files extracted, regardless
 *               of the embedded content types)
 *          &lt;/noExtractContainerContentTypes&gt;
 *      &lt;/embedded&gt;
 *
 *      &lt;fallbackParser
 *          class="(optionally overwrite the fallback parser)" /&gt;
 *
 *      &lt;parsers&gt;
 *          &lt;!-- Optionally overwrite default parsers.
 *               You can configure many parsers. --&gt;
 *          &lt;parser
 *              contentType="(content type)"
 *              class="(IDocumentParser implementing class)" /&gt;
 *      &lt;/parsers&gt;
 *
 *  &lt;/documentParserFactory&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following uses Tesseract to convert English and French images in PDF
 * into text and it will also extract documents from Zip files and treat
 * them as separate documents.
 * </p>
 * <pre>
 *  &lt;documentParserFactory&gt;
 *      &lt;ocr path="/app/tesseract/"&gt;
 *          &lt;languages&gt;en, fr&lt;/languages&gt;
 *          &lt;contentTypes&gt;application/pdf&lt;/contentTypes&gt;
 *      &lt;/ocr&gt;
 *      &lt;embedded&gt;
 *          &lt;splitContentTypes&gt;application/zip&lt;/splitContentTypes&gt;
 *      &lt;/embedded&gt;
 *  &lt;/documentParserFactory&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class GenericDocumentParserFactory
        implements IDocumentParserFactory, IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(GenericDocumentParserFactory.class);

    private final Map<ContentType, IDocumentParser> parsers =
            new HashMap<>();
    private final ParseHints parseHints = new ParseHints();
    private IDocumentParser fallbackParser;

    private String ignoredContentTypesRegex;

    private boolean parsersAreUpToDate = false;

    /**
     * Creates a new document parser factory of the given format.
     */
    public GenericDocumentParserFactory() {
        super();
        fixTikaInitWarning();

        //have all parsers lazy loaded instead?
        initDefaultParsers();
    }

    private void fixTikaInitWarning() {

        // A check for Tesseract OCR parser is done the first time a Tika
        // parser is used.  We remove this check since we manage Tesseract OCR
        // via Importer config only.
        try {
            FieldUtils.writeStaticField(
                    TesseractOCRParser.class, "HAS_WARNED", true, true);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            LOG.warn("Could not disable invalid Tessaract OCR warning. "
                   + "If you see such warning, you can ignore.");
        }

        // A check for SQL-Lite is also done and we do not want it.
        try {
            FieldUtils.writeStaticField(
                    SQLite3Parser.class, "HAS_WARNED", true, true);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            LOG.warn("Could not disable \"sqlite-jdbc\" warning. "
                   + "If you see such warning, you can ignore.");
        }
    }

    protected void initDefaultParsers() {
        // Fallback parser
        fallbackParser = new FallbackParser();

        //TODO delete when released in Tika:
        //https://issues.apache.org/jira/browse/TIKA-2222
        // PureEdge XFDL
        parsers.put(
                ContentType.valueOf("application/vnd.xfdl"), new XFDLParser());
    }

    /**
     * Gets parse hints.
     * @return parse hints
     * @since 2.6.0
     */
    public ParseHints getParseHints() {
        return parseHints;
    }

    /**
     * Registers a parser to use for the given content type. The provided
     * parser will never be used if the content type
     * is ignored by {@link #getIgnoredContentTypesRegex()}.
     * @param contentType content type
     * @param parser parser
     * @since 2.6.0
     */
    public void registerParser(
            ContentType contentType, IDocumentParser parser) {
        parsers.put(contentType, parser);
    }

    /**
     * Gets a parser based on content type, regardless of document reference
     * (ignoring it).
     * All parsers are assumed to have been configured properly
     * before the first call to this method.
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

        ensureParseHintsState();
        IDocumentParser parser = parsers.get(contentType);
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

    private synchronized void ensureParseHintsState() {
        if (!parsersAreUpToDate) {
            for (Entry<ContentType, IDocumentParser> entry :
                parsers.entrySet()) {
                IDocumentParser parser = entry.getValue();
                initParseHints(parser);
            }
            initParseHints(fallbackParser);
            parsersAreUpToDate = true;
            validateOCRInstall();
        }
    }
    private void initParseHints(IDocumentParser parser) {
        if (parser instanceof IHintsAwareParser) {
            IHintsAwareParser p = (IHintsAwareParser) parser;
            p.initialize(parseHints);
        }
    }

    //TODO Should this be a generic utility method?
    //TODO Validate languagues in config matches those installed.
    //TODO Print out Tesseract version and path on startup?
    private void validateOCRInstall() {
        OCRConfig ocrConfig = parseHints.getOcrConfig();
        if (StringUtils.isBlank(ocrConfig.getPath())) {
            LOG.debug("OCR parsing is disabled (no path provided).");
            return;
        }
        String exePath = ocrConfig.getPath();
        File exeFile = new File(exePath,
                (System.getProperty("os.name").startsWith("Windows")
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


    @Override
    public void loadFromXML(Reader in) throws IOException {
        XML xml = new XML(in);
        setIgnoredContentTypesRegex(xml.getString(
                "ignoredContentTypes", getIgnoredContentTypesRegex()));

        // Parse hints
        loadParseHintsFromXML(xml);

        // Fallback parser
        fallbackParser = xml.getObject("fallbackParser", fallbackParser);

        // Parsers
        List<XML> nodes = xml.getXMLList("parsers/parser");
        for (XML node : nodes) {
            IDocumentParser parser = node.getObject(".");
            String contentType = node.getString("@contentType");
            if (StringUtils.isBlank(contentType)) {
                throw new ConfigurationException(
                        "Attribute \"contentType\" missing for parser: "
                      + node.getString("@class"));
            }
            parsers.put(ContentType.valueOf(contentType), parser);
        }
    }

    private void loadParseHintsFromXML(XML xml) {
        // Embedded Config
        XML embXml = xml.getXML("embedded");
        EmbeddedConfig embCfg = parseHints.getEmbeddedConfig();
        embCfg.setSplitContentTypes(
                embXml.getString("splitContentTypes", null));
        embCfg.setNoExtractContainerContentTypes(
                embXml.getString("noExtractContainerContentTypes", null));
        embCfg.setNoExtractEmbeddedContentTypes(
                embXml.getString("noExtractEmbeddedContentTypes", null));

        // OCR Config
        XML ocrXml = xml.getXML("ocr");
        OCRConfig ocrCfg = parseHints.getOcrConfig();
        ocrCfg.setPath(ocrXml.getString("@path"));
        ocrCfg.setLanguages(ocrXml.getString("languages"));
        ocrCfg.setContentTypes(ocrXml.getString("contentTypes"));
    }


    @Override
    public void saveToXML(Writer out, String tagName) throws IOException {
        EnhancedXMLStreamWriter xml = new EnhancedXMLStreamWriter(out);
        xml.writeStartElement(tagName, getClass());

        if (ignoredContentTypesRegex != null) {
            xml.writeStartElement("ignoredContentTypes");
            xml.writeCharacters(ignoredContentTypesRegex);
            xml.writeEndElement();
        }

        saveParseHintsToXML(xml);

        xml.writeObject("fallbackParser", fallbackParser);

        if (!parsers.isEmpty()) {
            xml.writeStartElement("parsers");
            for (Entry<ContentType, IDocumentParser> entry:
                    parsers.entrySet()) {
                ContentType ct = entry.getKey();
                IDocumentParser parser = entry.getValue();
                //TODO simplify this
                if (parser instanceof IXMLConfigurable) {
                    // Writing a comment here is a necessary workaround to
                    // close <parsers> tag.
                    xml.writeComment(" ");
                    xml.flush();
                    StringWriter sout = new StringWriter();
                    ((IXMLConfigurable) parser).saveToXML(sout, "parser");
                    String parserXML = sout.toString();
                    if (!parserXML.matches("\\S+contentType\\s*=")) {
                        parserXML = parserXML.replaceFirst("^<parser",
                                "<parser contentType=\""
                                        + ct.toString() + "\"");
                    }
                    out.write(parserXML);
                    out.flush();
                } else {
                    xml.writeStartElement("parser");
                    xml.writeAttributeString("class",
                            parser.getClass().getCanonicalName());
                    xml.writeAttributeString("contentType", ct.toString());
                    xml.writeEndElement();
                }

            }
            xml.writeEndElement();
        }

        xml.writeEndElement();
        xml.flush();
        xml.close();
    }

    private void saveParseHintsToXML(EnhancedXMLStreamWriter xml) {
        EmbeddedConfig emb = parseHints.getEmbeddedConfig();
        if (!emb.isEmpty()) {
            xml.writeStartElement("embedded");
            xml.writeElementString("splitContentTypes",
                    emb.getSplitContentTypes());
            xml.writeElementString("noExtractEmbeddedContentTypes",
                    emb.getNoExtractEmbeddedContentTypes());
            xml.writeElementString("noExtractContainerContentTypes",
                    emb.getNoExtractContainerContentTypes());
            xml.writeEndElement();
        }
        OCRConfig ocr = parseHints.getOcrConfig();
        if (!ocr.isEmpty()) {
            xml.writeStartElement("ocr");
            xml.writeAttributeString("path", ocr.getPath());
            xml.writeElementString("languages", ocr.getLanguages());
            xml.writeElementString(
                    "contentTypes", ocr.getContentTypes());
            xml.writeEndElement();
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof GenericDocumentParserFactory)) {
            return false;
        }
        GenericDocumentParserFactory castOther =
                (GenericDocumentParserFactory) other;

        if (!new EqualsBuilder()
                .append(ignoredContentTypesRegex,
                        castOther.ignoredContentTypesRegex)
                .append(parseHints, castOther.parseHints)
                .append(parsersAreUpToDate, castOther.parsersAreUpToDate)
                .append(parsers.size(), castOther.parsers.size())
                .append(fallbackParser, castOther.fallbackParser)
                .isEquals()) {
            return false;
        }

        for (Entry<ContentType, IDocumentParser> entry : parsers.entrySet()) {
            IDocumentParser otherParser = castOther.parsers.get(entry.getKey());
            if (otherParser == null) {
                return false;
            }
            IDocumentParser parser = entry.getValue();
            if (!Objects.equals(parser,  otherParser)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = new HashCodeBuilder()
                .append(ignoredContentTypesRegex)
                .append(parseHints)
                .append(parsersAreUpToDate)
                .append(parsers.size())
                .toHashCode();
        hash += fallbackParser.hashCode();
        for (Entry<ContentType, IDocumentParser> entry : parsers.entrySet()) {
            ContentType ct = entry.getKey();
            hash += ct.hashCode();
            IDocumentParser parser = entry.getValue();
            if (parser == null) {
                continue;
            }
            hash += parser.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("namedParsers", parsers)
                .append("fallbackParser", fallbackParser)
                .append("ignoredContentTypesRegex", ignoredContentTypesRegex)
                .append("parseHints", parseHints)
                .append("parsersAreUpToDate", parsersAreUpToDate)
                .toString();
    }
}
