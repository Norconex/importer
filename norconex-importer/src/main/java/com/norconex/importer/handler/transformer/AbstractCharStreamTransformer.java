/* Copyright 2010-2020 Norconex Inc.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

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
 * {@nx.xml.usage #attributes
 *  sourceCharset="(character encoding)"
 * }
 * <p>
 * Subclasses inherit the above {@link IXMLConfigurable} attribute(s),
 * in addition to <a href="../AbstractImporterHandler.html#nx-xml-restrictTo">
 * &lt;restrictTo&gt;</a>.
 * </p>
 * @author Pascal Essiembre
 */
public abstract class AbstractCharStreamTransformer
            extends AbstractDocumentTransformer {

    private static final Logger LOG =
            LoggerFactory.getLogger(AbstractCharStreamTransformer.class);

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
    public void setSourceCharset(final String sourceCharset) {
        this.sourceCharset = sourceCharset;
    }

    @Override
    protected final void transformApplicableDocument(
            HandlerDoc doc, InputStream input,
            OutputStream output, ParseState parseState)
                    throws ImporterHandlerException {

        String inputCharset = detectCharsetIfBlank(
                doc, input, sourceCharset, parseState);
        if (StringUtils.isBlank(inputCharset)) {
            LOG.warn("Character encoding could not be detected (will assume "
                    + "UTF-8). If this leads to a failure, it could be that "
                    + "you are using this transformer ({}) with binary "
                    + "content. You can avoid this by applying "
                    + "restrictions or making sure it was parsed first. "
                    + "Reference: {}",
                    getClass().getCanonicalName(), doc.getReference());
            inputCharset = StandardCharsets.UTF_8.toString();
        }
        try {
            InputStreamReader reader =
                    new InputStreamReader(input, inputCharset);
            OutputStreamWriter writer =
                    new OutputStreamWriter(output, inputCharset);
            transformTextDocument(doc, reader, writer, parseState);
            writer.flush();
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot transform character stream.", e);
        }
    }

    protected abstract void transformTextDocument(
            HandlerDoc doc, Reader input,
            Writer output, ParseState parseState)
                    throws ImporterHandlerException;


    @Override
    protected final void saveHandlerToXML(final XML xml) {
        xml.setAttribute("sourceCharset", sourceCharset);
        saveCharStreamTransformerToXML(xml);
    }
    /**
     * Saves configuration settings specific to the implementing class.
     * The parent tag along with the "class" attribute are already written.
     * Implementors must not close the writer.
     *
     * @param xml the XML
     */
    protected abstract void saveCharStreamTransformerToXML(XML xml);

    @Override
    protected final void loadHandlerFromXML(final XML xml) {
        setSourceCharset(xml.getString("@sourceCharset", sourceCharset));
        loadCharStreamTransformerFromXML(xml);
    }
    /**
     * Loads configuration settings specific to the implementing class.
     * @param xml XML configuration
     */
    protected abstract void loadCharStreamTransformerFromXML(XML xml);

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}