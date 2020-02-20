/* Copyright 2014-2020 Norconex Inc.
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

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
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
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.transformer.impl.ReduceConsecutivesTransformer"
 *     ignoreCase="[false|true]"
 *     {@nx.include com.norconex.importer.handler.transformer.AbstractStringTransformer#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <!-- multiple reduce tags allowed -->
 *   <reduce>(character or string to strip)</reduce>
 *
 * </handler>
 * }
 * <p>
 * In addition to regular characters, you can specify these special characters
 * in your XML:
 * </p>
 * <ul>
 *   <li>\r (carriage returns)</li>
 *   <li>\n (line feed)</li>
 *   <li>\t (tab)</li>
 *   <li>\s (space)</li>
 * </ul>
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.transformer.impl.ReduceConsecutivesTransformer">
 *   <reduce>\s</reduce>
 * </handler>
 * }
 * <p>
 * The above example reduces multiple spaces into a single one.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 1.2.0
 * @see ReplaceTransformer
 */
@SuppressWarnings("javadoc")
public class ReduceConsecutivesTransformer extends AbstractStringTransformer {

    private boolean ignoreCase;
    private final List<String> reductions = new ArrayList<>();

    @Override
    protected void transformStringContent(final String reference,
            final StringBuilder content, final Properties metadata, final boolean parsed,
            final int sectionIndex) {

        String text = content.toString();
        content.setLength(0);
        Pattern pattern;
        for (String reduction : reductions) {
            String regex = "(" + escapeRegex(reduction) + ")+";
            if (ignoreCase) {
                pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            } else {
                pattern = Pattern.compile(regex);
            }
            text = pattern.matcher(text).replaceAll("$1");
        }
        content.append(text);
    }

    public List<String> getReductions() {
        return new ArrayList<>(reductions);
    }
    public void setReductions(final String... reductions) {
        CollectionUtil.setAll(this.reductions, reductions);
    }
    public void addReductions(final String... reductions) {
        this.reductions.addAll(Arrays.asList(reductions));
    }

    /**
     * Gets whether character matching should be case sensitive or not.
     * @return <code>true</code> if case sensitive.
     * @deprecated Since 3.0.0, use {@link #isIgnoreCase()}.
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return !ignoreCase;
    }
    /**
     * Sets whether to ignore case when matching characters or string
     * to reduce.
     * @param caseSensitive <code>true</code> to consider character case
     * @deprecated Since 3.0.0, use {@link #setIgnoreCase(boolean)}.
     */
    @Deprecated
    public void setCaseSensitive(final boolean caseSensitive) {
        this.ignoreCase = !caseSensitive;
    }

    /**
     * Gets whether to ignore case sensitivity.
     * @return <code>true</code> if ignoring character case
     * @since 3.0.0
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }
    /**
     * Sets whether to ignore case sensitivity.
     * @param ignoreCase <code>true</code> if ignoring character case
     * @since 3.0.0
     */
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    private String escapeRegex(final String text) {
        return text.replaceAll(
                "([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\$\\|])", "\\\\$1");
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        xml.checkDeprecated("@caseSensitive", "ignoreCase", true);

        setIgnoreCase(xml.getBoolean("@ignoreCase", ignoreCase));

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
        xml.setAttribute("ignoreCase", ignoreCase);
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
