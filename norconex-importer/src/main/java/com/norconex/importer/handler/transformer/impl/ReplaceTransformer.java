/* Copyright 2010-2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.transformer.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * Replaces every occurrences of the given replacements
 * (document content only).
 * <p/>
 * 
 * This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.
 * <p/>
 * XML configuration usage:
 * <p/>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ReplaceTransformer"
 *          caseSensitive="[false|true]" &gt;
 *      &lt;replace&gt
 *          &lt;fromValue&gt(regex of value to replace)&lt;/fromValue&gt
 *          &lt;toValue&gt(replacement value)&lt;/toValue&gt
 *      &lt;/replace&gt
 *      &lt;!-- multiple replace tags allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]" &gt;
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 1.2.0
 */
public class ReplaceTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final long serialVersionUID = -8066239974517286505L;

    private boolean caseSensitive;
    private final Map<String, String> replacements = 
            new ListOrderedMap<String, String>();

    @Override
    protected void transformStringDocument(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            boolean partialContent) {

        String text = content.toString();
        content.setLength(0);
        Pattern pattern = null;
        
        for (String from : replacements.keySet()) {
            String to = replacements.get(from);
            if (caseSensitive) {
                pattern = Pattern.compile(from);
            } else {
                pattern = Pattern.compile(from, Pattern.CASE_INSENSITIVE);
            }
            text = pattern.matcher(text).replaceAll(to);
        }
        content.append(text);
    }
     
    public Map<String, String> getReplacements() {
        return ListOrderedMap.listOrderedMap(replacements);
    }
    public void addReplacement(String from, String to) {
        this.replacements.put(from, to);
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * Sets whether to ignore case when matching characters or string
     * to reduce.
     * @param caseSensitive <code>true</code> to consider character case
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));

        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("replace");
        for (HierarchicalConfiguration node : nodes) {
            replacements.put(
                    node.getString("fromValue"), node.getString("toValue"));
        }
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
        for (String from : replacements.keySet()) {
            String to = replacements.get(from);
            writer.writeStartElement("replace");
            writer.writeStartElement("fromValue");
            writer.writeCharacters(from);
            writer.writeEndElement();
            writer.writeStartElement("toValue");
            writer.writeCharacters(to);
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(caseSensitive)
            .append(replacements)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ReplaceTransformer other = (ReplaceTransformer) obj;
        if (caseSensitive != other.caseSensitive) {
            return false;
        }
        if (replacements == null) {
            if (other.replacements != null) {
                return false;
            }
        } else if (!replacements.equals(other.replacements)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("caseSensitive", caseSensitive)
                .append("replacements", replacements).toString();
    }
}
