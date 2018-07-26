/* Copyright 2010-2018 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.regex.Regex;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.tagger.AbstractStringTagger;

/**
 * <p>Extracts and add values found between a matching start and
 * end strings to a document metadata field.
 * The matching string end-points are defined in pairs and multiple ones
 * can be specified at once. The field specified for a pair of end-points
 * is considered a multi-value field.</p>
 * <p>
 * This class can be used as a pre-parsing handler on text documents only
 * or a post-parsing handler.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TextBetweenTagger"
 *          inclusive="[false|true]"
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
 *      &lt;textBetween name="targetFieldName"&gt;
 *          &lt;start&gt;(regex)&lt;/start&gt;
 *          &lt;end&gt;(regex)&lt;/end&gt;
 *      &lt;/textBetween&gt;
 *      &lt;!-- multiple textBetween tags allowed --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following example extract the content between "OPEN" and
 * "CLOSE" strings, excluding these strings, and store it in a "content"
 * field.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TextBetweenTagger" &gt;
 *      &lt;textBetween name="content"&gt;
 *          &lt;start&gt;OPEN&lt;/start&gt;
 *          &lt;end&gt;CLOSE&lt;/end&gt;
 *      &lt;/textBetween&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Khalid AlHomoud
 * @author Pascal Essiembre
 */
public class TextBetweenTagger
        extends AbstractStringTagger implements IXMLConfigurable {

    private final Set<TextBetween> betweens = new TreeSet<>();

    private boolean inclusive;
    private boolean caseSensitive;

    @Override
    protected void tagStringContent(String reference, StringBuilder content,
            ImporterMetadata metadata, boolean parsed, int sectionIndex) {

        Regex regex = new Regex().dotAll().setCaseInsensitive(!caseSensitive);


//        int flags = Pattern.DOTALL;
//        if (!caseSensitive) {
//            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
//        }
        for (TextBetween between : betweens) {
            List<Pair<Integer, Integer>> matches = new ArrayList<>();
                                //Pattern.compile(between.start, flags);
            Pattern leftPattern = regex.compile(between.start);
            Matcher leftMatch = leftPattern.matcher(content);
            while (leftMatch.find()) {
                                     //Pattern.compile(between.end, flags);
                Pattern rightPattern = regex.compile(between.end);
                Matcher rightMatch = rightPattern.matcher(content);
                if (rightMatch.find(leftMatch.end())) {
                    if (inclusive) {
                        matches.add(new ImmutablePair<>(
                                leftMatch.start(), rightMatch.end()));
                    } else {
                        matches.add(new ImmutablePair<>(
                                leftMatch.end(), rightMatch.start()));
                    }
                } else {
                    break;
                }
            }
            for (int i = matches.size() -1; i >= 0; i--) {
                Pair<Integer, Integer> matchPair = matches.get(i);
                String value = content.substring(
                        matchPair.getLeft(), matchPair.getRight());
                if (value != null) {
                    metadata.addString(between.name, value);
                }
            }
        }
    }

    public boolean isInclusive() {
        return inclusive;
    }
    /**
     * Sets whether start and end text pairs should be kept or
     * not.
     * @param inclusive <code>true</code> to keep matching start and end text
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
    /**
     * Adds a new pair of end points to match.
     * @param name target metadata field name where to store the extracted
     *             values
     * @param fromText the left string to match
     * @param toText the right string to match
     */
    public void addTextEndpoints(String name, String fromText, String toText) {
        if (StringUtils.isBlank(name)
                || StringUtils.isBlank(fromText)
                || StringUtils.isBlank(toText)) {
            return;
        }
        betweens.add(new TextBetween(name, fromText, toText));
    }

    @Override
    protected void loadStringTaggerFromXML(XML xml) {
        setCaseSensitive(xml.getBoolean("@caseSensitive", false));
        setInclusive(xml.getBoolean("@inclusive", false));
        List<XML> nodes = xml.getXMLList("textBetween");
        for (XML node : nodes) {
            addTextEndpoints(
                    node.getString("@name"),
                    node.getString("start", null),
                    node.getString("end", null));
        }
    }

    @Override
    protected void saveStringTaggerToXML(XML xml) {
         xml.setAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
        xml.setAttribute("inclusive", Boolean.toString(isInclusive()));
        for (TextBetween between : betweens) {
            XML bxml = xml.addElement("textBetween")
                    .setAttribute("name", between.name);
            bxml.addElement("start", between.start);
            bxml.addElement("end", between.end);
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

    private static class TextBetween implements Comparable<TextBetween> {
        private final String name;
        private final String start;
        private final String end;
        public TextBetween(String name, String start, String end) {
            super();
            this.name = name;
            this.start = start;
            this.end = end;
        }
        @Override
        public int compareTo(final TextBetween other) {
            return CompareToBuilder.reflectionCompare(this, other);
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
