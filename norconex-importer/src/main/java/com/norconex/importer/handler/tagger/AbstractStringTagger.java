/* Copyright 2010-2016 Norconex Inc.
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
package com.norconex.importer.handler.tagger;

import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.io.TextReader;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class to facilitate creating taggers based on text content, loading
 * text into {@link StringBuilder} for memory processing.
 * 
 * <p><b>Since 2.2.0</b> this class limits the memory used for analysing 
 * content by reading one section of text at a time.  Each
 * sections are sent for tagging once they are read,
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
 * <p>
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * </p>
 * <pre>
 *  &lt;!-- parent tag has this attribute: 
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
public abstract class AbstractStringTagger 
            extends AbstractCharStreamTagger {

    private int maxReadSize = TextReader.DEFAULT_MAX_READ_SIZE;

    @Override
    protected final void tagTextDocument(
            String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        int sectionIndex = 0;
        StringBuilder b = new StringBuilder();
        String text = null;
        boolean atLeastOnce = false;
        try (TextReader reader = new TextReader(input, maxReadSize)) {
            while ((text = reader.readText()) != null) {
                b.append(text);
                tagStringContent(reference, b, metadata, parsed, sectionIndex);
                sectionIndex++;
                b.setLength(0);
                atLeastOnce = true;
            }
            // If no content, go at least once in it in case the tagger
            // supports has metadata-related operations that should work
            // even if no content exists.
            if (!atLeastOnce) {
                tagStringContent(reference, b, metadata, parsed, 0);
            }
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot tag text document.", e);
        }
        b.setLength(0);
        b = null;
    }
    
    /**
     * Gets the maximum number of characters to read from content for tagging
     * at once. Default is {@link TextReader#DEFAULT_MAX_READ_SIZE}.
     * @return maximum read size
     */
    public int getMaxReadSize() {
        return maxReadSize;
    }
    /**
     * Sets the maximum number of characters to read from content for tagging
     * at once.
     * @param maxReadSize maximum read size
     */
    public void setMaxReadSize(int maxReadSize) {
        this.maxReadSize = maxReadSize;
    }
    
    protected abstract void tagStringContent(
           String reference, StringBuilder content, ImporterMetadata metadata,
           boolean parsed, int sectionIndex) 
                   throws ImporterHandlerException;
    
    @Override
    protected final void saveCharStreamTaggerToXML(
            EnhancedXMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttributeInteger("maxReadSize", getMaxReadSize());
        saveStringTaggerToXML(writer);
    }
    /**
     * Saves configuration settings specific to the implementing class.
     * The parent tag along with the "class" attribute are already written.
     * Implementors must not close the writer.
     * 
     * @param writer the xml writer
     * @throws XMLStreamException could not save to XML
     */
    protected abstract void saveStringTaggerToXML(
            EnhancedXMLStreamWriter writer) throws XMLStreamException;
    
    @Override
    protected final void loadCharStreamTaggerFromXML(XMLConfiguration xml)
            throws IOException {
        setMaxReadSize(xml.getInt("[@maxReadSize]", getMaxReadSize()));
        loadStringTaggerFromXML(xml);
    }
    /**
     * Loads configuration settings specific to the implementing class.
     * @param xml xml configuration
     * @throws IOException could not load from XML
     */
    protected abstract void loadStringTaggerFromXML(XMLConfiguration xml)
            throws IOException;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractStringTagger)) {
            return false;
        }
        AbstractStringTagger other = (AbstractStringTagger) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(maxReadSize, other.maxReadSize)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(maxReadSize)
            .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .appendSuper(super.toString())
            .append("maxReadSize", maxReadSize)
            .toString();
    }
    

}