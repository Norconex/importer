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
 * <h3>XML configuration usage:</h3>
 * (Not required since used by default)
 * <p />
 * <pre>
 *  &lt;documentParserFactory 
 *          class="com.norconex.importer.parser.DefaultDocumentParserFactory"
 *          format="text|xml" &gt;
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
    
    public static final String DEFAULT_FORMAT = "text";
    
    private final Map<ContentType, IDocumentParser> namedParsers = 
            new HashMap<ContentType, IDocumentParser>();
    private IDocumentParser fallbackParser;
    private String format;
    private String ignoredContentTypesRegex;
    
    /**
     * Creates a new document parser factory of "text" format.
     */
    public DefaultDocumentParserFactory() {
        this(DEFAULT_FORMAT);
    }
    /**
     * Creates a new document parser factory of the given format.
     * @param format dependent on parser expectations but typically, one 
     *        of "text" or "xml"
     */
    public DefaultDocumentParserFactory(String format) {
        super();
        this.format = format;
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
    
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
    public String getIgnoredContentTypesRegex() {
        return ignoredContentTypesRegex;
    }
    public void setIgnoredContentTypesRegex(String ignoredContentTypesRegex) {
        this.ignoredContentTypesRegex = ignoredContentTypesRegex;
    }
    protected final void registerNamedParser(
            ContentType contentType, IDocumentParser parser) {
        namedParsers.put(contentType, parser);
    }
    protected final void registerFallbackParser(IDocumentParser parser) {
        this.fallbackParser = parser;
    }
    protected final IDocumentParser getFallbackParser() {
        return fallbackParser;
    }

    private void registerNamedParsers() {
        registerNamedParser(ContentType.HTML, new HTMLParser(format));

        IDocumentParser pdfParser = new PDFParser(format);
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
        registerFallbackParser(new FallbackParser(format));
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationLoader.loadXML(in);
            setFormat(xml.getString("[@format]"));
            setIgnoredContentTypesRegex(xml.getString("ignoredContentTypes"));
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
            if (format != null) {
                writer.writeAttribute("format", format);
            }
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
