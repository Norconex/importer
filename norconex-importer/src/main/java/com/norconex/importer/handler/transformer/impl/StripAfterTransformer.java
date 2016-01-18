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
package com.norconex.importer.handler.transformer.impl;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Strips any content found after first match found for given pattern.</p>
 * 
 * <p>This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.StripAfterTransformer"
 *          inclusive="[false|true]" 
 *          caseSensitive="[false|true]"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" &gt;
 *      &lt;stripAfterRegex&gt;(regex)&lt;/stripAfterRegex&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see Pattern
 */
public class StripAfterTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final Logger LOG = 
            LogManager.getLogger(StripAfterTransformer.class);    

    private boolean inclusive;
    private boolean caseSensitive;
    private String stripAfterRegex;

    @Override
    protected void transformStringContent(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) {
        if (stripAfterRegex == null) {
            LOG.error("No regular expression provided.");
            return;
        }
        int flags = Pattern.DOTALL;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        Pattern pattern = Pattern.compile(stripAfterRegex, flags);
        Matcher match = pattern.matcher(content);
        if (match.find()) {
            if (inclusive) {
                content.delete(match.start(), content.length());
            } else {
                content.delete(match.end(), content.length());
            }
        }
    }
        
    public boolean isInclusive() {
        return inclusive;
    }
    /**
     * Sets whether start and end text pairs should themselves be stripped or 
     * not.
     * @param inclusive <code>true</code> to strip start and end text
     */
    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * Sets whether to ignore case when matching start and end text.
     * @param caseSensitive <code>true</code> to consider character case
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getStripAfterRegex() {
        return stripAfterRegex;
    }
    public void setStripAfterRegex(String regex) {
        this.stripAfterRegex = regex;
    }

    @Override
    protected void loadStringTransformerFromXML(XMLConfiguration xml)
            throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
        setInclusive(xml.getBoolean("[@inclusive]", false));
        setStripAfterRegex(xml.getString("stripAfterRegex", null));
    }

    @Override
    protected void saveStringTransformerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
        writer.writeAttribute("inclusive", Boolean.toString(isInclusive()));
        writer.writeStartElement("stripAfterRegex");
        writer.writeCharacters(stripAfterRegex);
        writer.writeEndElement();
    }
    

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(caseSensitive)
            .append(inclusive)
            .append(stripAfterRegex)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof StripAfterTransformer)) {
            return false;
        }
        StripAfterTransformer castOther = (StripAfterTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(inclusive, castOther.inclusive)
                .append(caseSensitive, castOther.caseSensitive)
                .append(stripAfterRegex, castOther.stripAfterRegex)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("inclusive", inclusive)
                .append("caseSensitive", caseSensitive)
                .append("stripAfterRegex", stripAfterRegex).toString();
    }
}
