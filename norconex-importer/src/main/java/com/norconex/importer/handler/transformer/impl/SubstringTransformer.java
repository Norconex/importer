/* Copyright 2017 Norconex Inc.
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
import java.io.Reader;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.AbstractCharStreamTransformer;

/**
 * <p>Keep a substring of the content matching a begin and end character 
 * indexes. 
 * Useful when you have to 
 * truncate long content, or when you know precisely where is located
 * the text to extract in some files.
 * </p>
 * <p>
 * The "begin" value is inclusive, while the "end" value
 * is exclusive.  Both are optional.  When not specified (or a negative value),
 * the index
 * is assumed to be the beginning and end of the content, respectively. 
 * </p>
 * <p>
 * This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.</p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.SubstringTransformer"
 *          begin="(number)" end="(number)"&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *      
 *  &lt;/transformer&gt;
 * </pre>
 * 
 * <h4>Usage example:</h4>
 * <p>
 * The following truncates long text to be 10,000 characters maximum.
 * </p> 
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.SubstringTransformer"
 *          end="10000"/&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.7.0
 */
public class SubstringTransformer extends AbstractCharStreamTransformer
        implements IXMLConfigurable {

    private long begin = 0;
    private long end = -1;

    public long getBegin() {
        return begin;
    }
    /**
     * Sets the beginning index (inclusive). 
     * A negative value is treated the same as zero.
     * @param beginIndex beginning index
     */
    public void setBegin(long beginIndex) {
        this.begin = beginIndex;
    }
    public long getEnd() {
        return end;
    }
    /**
     * Sets the end index (exclusive).
     * A negative value is treated as the content end.
     * @param endIndex end index
     */
    public void setEnd(long endIndex) {
        this.end = endIndex;
    }

    @Override
    protected void transformTextDocument(String reference, Reader input,
            Writer output, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        long length = -1;
        if (end > -1) {
            if (end < begin) {
                throw new ImporterHandlerException(
                        "\"end\" cannot be smaller than \"begin\" "
                      + "(begin:" + begin + "; end:" + end);
            }
            length = end - Math.max(begin, 0);
        }
        try {
            IOUtils.copyLarge(input, output, begin, length);
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Could not get a subtring of content for " + reference, e);
        }
    }

    @Override
    protected void saveCharStreamTransformerToXML(
            EnhancedXMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttributeLong("begin", begin);
        writer.writeAttributeLong("end", end);
    }

    @Override
    protected void loadCharStreamTransformerFromXML(XMLConfiguration xml)
            throws IOException {
        setBegin(xml.getLong("[@begin]", getBegin()));
        setEnd(xml.getLong("[@end]", getEnd()));
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(begin)
            .append(end)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof SubstringTransformer)) {
            return false;
        }
        SubstringTransformer castOther = 
                (SubstringTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(begin, castOther.begin)
                .append(end, castOther.end)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("begin", begin)
                .append("end", end).toString();
    }
}
