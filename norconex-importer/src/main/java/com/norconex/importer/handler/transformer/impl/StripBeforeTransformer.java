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

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Strips any content found before first match found for given pattern.</p>
 *
 * <p>This class can be used as a pre-parsing (text content-types only)
 * or post-parsing handlers.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.transformer.impl.StripBeforeTransformer"
 *     inclusive="[false|true]"
 *     {@nx.include com.norconex.importer.handler.transformer.AbstractStringTransformer#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <stripBeforeMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>>
 *     (expression matching text up to which to strip)
 *   </stripBeforeMatcher>
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.transformer.impl.StripBeforeTransformer"
 *     inclusive="true">
 *   <stripBeforeMatcher><![CDATA[<!-- HEADER_END -->]]></stripBeforeMatcher>
 * </handler>
 * }
 *
 * <p>
 * The above example will strip all text up to and including this HTML comment:
 * <code>&lt;!-- HEADER_END --&gt;</code>.
 * </p>
 *
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public class StripBeforeTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(StripBeforeTransformer.class);

    private boolean inclusive;
    private final TextMatcher stripBeforeMatcher = new TextMatcher();

    @Override
    protected void transformStringContent(HandlerDoc doc,
            final StringBuilder content, final ParseState parseState,
            final int sectionIndex) {
        if (stripBeforeMatcher.getPattern() == null) {
            LOG.error("No matcher pattern provided.");
            return;
        }

        Matcher m = stripBeforeMatcher.toRegexMatcher(content);
        if (m.find()) {
            if (inclusive) {
                content.delete(0, m.end());
            } else {
                content.delete(0, m.start());
            }
        }
    }

    /**
     * Gets the matcher for the text up to which to strip content.
     * @return text matcher
     * @since 3.0.0
     */
    public TextMatcher getStripBeforeMatcher() {
        return stripBeforeMatcher;
    }
    /**
     * Sets the matcher for the text up to which to strip content.
     * @param stripBeforeMatcher text matcher
     * @since 3.0.0
     */
    public void setStripBeforeMatcher(TextMatcher stripBeforeMatcher) {
        this.stripBeforeMatcher.copyFrom(stripBeforeMatcher);
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
     * @deprecated Since 3.0.0, use {@link #getStripBeforeMatcher()}.
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return !stripBeforeMatcher.isIgnoreCase();
    }
    /**
     * Sets whether matching is case sensitive.
     * @param caseSensitive <code>true</code> if case sensitive
     * @deprecated Since 3.0.0, use {@link #setStripBeforeMatcher(TextMatcher)}.
     */
    @Deprecated
    public void setCaseSensitive(final boolean caseSensitive) {
        stripBeforeMatcher.setIgnoreCase(!caseSensitive);
    }

    /**
     * Gets the expression matching text up to which to strip.
     * @return expression
     * @deprecated Since 3.0.0, use {@link #getStripBeforeMatcher()}.
     */
    @Deprecated
    public String getStripBeforeRegex() {
        return stripBeforeMatcher.getPattern();
    }
    /**
     * Sets the expression matching text up to which to strip.
     * @param regex expression
     * @deprecated Since 3.0.0, use {@link #setStripBeforeMatcher(TextMatcher)}.
     */
    @Deprecated
    public void setStripBeforeRegex(final String regex) {
        this.stripBeforeMatcher.setPattern(regex).setMethod(Method.REGEX);
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        xml.checkDeprecated(
                "@caseSensitive", "stripBeforeMatcher@ignoreCase", true);
        xml.checkDeprecated("stripBeforeRegex", "stripBeforeMatcher", true);
        setInclusive(xml.getBoolean("@inclusive", inclusive));
        stripBeforeMatcher.loadFromXML(xml.getXML("stripBeforeMatcher"));
    }

    @Override
    protected void saveStringTransformerToXML(final XML xml) {
        xml.setAttribute("inclusive", inclusive);
        stripBeforeMatcher.saveToXML(xml.addElement("stripBeforeMatcher"));
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
