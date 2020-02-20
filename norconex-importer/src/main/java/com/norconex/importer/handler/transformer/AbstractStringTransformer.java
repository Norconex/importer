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
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.io.TextReader;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class to facilitate creating transformers on text content, loading
 * text into a {@link StringBuilder} for memory processing.
 * </p>
 *
 * <p><b>Since 2.2.0</b> this class limits the memory used for content
 * transformation by reading one section of text at a time.  Each
 * sections are sent for transformation once they are read,
 * so that no two sections exists in memory at once.  Sub-classes should
 * respect this approach.  Each of them have a maximum number of characters
 * equal to the maximum read size defined using {@link #setMaxReadSize(int)}.
 * When none is set, the default read size is defined by
 * {@link TextReader#DEFAULT_MAX_READ_SIZE}.
 * </p>
 *
 * <p>An attempt is made to break sections nicely after a paragraph, sentence,
 * or word.  When not possible, long text will be cut at a size equal
 * to the maximum read size.
 * </p>
 *
 * <p>
 * Implementors should be conscious about memory when dealing with the string
 * builder.
 * </p>
 *
 * {@nx.xml.usage #attributes
 *   maxReadSize="(max characters to read at once)"
 *   {@nx.include com.norconex.importer.handler.transformer.AbstractCharStreamTransformer#attributes}
 * }
 *
 * <p>
 * Subclasses inherit the above {@link IXMLConfigurable} attribute(s),
 * in addition to <a href="../AbstractImporterHandler.html#nx-xml-restrictTo">
 * &lt;restrictTo&gt;</a>.
 * </p>
 *
 * <p>
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * </p>
 * <pre>
 *  &lt;!-- parent tag has these attribute:
 *      maxReadSize="(max characters to read at once)"
 *      sourceCharset="(character encoding)"
 *    --&gt;
 *  &lt;restrictTo caseSensitive="[false|true]"
 *          field="(name of header/metadata field name to match)" &gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public abstract class AbstractStringTransformer
            extends AbstractCharStreamTransformer {

    private int maxReadSize = TextReader.DEFAULT_MAX_READ_SIZE;

    @Override
    protected final void transformTextDocument(
            final String reference, final Reader input,
            final Writer output, final Properties metadata, final boolean parsed)
                    throws ImporterHandlerException {

        int sectionIndex = 0;
        StringBuilder b = new StringBuilder();
        String text = null;
        try (TextReader reader = new TextReader(input, maxReadSize)) {
            while ((text = reader.readText()) != null) {
                b.append(text);
                transformStringContent(
                        reference, b, metadata, parsed, sectionIndex);
                output.append(b);
                sectionIndex++;
                b.setLength(0);
            }
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot transform text document.", e);
        }
        b.setLength(0);
        b = null;
    }

    /**
     * Gets the maximum number of characters to read and transform
     * at once. Default is {@link TextReader#DEFAULT_MAX_READ_SIZE}.
     * @return maximum read size
     */
    public int getMaxReadSize() {
        return maxReadSize;
    }
    /**
     * Sets the maximum number of characters to read and transform
     * at once.
     * @param maxReadSize maximum read size
     */
    public void setMaxReadSize(final int maxReadSize) {
        this.maxReadSize = maxReadSize;
    }

    protected abstract void transformStringContent(
           String reference, StringBuilder content, Properties metadata,
           boolean parsed, int sectionIndex) throws ImporterHandlerException;

    @Override
    protected final void saveCharStreamTransformerToXML(final XML xml) {
        xml.setAttribute("maxReadSize", maxReadSize);
        saveStringTransformerToXML(xml);
    }
    /**
     * Saves configuration settings specific to the implementing class.
     * The parent tag along with the "class" attribute are already written.
     * Implementors must not close the writer.
     *
     * @param xml the XML
     */
    protected abstract void saveStringTransformerToXML(XML xml);

    @Override
    protected final void loadCharStreamTransformerFromXML(final XML xml) {
        setMaxReadSize(xml.getInteger("@maxReadSize", maxReadSize));
        loadStringTransformerFromXML(xml);
    }
    /**
     * Loads configuration settings specific to the implementing class.
     * @param xml XML configuration
     */
    protected abstract void loadStringTransformerFromXML(XML xml);

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