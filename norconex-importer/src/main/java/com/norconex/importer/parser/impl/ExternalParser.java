/* Copyright 2015-2016 Norconex Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.GenericDocumentParserFactory;
import com.norconex.importer.parser.IDocumentParser;

/**
 * <p>
 * Wrapper class around an external program used to extract the text
 * from a file (this class is an extension of 
 * {@link org.apache.tika.parser.external.ExternalParser}).
 * </p>
 * <p>
 * Since 2.6.0, this parser can be made configurable via XML. See
 * {@link GenericDocumentParserFactory} for general indications how 
 * to configure all parsers.  It uses a similar configuration format
 * as Tika <code>tika-external-parsers.xml</code> file.
 * Use the strings <code>${INPUT}</code> and 
 * <code>${OUTPUT}</code> in the 
 * command to identify the input file that will be given by the Importer
 * and the output file that the Importer will read from the program.  For 
 * metadata, ensure you surround your value to be match in parenthesis
 * (regex group).
 * </p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;parser contentType="(content type)" 
 *          class="com.norconex.importer.parser.impl.ExternalParser" &gt;
 *      &lt;command&gt;(your command with arguments)&lt;/command&gt;
 *      &lt;metadata&gt;
 *          &lt;match key="(target field name)"&gt;(regular expression)&lt;/match&gt;
 *          &lt;!-- repeat match tag as needed --&gt;
 *      &lt;/metadata&gt;
 *  &lt;/parser&gt;
 * </pre> 
 * <h4>Usage example:</h4>
 * <pre>
 *  &lt;parser contentType="application/pdf" 
 *          class="com.norconex.importer.parser.impl.ExternalParser" &gt;
 *      &lt;command&gt;java -jar c:\Apps\pdfbox-app-2.0.2.jar ExtractText ${INPUT} ${OUTPUT}&lt;/command&gt;
 *  &lt;/parser&gt;
 * </pre> 
 * @author Pascal Essiembre
 * @since 2.2.0
 */
public class ExternalParser 
        extends org.apache.tika.parser.external.ExternalParser
        implements IDocumentParser, IXMLConfigurable {

    private static final long serialVersionUID = 3569996828422125700L;

    @Override
    public List<ImporterDocument> parseDocument(ImporterDocument doc,
            Writer output) throws DocumentParserException {
        Metadata tikaMetadata = new Metadata();
        if (doc.getContentType() == null) {
            throw new DocumentParserException(
                    "ImporterDocument must have a content-type.");
        }
        String contentType = doc.getContentType().toString();
        tikaMetadata.set(HttpHeaders.CONTENT_TYPE, contentType);
        tikaMetadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, 
                doc.getReference());
        tikaMetadata.set(Metadata.CONTENT_ENCODING, doc.getContentEncoding());

        ContentHandler handler = new BodyContentHandler(output);

        InputStream stream = doc.getContent();
        try {
            parse(stream, handler,  tikaMetadata, new ParseContext());
        } catch (IOException | SAXException | TikaException e) {
            throw new DocumentParserException(e);
        }
        return null;
    }
    
    @Override
    public void setSupportedTypes(Set<MediaType> supportedTypes) {
        throw new UnsupportedOperationException(
                "Cannot set supported types this way.");
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = XMLConfigurationUtil.newXMLConfiguration(in);
        String[] commands = StringUtils.split(
                xml.getString("command", null), " ");
        if (commands != null) {
            setCommand(commands);
        }

        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("metadata.match");
        if (!nodes.isEmpty()) {
            Map<Pattern, String> patterns = new HashMap<>();
            for (HierarchicalConfiguration node : nodes) {
                patterns.put(Pattern.compile(node.getString("")),
                        node.getString("[@key]"));
            }
            setMetadataExtractionPatterns(patterns);
        }
    }
    
    @Override
    public void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            writer.writeStartElement("parser");
            writer.writeAttribute("class", getClass().getCanonicalName());

            writer.writeElementString(
                    "command", StringUtils.join(getCommand(), " "));

            if (getMetadataExtractionPatterns() != null) {
                writer.writeStartElement("metadata");
                for (Entry<Pattern, String> entry 
                        : getMetadataExtractionPatterns().entrySet()) {
                    writer.writeStartElement("match");
                    writer.writeAttribute("key", entry.getValue());
                    writer.writeCharacters(entry.getKey().toString());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            
            writer.writeEndElement();
            writer.flush();
            writer.close();
            
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }
    
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ExternalParser)) {
            return false;
        }
        ExternalParser castOther = (ExternalParser) other;        
        return new EqualsBuilder()
                .append(getCommand(), castOther.getCommand())
                .append(getMetadataExtractionPatterns(), 
                        castOther.getMetadataExtractionPatterns())
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getCommand())
                .append(getMetadataExtractionPatterns())
                .toHashCode();
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("command", getCommand())
                .append("metadataExtractionPatterns", 
                        getMetadataExtractionPatterns())
                .toString();
    }      
    
}
