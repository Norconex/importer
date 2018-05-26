/* Copyright 2010-2017 Norconex Inc.
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
package com.norconex.importer.handler.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class for transformers dealing with text documents only.
 * Subclasses can safely be used as either pre-parse or post-parse handlers
 * restricted to text documents only (see {@link AbstractImporterHandler}).
 * </p>
 * 
 * <p>
 * Sub-classes can restrict to which document to apply this transformation
 * based on document metadata (see {@link AbstractImporterHandler}).
 * </p>
 * 
 * <p><b>Since 2.5.0</b>, when used as a pre-parse handler,
 * this class attempts to detect the content character 
 * encoding unless the character encoding
 * was specified using {@link #setSourceCharset(String)}.  If the character
 * set cannot be established, UTF-8 is assumed.
 * Since document
 * parsing converts content to UTF-8, UTF-8 is always assumed when
 * used as a post-parse handler.
 * </p>
 * 
 * <p>
 * Subclasses implementing {@link IXMLConfigurable} should allow this inner 
 * configuration:
 * </p>
 * <pre>
 *  &lt;!-- parent tag has these attribute: 
 *      sourceCharset="(character encoding)"
 *    --&gt; 
 *  &lt;restrictTo caseSensitive="[false|true]"
 *          field="(name of header/metadata field name to match)"&gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public abstract class AbstractCharStreamTransformer 
            extends AbstractDocumentTransformer {

    private static final Logger LOG = 
            LogManager.getLogger(AbstractCharStreamTransformer.class);    

    private String sourceCharset = null;
    
    /**
     * Gets the assumed source character encoding.
     * @return character encoding of the source to be transformed 
     * @since 2.5.0
     */
    public String getSourceCharset() {
        return sourceCharset;
    }
    /**
     * Sets the assumed source character encoding.
     * @param sourceCharset character encoding of the source to be transformed
     * @since 2.5.0
     */
    public void setSourceCharset(String sourceCharset) {
        this.sourceCharset = sourceCharset;
    }

    @Override
    protected final void transformApplicableDocument(
            String reference, InputStream input,
            OutputStream output, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        String inputCharset = detectCharsetIfBlank(
                sourceCharset, reference, input, metadata, parsed);
        if (StringUtils.isBlank(inputCharset)) {
            LOG.warn("Character encoding could not be detected (will assume "
                    + "UTF-8). If this leads to a failure, it could be that "
                    + "you are using this transformer "
                    + getClass().getCanonicalName()
                    + " with binary content. You can avoid this by applying "
                    + "restrictions or making sure it was parsed first. "
                    + "Reference: " + reference);
            inputCharset = StandardCharsets.UTF_8.toString();
        }
        try {
            InputStreamReader is = new InputStreamReader(input, inputCharset);
            OutputStreamWriter os = 
                    new OutputStreamWriter(output, inputCharset);
            transformTextDocument(reference, is, os, metadata, parsed);
            os.flush();
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot transform character stream.", e);
        }
    }

    protected abstract void transformTextDocument(
            String reference, Reader input,
            Writer output, ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException;
    
    
    @Override
    protected final void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("sourceCharset", getSourceCharset());
        saveCharStreamTransformerToXML(writer);
    }
    /**
     * Saves configuration settings specific to the implementing class.
     * The parent tag along with the "class" attribute are already written.
     * Implementors must not close the writer.
     * 
     * @param writer the xml writer
     * @throws XMLStreamException could not save to XML
     */
    protected abstract void saveCharStreamTransformerToXML(
            EnhancedXMLStreamWriter writer) throws XMLStreamException;

    @Override
    protected final void loadHandlerFromXML(
            XMLConfiguration xml) throws IOException {
        setSourceCharset(xml.getString("[@sourceCharset]", getSourceCharset()));
        loadCharStreamTransformerFromXML(xml);
    }
    /**
     * Loads configuration settings specific to the implementing class.
     * @param xml xml configuration
     * @throws IOException could not load from XML
     */
    protected abstract void loadCharStreamTransformerFromXML(
            XMLConfiguration xml) throws IOException;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractCharStreamTransformer)) {
            return false;
        }
        AbstractCharStreamTransformer other = 
                (AbstractCharStreamTransformer) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(sourceCharset, other.sourceCharset)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(sourceCharset)
            .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .appendSuper(super.toString())
            .append("sourceCharset", sourceCharset)
            .toString();
    }    
}