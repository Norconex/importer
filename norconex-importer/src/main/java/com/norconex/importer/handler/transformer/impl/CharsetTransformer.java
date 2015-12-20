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
package com.norconex.importer.handler.transformer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.AbstractDocumentTransformer;

/**
 * <p>
 * Transforms a document content (if needed) from a source character
 * encoding (charset) to a target one. Both the source and target character
 * encodings are optional. If no source character encoding is explicitly
 * provided, it first tries to detect the encoding of the document
 * content before converting it to the target encoding. If the source 
 * character encoding cannot be established, the content encoding will remain
 * unchanged. When no target character encoding is specified, UTF-8 is assumed.
 * </p>
 * 
 * <h3>Should I use this transformer?</h3>
 * <p>
 * Before using this transformer, you need to know the parsing of documents
 * by the importer using default document parser factory will try to convert
 * and return content as UTF-8 (for most, if not all content-types).
 * If UTF-8 is your desired target, it only make sense to use this transformer
 * as a pre-parsing handler (for text content-types only) when it is important
 * to work with a specific character encoding before parsing. 
 * If on the other hand you wish to convert to a character encoding to a 
 * target different than UTF-8, you can use this transformer as a post-parsing
 * handler to do so.
 * </p>
 * 
 * <h3>Conversion is not flawless</h3>
 * <p>
 * Because character encoding detection is not always accurate and because
 * documents sometime mix different encoding, there is no guarantee this
 * class will handle ALL character encoding conversions properly.
 * </p>
 * <h3>
 * XML configuration usage:
 * </h3>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.CharsetTransformer"
 *      sourceCharset="(character encoding)" targetCharset="(character encoding)"&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.5.0
 */
public class CharsetTransformer extends AbstractDocumentTransformer
        implements IXMLConfigurable {

    private static final Logger LOG = 
            LogManager.getLogger(CharsetTransformer.class);    

    public static final String DEFAULT_TARGET_CHARSET = CharEncoding.UTF_8;
    
    private String targetCharset = DEFAULT_TARGET_CHARSET;
    private String sourceCharset = null;

    @Override
    protected void transformApplicableDocument(String reference,
            InputStream input, OutputStream output, ImporterMetadata metadata,
            boolean parsed) throws ImporterHandlerException {
        
        //--- Get source charset ---
        String source = sourceCharset;
        if (StringUtils.isBlank(source)) {
            source = detectCharset(input, metadata.getString(
                ImporterMetadata.DOC_CONTENT_ENCODING));
        }
        // Do not attempt conversion of no source charset is found
        if (StringUtils.isBlank(source)) {
            return;
        }

        //--- Get target charset ---
        String target = targetCharset;
        if (StringUtils.isBlank(target)) {
            target = CharEncoding.UTF_8;
        }
        
        //--- Convert ---
        try {
            CharsetDecoder decoder = Charset.forName(source).newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            CharsetEncoder encoder = Charset.forName(target).newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPLACE);
            encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            Reader reader = new InputStreamReader(input, decoder);
            Writer writer = new OutputStreamWriter(output, encoder);
            IOUtils.copyLarge(reader, writer);
            writer.flush();
            rewind(input);
        } catch (IOException e) {
            LOG.warn("Cannot convert character encoding from " + source
                    + " to " + target + ". Encoding will remain unchanged.", e);
             return;
        }
    }
    
    private String detectCharset(InputStream input, String declaredEncoding) {
        CharsetDetector cd = new CharsetDetector();
        if (StringUtils.isNotBlank(declaredEncoding)) {
            cd.setDeclaredEncoding(declaredEncoding);
        }
        String sourceCharset = null;
        try {
            cd.enableInputFilter(true);
            cd.setText(input);
            rewind(input);
            CharsetMatch match = cd.detect();
            sourceCharset = match.getName();
        } catch (IOException e) {
            LOG.warn("Cannot detect source encoding. "
                   + "Encoding will remain unchanged.", e);
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Detected encoding: " + sourceCharset);
        }
        return sourceCharset;
    }
    
    private void rewind(InputStream is) {
        //TODO investigate why regular reset on CachedInputStream has
        //no effect and returns an empty stream when read again. Fix that 
        //instead of having this method.
        if (is instanceof CachedInputStream) {
            ((CachedInputStream) is).rewind();;
        }
    }
    
    public String getTargetCharset() {
        return targetCharset;
    }
    public void setTargetCharset(String targetCharset) {
        this.targetCharset = targetCharset;
    }

    public String getSourceCharset() {
        return sourceCharset;
    }
    public void setSourceCharset(String sourceCharset) {
        this.sourceCharset = sourceCharset;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setSourceCharset(xml.getString("[@sourceCharset]", getSourceCharset()));
        setTargetCharset(xml.getString("[@targetCharset]", getTargetCharset()));
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("sourceCharset", getSourceCharset());
        writer.writeAttributeString("targetCharset", getTargetCharset());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(sourceCharset)
            .append(targetCharset)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CharsetTransformer)) {
            return false;
        }
        CharsetTransformer castOther = (CharsetTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(sourceCharset, castOther.sourceCharset)
                .append(targetCharset, castOther.targetCharset)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("sourceCharset", sourceCharset)
                .append("targetCharset", targetCharset)
                .toString();
    }
}
