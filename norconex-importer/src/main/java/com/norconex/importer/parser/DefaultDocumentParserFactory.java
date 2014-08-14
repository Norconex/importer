/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.importer.ImporterResponse;
import com.norconex.importer.parser.impl.FallbackParser;
import com.norconex.importer.parser.impl.HTMLParser;
import com.norconex.importer.parser.impl.PDFParser;
import com.norconex.importer.parser.impl.wordperfect.WordPerfectParser;

/**
 * Default document parser factory.  It uses Apacke Tika for <i>most</i> of its 
 * supported content types.  For unknown
 * content types, it falls back to Tika generic media detector/parser.
 * <p />
 * <h3>Ignoring content types:</h3>
 * As of version 2.0.0, you can "ignore" content-types so they do not get
 * parsed.  Unparsed documents will be sent as is to the post handlers 
 * and the calling application.   Use caution when using that feature since
 * many post-parsing handlers or applications expect text-only content for 
 * them to execute properly.  Unless you really know what you are doing, <b> 
 * avoid excluding binary content types from parsing.</b>
 * <p />
 * <h3>Embedded documents:</h3>
 * For documents containing embedded documents (e.g. zip files), the default 
 * behavior of this treat them as a single document, merging all
 * embedded documents content and metadata into the parent document.
 * As of version 2.0.0, you can tell this parser to "split" embedded
 * documents to have them treated as if they were individual documents.  When
 * split, each embedded documents will go through the entire import cycle, 
 * going through your handlers and even this parser again
 * (just like any regular document would).  The resulting 
 * {@link ImporterResponse} should then contain nested documents, which in turn,
 * might contain some (tree-like structure). 
 * <p />
 * <h3>XML configuration usage:</h3>
 * (Not required since used by default)
 * <p />
 * <pre>
 *  &lt;documentParserFactory 
 *          class="com.norconex.importer.parser.DefaultDocumentParserFactory" 
 *          splitEmbedded="(false|true)" &gt;
 *      &lt;ignoredContentTypes&gt;
 *          (optional regex matching content types to ignore for parsing, 
 *           i.e., not parsed.)
 *      &lt;/ignoredContentTypes&gt;
 *  &lt;/documentParserFactory&gt;
 * </pre>
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class DefaultDocumentParserFactory 
        implements IDocumentParserFactory, 
                   IXMLConfigurable {

    private static final long serialVersionUID = 6639928288252330105L;
    
    private final Map<ContentType, IDocumentParser> namedParsers = 
            new HashMap<ContentType, IDocumentParser>();
    private IDocumentParser fallbackParser;

    private String ignoredContentTypesRegex;
    private boolean splitEmbedded;
    private boolean parsersAllSet = false;
    
    /**
     * Creates a new document parser factory of the given format.
     */
    public DefaultDocumentParserFactory() {
        super();
        registerNamedParsers();
        registerFallbackParser();
    }

    /**
     * Gets a parser based on content type, regardless of document reference
     * (ignoring it).
     */
    @Override
    public final IDocumentParser getParser(
            String documentReference, ContentType contentType) {
        ensureParsersAllSet();
        // If ignoring content-type, do not even return a parser
        if (contentType != null 
                && StringUtils.isNotBlank(ignoredContentTypesRegex)
                && contentType.toString().matches(ignoredContentTypesRegex)) {
            return null;
        }
        
        IDocumentParser parser = namedParsers.get(contentType);
        if (parser == null) {
            return fallbackParser;
        }
        return parser;
    }
    
    public String getIgnoredContentTypesRegex() {
        return ignoredContentTypesRegex;
    }
    public void setIgnoredContentTypesRegex(String ignoredContentTypesRegex) {
        this.ignoredContentTypesRegex = ignoredContentTypesRegex;
    }

    public boolean isSplitEmbedded() {
        return splitEmbedded;
    }
    public void setSplitEmbedded(boolean splitEmbedded) {
        this.splitEmbedded = splitEmbedded;
    }

    protected final void registerNamedParser(
            ContentType contentType, IDocumentParser parser) {
        parsersAllSet = false;
        namedParsers.put(contentType, parser);
    }
    protected final void registerFallbackParser(IDocumentParser parser) {
        parsersAllSet = false;
        this.fallbackParser = parser;
    }
    protected final IDocumentParser getFallbackParser() {
        ensureParsersAllSet();
        return fallbackParser;
    }

    private void registerNamedParsers() {
        registerNamedParser(ContentType.HTML, new HTMLParser());

        IDocumentParser pdfParser = new PDFParser();
        registerNamedParser(ContentType.PDF, pdfParser);
        registerNamedParser(
                ContentType.valueOf("application/x-pdf"), pdfParser);

        IDocumentParser wpParser = new WordPerfectParser();
        registerNamedParser(
                ContentType.valueOf("application/wordperfecet"), wpParser);
        registerNamedParser(
                ContentType.valueOf("application/wordperfect6.0"), wpParser);
        registerNamedParser(
                ContentType.valueOf("application/wordperfect6.1"), wpParser);
    }
    private void registerFallbackParser() {
        registerFallbackParser(new FallbackParser());
    }
    
    private void ensureParsersAllSet() {
        if (!parsersAllSet) {
            for (IDocumentParser parser : namedParsers.values()) {
                if (parser instanceof IDocumentSplittableEmbeddedParser) {
                    ((IDocumentSplittableEmbeddedParser) parser)
                            .setSplitEmbedded(splitEmbedded);
                }
            }
            if (fallbackParser != null && fallbackParser 
                    instanceof IDocumentSplittableEmbeddedParser) {
                ((IDocumentSplittableEmbeddedParser) fallbackParser)
                        .setSplitEmbedded(splitEmbedded);
            }
            parsersAllSet = true;
        }
    }
    
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationLoader.loadXML(in);
            setIgnoredContentTypesRegex(xml.getString(
                    "ignoredContentTypes", getIgnoredContentTypesRegex()));
            setSplitEmbedded(xml.getBoolean(
                    "[@splitEmbedded]", isSplitEmbedded()));
        } catch (ConfigurationException e) {
            throw new IOException("Cannot load XML.", e);
        }
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("tagger");
            writer.writeAttribute("class", getClass().getCanonicalName());
            writer.writeAttribute("splitEmbedded", 
                    Boolean.toString(isSplitEmbedded()));
            if (ignoredContentTypesRegex != null) {
                writer.writeStartElement("ignoredContentTypes");
                writer.writeCharacters(ignoredContentTypesRegex);
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

}
