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
import java.io.OutputStream;
import java.io.Reader;

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
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.AbstractDocumentTransformer;

/**
 * <p>
 * Transforms a document content (if needed) to a specific character encoding
 * (charset). It first tries to detect the charset of the document content
 * before converting it to target charset.  If the source charset cannot be
 * established, the content will remain unchanged. When no target charset is
 * specified, UTF-8 is assumed.
 * </p>
 * 
 * <h3>Should I use this transformer?</h3>
 * <p>
 * Before using this transformer, you need to know the parsing of documents
 * by the importer (using default document parser factory) will try to convert
 * and return content as UTF-8.
 * If UTF-8 is your desired target, it only make sense to use this transformer
 * as a pre-parsing (text content-types only) handler when it is important
 * to work with a specific charset before parsing. If on the other hand
 * you wish to convert to a character encoding to a target different than
 * UTF-8, you can use this transformer as a post-parsing handler to do so.
 * </p>
 * 
 * <h3>No guarantee</h3>
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
 *      targetCharset="(character encoding)"&gt;
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

    @Override
    protected void transformApplicableDocument(String reference,
            InputStream input, OutputStream output, ImporterMetadata metadata,
            boolean parsed) throws ImporterHandlerException {
        
        //--- Get source charset ---
        CharsetDetector cd = new CharsetDetector();
        String declaredEncoding = metadata.getString(
                ImporterMetadata.DOC_CONTENT_ENCODING);
        if (StringUtils.isNotBlank(declaredEncoding)) {
            cd.setDeclaredEncoding(declaredEncoding);
        }
        String sourceCharset = null;
        Reader sourceReader = null;
        try {
            cd.enableInputFilter(true);
            cd.setText(input);
            CharsetMatch match = cd.detect();
            sourceCharset = match.getName();
            sourceReader = match.getReader();
        } catch (IOException e) {
            LOG.warn("Cannot detect source encoding. "
                   + "Encoding will remain unchanged.", e);
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Detected encoding: " + sourceCharset);
        }

        //--- Get target charset ---
        String safeTargetCharset = targetCharset;
        if (StringUtils.isBlank(targetCharset)) {
            safeTargetCharset = CharEncoding.UTF_8;
        }
        
        //--- Convert ---
        try {
            IOUtils.copy(sourceReader, output, safeTargetCharset);
        } catch (IOException e) {
            LOG.warn("Cannot convert character encoding from " + sourceCharset
                    + " to " + safeTargetCharset
                    + ". Encoding will remain unchanged.", e);
             return;
        }
    }

    public String getTargetCharset() {
        return targetCharset;
    }
    public void setTargetCharset(String targetCharset) {
        this.targetCharset = targetCharset;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setTargetCharset(xml.getString("[@targetCharset]", getTargetCharset()));
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("targetCharset", getTargetCharset());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
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
                .append(targetCharset, castOther.targetCharset)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("targetCharset", targetCharset)
                .toString();
    }
}
