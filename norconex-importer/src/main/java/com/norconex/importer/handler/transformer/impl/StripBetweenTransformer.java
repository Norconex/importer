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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Strips any content found between a matching start and end strings.  The
 * matching strings are defined in pairs and multiple ones can be specified
 * at once.</p>
 *
 * <p>This class can be used as a pre-parsing (text content-types only)
 * or post-parsing handlers.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.transformer.impl.StripBetweenTransformer"
 *     {@nx.include com.norconex.importer.handler.transformer.AbstractStringTransformer#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <!-- multiple stripBetween tags allowed -->
 *   <stripBetween
 *       inclusive="[false|true]">
 *     <startMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (expression matching "left" delimiter)
 *     </startMatcher>
 *     <endMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (expression matching "right" delimiter)
 *     </endMatcher>
 *   </stripBetween>
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.transformer.impl.StripBetweenTransformer"
 *     inclusive="true">
 *   <stripBetween>
 *     <startMatcher><![CDATA[<!-- SIDENAV_START -->]]></startMatcher>
 *     <endMatcher><![CDATA[<!-- SIDENAV_END -->]]></endMatcher>
 *   </stripBetween>
 * </handler>
 * }
 * <p>
 * The following will strip all text between (and including) these two
 * HTML comments:
 * <code>&lt;!-- SIDENAV_START --&gt;</code> and
 * <code>&lt;!-- SIDENAV_END --&gt;</code>.
 * </p>
 *
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public class StripBetweenTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private final List<StripBetweenDetails> betweens = new ArrayList<>();

    @Override
    protected void transformStringContent(final String reference,
            final StringBuilder content, final Properties metadata,
            final boolean parsed,
            final int sectionIndex) {

        for (StripBetweenDetails between : betweens) {
            Matcher leftMatch = between.startMatcher.toRegexMatcher(content);
            while (leftMatch.find()) {
                Matcher rightMatch = between.endMatcher.toRegexMatcher(content);
                if (rightMatch.find(leftMatch.end())) {
                    if (between.inclusive) {
                        content.delete(leftMatch.start(), rightMatch.end());
                    } else {
                        content.delete(leftMatch.end(), rightMatch.start());
                    }
                } else {
                    break;
                }
                leftMatch = between.startMatcher.toRegexMatcher(content);
            }
        }
    }

    /**
     * Adds strip between instructions.
     * @param details "strip between" details
     * @since 3.0.0
     */
    public void addStripBetweenDetails(StripBetweenDetails details) {
        betweens.add(details);
    }
    /**
     * Gets text between instructions.
     * @return "strip between" details
     * @since 3.0.0
     */
    public List<StripBetweenDetails> getStripBetweenDetailsList() {
        return new ArrayList<>(betweens);
    }

    /**
     * Gets whether start and end text pairs should be stripped or
     * not.
     * @return always <code>false</code>
     * @deprecated Since 3.0.0, use {@link StripBetweenDetails#isInclusive()}
     */
    @Deprecated
    public boolean isInclusive() {
        return false;
    }
    /**
     * Sets whether start and end text pairs should be stripped or
     * not. <b>Calling this method has no effect.</b>
     * @param inclusive <code>true</code> to keep matching start and end text
     * @deprecated Since 3.0.0, use {@link StripBetweenDetails#setInclusive(boolean)}
     */
    @Deprecated
    public void setInclusive(boolean inclusive) {
        //NOOP
    }

    /**
     * Gets whether to ignore case when matching start and end text.
     * @return always <code>false</code>
     * @deprecated Since 3.0.0, use {@link StripBetweenDetails#isCaseSensitive()}
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return false;
    }
    /**
     * Sets whether to ignore case when matching start and end text.
     * <b>Calling this method has no effect.</b>
     * @param caseSensitive <code>true</code> to consider character case
     * @deprecated Since 3.0.0,
     *             use {@link StripBetweenDetails#setCaseSensitive(boolean)}
     */
    @Deprecated
    public void setCaseSensitive(boolean caseSensitive) {
        //NOOP
    }

    /**
     * Adds a new pair of end points to match for stripping.
     * @param fromText the left string to match
     * @param toText the right string to match
     * @deprecated Since 3.0.0, use
     *              {@link #addStripBetweenDetails(StripBetweenDetails)}
     */
    @Deprecated
    public synchronized void addStripEndpoints(
            final String fromText, final String toText) {

        if (StringUtils.isAnyBlank(fromText, toText)) {
            return;
        }
        betweens.add(new StripBetweenDetails(
                TextMatcher.basic(fromText), TextMatcher.basic(toText)));
    }
    /**
     * Gets an empty list.
     * @return empty list
     * @deprecated Since 3.0.0, use {@link #getStripBetweenDetailsList()}.
     */
    @Deprecated
    public List<Pair<String, String>> getStripEndpoints() {
        return new ArrayList<>();
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        xml.checkDeprecated("@caseSensitive",
                "startMatcher@ignoreCase and endMatcher@ignoreCase", true);
        xml.checkDeprecated("@inclusive", "stripBetween@inclusive", true);
        for (XML node : xml.getXMLList("stripBetween")) {
            StripBetweenDetails d = new StripBetweenDetails();
            d.setInclusive(node.getBoolean("@inclusive", false));
            d.startMatcher.loadFromXML(node.getXML("startMatcher"));
            d.endMatcher.loadFromXML(node.getXML("endMatcher"));
            addStripBetweenDetails(d);
        }
    }

    @Override
    protected void saveStringTransformerToXML(final XML xml) {
        for (StripBetweenDetails between : betweens) {
            XML bxml = xml.addElement("stripBetween")
                    .setAttribute("inclusive", between.inclusive);
            between.startMatcher.saveToXML(bxml.addElement("startMatcher"));
            between.endMatcher.saveToXML(bxml.addElement("endMatcher"));
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

    /**
     * @since 3.0.0
     */
    public static class StripBetweenDetails {
        private final TextMatcher startMatcher = new TextMatcher();
        private final TextMatcher endMatcher = new TextMatcher();
        private boolean inclusive;
        /**
         * Constructor.
         */
        public StripBetweenDetails() {
            super();
        }
        /**
         * Constructor.
         * @param startMatcher start matcher
         * @param endMatcher end matcher
         */
        public StripBetweenDetails(
                TextMatcher startMatcher, TextMatcher endMatcher) {
            super();
            this.startMatcher.copyFrom(startMatcher);
            this.endMatcher.copyFrom(endMatcher);
        }

        /**
         * Gets the start delimiter matcher for text to strip.
         * @return start delimiter matcher
         */
        public TextMatcher getStartMatcher() {
            return startMatcher;
        }
        /**
         * Sets the start delimiter matcher for text to strip.
         * @param startMatcher start delimiter matcher
         */
        public void setStartMatcher(TextMatcher startMatcher) {
            this.startMatcher.copyFrom(startMatcher);
        }
        /**
         * Gets the end delimiter matcher for text to strip.
         * @return end delimiter matcher
         */
        public TextMatcher getEndMatcher() {
            return endMatcher;
        }
        /**
         * Sets the end delimiter matcher for text to strip.
         * @param endMatcher end delimiter matcher
         */
        public void setEndMatcher(TextMatcher endMatcher) {
            this.endMatcher.copyFrom(endMatcher);
        }

        public boolean isInclusive() {
            return inclusive;
        }
        public void setInclusive(boolean inclusive) {
            this.inclusive = inclusive;
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
}
