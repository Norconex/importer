/* Copyright 2010-2020 Norconex Inc.
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

import java.util.regex.Matcher;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Strips any content found after first match found for given pattern.</p>
 *
 * <p>This class can be used as a pre-parsing (text content-types only)
 * or post-parsing handlers.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.transformer.impl.StripAfterTransformer"
 *     inclusive="[false|true]"
 *     {@nx.include com.norconex.importer.handler.transformer.AbstractStringTransformer#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <stripAfterMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>>
 *     (expression matching text from which to strip)
 *   </stripAfterMatcher>
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.transformer.impl.StripAfterTransformer"
 *     inclusive="true">
 *   <stripAfterMatcher><![CDATA[<!-- FOOTER -->]]></stripAfterMatcher>
 * </handler>
 * }
 * <p>
 * The above example will strip all text starting with the following HTML
 * comment and everything after it:
 * <code>&lt;!-- FOOTER --&gt;</code>.
 * </p>
 *
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public class StripAfterTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(StripAfterTransformer.class);

    private boolean inclusive;
    private final TextMatcher stripAfterMatcher = new TextMatcher();

    @Override
    protected void transformStringContent(final String reference,
            final StringBuilder content, final Properties metadata,
            final boolean parsed, final int sectionIndex) {
        if (stripAfterMatcher.getPattern() == null) {
            LOG.error("No matcher pattern provided.");
            return;
        }

        Matcher m = stripAfterMatcher.toRegexMatcher(content);
        if (m.find()) {
            if (inclusive) {
                content.delete(m.start(), content.length());
            } else {
                content.delete(m.end(), content.length());
            }
        }
    }

    /**
     * Gets the matcher for the text from which to strip content.
     * @return text matcher
     * @since 3.0.0
     */
    public TextMatcher getStripAfterMatcher() {
        return stripAfterMatcher;
    }
    /**
     * Sets the matcher for the text from which to strip content.
     * @param stripAfterMatcher text matcher
     * @since 3.0.0
     */
    public void setStripAfterMatcher(TextMatcher stripAfterMatcher) {
        this.stripAfterMatcher.copyFrom(stripAfterMatcher);
    }

    public boolean isInclusive() {
        return inclusive;
    }
    /**
     * Sets whether the match itself should be stripped or not.
     * @param inclusive <code>true</code> to strip start and end text
     */
    public void setInclusive(final boolean inclusive) {
        this.inclusive = inclusive;
    }

    /**
     * Gets whether matching is case sensitive.
     * @return <code>true</code> if case sensitive
     * @deprecated Since 3.0.0, use {@link #getStripAfterMatcher()}.
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return !stripAfterMatcher.isIgnoreCase();
    }
    /**
     * Sets whether matching is case sensitive.
     * @param caseSensitive <code>true</code> if case sensitive
     * @deprecated Since 3.0.0, use {@link #setStripAfterMatcher(TextMatcher)}.
     */
    @Deprecated
    public void setCaseSensitive(final boolean caseSensitive) {
        stripAfterMatcher.setIgnoreCase(!caseSensitive);
    }

    /**
     * Gets the expression matching text after which to strip.
     * @return expression
     * @deprecated Since 3.0.0, use {@link #getStripAfterMatcher()}.
     */
    @Deprecated
    public String getStripAfterRegex() {
        return stripAfterMatcher.getPattern();
    }
    /**
     * Sets the expression matching text after which to strip.
     * @param regex expression
     * @deprecated Since 3.0.0, use {@link #setStripAfterMatcher(TextMatcher)}.
     */
    @Deprecated
    public void setStripAfterRegex(final String regex) {
        this.stripAfterMatcher.setPattern(regex).setMethod(Method.REGEX);
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        xml.checkDeprecated(
                "@caseSensitive", "stripAfterMatcher@ignoreCase", true);
        xml.checkDeprecated("stripAfterRegex", "stripAfterMatcher", true);
        setInclusive(xml.getBoolean("@inclusive", inclusive));
        stripAfterMatcher.loadFromXML(xml.getXML("stripAfterMatcher"));
    }

    @Override
    protected void saveStringTransformerToXML(final XML xml) {
        xml.setAttribute("inclusive", inclusive);
        stripAfterMatcher.saveToXML(xml.addElement("stripAfterMatcher"));
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
