/* Copyright 2014-2016 Norconex Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
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
 *          caseSensitive="[false|true]"
 *          sourceCharset="(character encoding)"         
 *          maxReadSize="(max characters to read at once)" &gt;
 *          
 *      &lt;reduce&gt;(character or string to strip)&lt;/reduce&gt;
 *      &lt;!-- multiple reduce tags allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
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
 * @see Pattern
 */
public class ReduceConsecutivesTransformer extends AbstractStringTransformer {

    private boolean caseSensitive;
    private final List<String> reductions = new ArrayList<String>();

    @Override
    protected void transformStringContent(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) {

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
    protected void loadStringTransformerFromXML(XMLConfiguration xml)
            throws IOException {
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
    }

    @Override
    protected void saveStringTransformerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
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
    public boolean equals(final Object other) {
        if (!(other instanceof ReduceConsecutivesTransformer)) {
            return false;
        }
        ReduceConsecutivesTransformer castOther = 
                (ReduceConsecutivesTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(caseSensitive, castOther.caseSensitive)
                .append(reductions, castOther.reductions)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("caseSensitive", caseSensitive)
                .append("reductions", reductions)
                .toString();
    }
}
