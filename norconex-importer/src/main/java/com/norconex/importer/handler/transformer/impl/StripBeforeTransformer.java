/* Copyright 2010-2014 Norconex Inc.
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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Strips any content found before first match found for given pattern.</p>
 * 
 * <p>This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.StripBeforeTransformer"
 *          inclusive="[false|true]" 
 *          caseSensitive="[false|true]" &gt;
 *      &lt;stripBeforeRegex&gt(regex)&lt;/stripBeforeRegex&gt
 *      
 *      &lt;restrictTo caseSensitive="[false|true]" &gt;
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class StripBeforeTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final Logger LOG = 
            LogManager.getLogger(StripBeforeTransformer.class);    

    private boolean inclusive;
    private boolean caseSensitive;
    private String stripBeforeRegex;

    @Override
    protected void transformStringContent(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            boolean partialContent) {
        if (stripBeforeRegex == null) {
            LOG.error("No regular expression provided.");
            return;
        }
        int flags = Pattern.DOTALL | Pattern.UNICODE_CASE;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE;
        }
        Pattern pattern = Pattern.compile(stripBeforeRegex, flags);
        Matcher match = pattern.matcher(content);
        if (match.find()) {
            if (inclusive) {
                content.delete(0, match.end());
            } else {
                content.delete(0, match.start());
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

    public String getStripBeforeRegex() {
        return stripBeforeRegex;
    }
    public void setStripBeforeRegex(String regex) {
        this.stripBeforeRegex = regex;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
        setInclusive(xml.getBoolean("[@inclusive]", false));
        setStripBeforeRegex(xml.getString("stripBeforeRegex", null));
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
        writer.writeAttribute("inclusive", Boolean.toString(isInclusive()));
        writer.writeStartElement("stripBeforeRegex");
        writer.writeCharacters(stripBeforeRegex);
        writer.writeEndElement();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .appendSuper(super.toString())
            .append("inclusive", inclusive)
            .append("caseSensitive", caseSensitive)
            .append("stripBeforeRegex", stripBeforeRegex)
            .toString();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(caseSensitive)
            .append(inclusive)
            .append(stripBeforeRegex)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StripBeforeTransformer)) {
            return false;
        }
        StripBeforeTransformer other = (StripBeforeTransformer) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(caseSensitive, other.caseSensitive)
            .append(inclusive, other.inclusive)
            .append(stripBeforeRegex, other.stripBeforeRegex)
            .isEquals();
    }

}
