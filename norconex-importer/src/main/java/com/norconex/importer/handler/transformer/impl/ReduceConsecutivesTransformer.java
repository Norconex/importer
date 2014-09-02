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
package com.norconex.importer.handler.transformer.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Reduces specified consecutive characters or strings to only one
 * instance (document content only).
 * If reducing duplicate words, you usually have to add a space at the 
 * Beginning or end of the word.
 * </p>
 * 
 * <p>This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ReduceConsecutivesTransformer"
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
 *      &lt;reduce&gt;(character or string to strip)&lt;/reduce&gt;
 *      &lt;!-- multiple reduce tags allowed --&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * You can specify these special characters in your XML:
 * <ul>
 *   <li>\r (carriage returns)</li>
 *   <li>\n (line feed)</li>
 *   <li>\t (tab)</li>
 *   <li>\s (space)</li>
 * </ul>
 * @author Pascal Essiembre
 * @since 1.2.0
 */
public class ReduceConsecutivesTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final long serialVersionUID = -5797391183198827565L;

    private boolean caseSensitive;
    private final List<String> reductions = new ArrayList<String>();

    @Override
    protected void transformStringDocument(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            boolean partialContent) {

        String text = content.toString();
        content.setLength(0);
        Pattern pattern = null;
        for (String reduction : reductions) {
            String regex = "(" + escapeRegex(reduction) + ")+";
            if (caseSensitive) {
                pattern = Pattern.compile(regex);
            } else {
                pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            }
            text = pattern.matcher(text).replaceAll("$1");
        }
        content.append(text);
    }
     
    public List<String> getReductions() {
        return new ArrayList<String>(reductions);
    }
    public void setReductions(String... reductions) {
        this.reductions.clear();
        addReductions(reductions);
    }
    public void addReductions(String... reductions) {
        this.reductions.addAll(Arrays.asList(reductions));
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

    private String escapeRegex(String text) {
        return text.replaceAll(
                "([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\$\\|])", "\\\\$1");
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));

        List<HierarchicalConfiguration> nodes =
                xml.configurationsAt("reduce");
        for (HierarchicalConfiguration node : nodes) {
            String text = node.getString("");
            text = text.replaceAll("\\\\s", " ");
            text = text.replaceAll("\\\\t", "\t");
            text = text.replaceAll("\\\\n", "\n");
            text = text.replaceAll("\\\\r", "\r");
            addReductions(text);
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
            for (String reduction : reductions) {
                if (reduction != null) {
                    writer.writeStartElement("reduce");
                    String text = reduction;
                    text = text.replaceAll(" ", "\\\\s");
                    text = text.replaceAll("\t", "\\\\t");
                    text = text.replaceAll("\n", "\\\\n");
                    text = text.replaceAll("\r", "\\\\r");
                    writer.writeCharacters(text);
                    writer.writeEndElement();
                }
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
            .append(reductions)
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
        ReduceConsecutivesTransformer other = (ReduceConsecutivesTransformer) obj;
        if (caseSensitive != other.caseSensitive) {
            return false;
        }
        if (reductions == null) {
            if (other.reductions != null) {
                return false;
            }
        } else if (!reductions.equals(other.reductions)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("caseSensitive", caseSensitive)
                .append("reductions", reductions).toString();
    }
}
