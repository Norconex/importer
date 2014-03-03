/* Copyright 2010-2013 Norconex Inc.
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
package com.norconex.importer.transformer.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.transformer.AbstractStringTransformer;

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
 *  &lt;transformer class="com.norconex.importer.transformer.impl.ReplaceTransformer"
 *          caseSensitive="[false|true]" &gt;
 *      &lt;contentTypeRegex&gt;
 *          (regex to identify text content-types for pre-import, 
 *           overriding default)
 *      &lt;/contentTypeRegex&gt;
 *      &lt;restrictTo
 *              caseSensitive="[false|true]" &gt;
 *              property="(name of header/metadata name to match)"
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;replace&gt
 *          &lt;fromValue&gt(regex of value to replace)&lt;/fromValue&gt
 *          &lt;toValue&gt(replacement value)&lt;/toValue&gt
 *      &lt;/replace&gt
 *      &lt;!-- multiple replace tags allowed --&gt;
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
            StringBuilder content, Properties metadata, boolean parsed,
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
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));

        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("replace");
        for (HierarchicalConfiguration node : nodes) {
            replacements.put(
                    node.getString("fromValue"), node.getString("toValue"));
        }
        super.loadFromXML(xml);
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("transformer");
            writer.writeAttribute("class", getClass().getCanonicalName());
            writer.writeAttribute(
                    "caseSensitive", Boolean.toString(isCaseSensitive()));
            super.saveToXML(writer);

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
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
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
