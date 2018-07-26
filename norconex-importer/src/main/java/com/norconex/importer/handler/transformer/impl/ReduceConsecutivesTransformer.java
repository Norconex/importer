/* Copyright 2014-2018 Norconex Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Reduces specified consecutive characters or strings to only one
 * instance (document content only).
 * If reducing duplicate words, you usually have to add a space at the
 * Beginning or end of the word.
 * </p>
 * <p>
 * This class can be used as a pre-parsing (text content-types only)
 * or post-parsing handlers.
 * </p>
 * <p>
 * For more advanced replacement needs, consider using
 * {@link ReplaceTransformer} instead.
 * </p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.ReduceConsecutivesTransformer"
 *          caseSensitive="[false|true]"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;reduce&gt;(character or string to strip)&lt;/reduce&gt;
 *      &lt;!-- multiple reduce tags allowed --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <p>
 * You can specify these special characters in your XML:
 * </p>
 * <ul>
 *   <li>\r (carriage returns)</li>
 *   <li>\n (line feed)</li>
 *   <li>\t (tab)</li>
 *   <li>\s (space)</li>
 * </ul>
 * <h4>Usage example:</h4>
 * <p>
 * The following reduces multiple spaces into a single one.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.ReduceConsecutivesTransformer"&gt;
 *      &lt;reduce&gt;\s&lt;/reduce&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 1.2.0
 */
public class ReduceConsecutivesTransformer extends AbstractStringTransformer {

    private boolean caseSensitive;
    private final List<String> reductions = new ArrayList<>();

    @Override
    protected void transformStringContent(final String reference,
            final StringBuilder content, final ImporterMetadata metadata, final boolean parsed,
            final int sectionIndex) {

        String text = content.toString();
        content.setLength(0);
        Pattern pattern;
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
        return new ArrayList<>(reductions);
    }
    public void setReductions(final String... reductions) {
        this.reductions.clear();
        addReductions(reductions);
    }
    public void addReductions(final String... reductions) {
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
    public void setCaseSensitive(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    private String escapeRegex(final String text) {
        return text.replaceAll(
                "([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\$\\|])", "\\\\$1");
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        setCaseSensitive(xml.getBoolean("@caseSensitive", false));

        List<XML> nodes = xml.getXMLList("reduce");
        for (XML node : nodes) {
            String text = node.getString(".");
            text = text.replaceAll("\\\\s", " ");
            text = text.replaceAll("\\\\t", "\t");
            text = text.replaceAll("\\\\n", "\n");
            text = text.replaceAll("\\\\r", "\r");
            addReductions(text);
        }
    }

    @Override
    protected void saveStringTransformerToXML(final XML xml) {
        xml.setAttribute("caseSensitive", isCaseSensitive());
        for (String reduction : reductions) {
            if (reduction != null) {
                String text = reduction;
                text = text.replaceAll(" ", "\\\\s");
                text = text.replaceAll("\t", "\\\\t");
                text = text.replaceAll("\n", "\\\\n");
                text = text.replaceAll("\r", "\\\\r");
                xml.addElement("reduce", text);
            }
        }
    }

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
