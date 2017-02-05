/* Copyright 2010-2017 Norconex Inc.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Strips any content found between a matching start and end strings.  The
 * matching strings are defined in pairs and multiple ones can be specified
 * at once.</p>
 * 
 * <p>This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.StripBetweenTransformer"
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
 *      &lt;stripBetween&gt;
 *          &lt;start&gt;(regex)&lt;/start&gt;
 *          &lt;end&gt;(regex)&lt;/end&gt;
 *      &lt;/stripBetween&gt;
 *      &lt;!-- multiple strignBetween tags allowed --&gt;
 *      
 *  &lt;/transformer&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following will strip all text between (and including) these two 
 * HTML comments: 
 * <code>&lt;!-- SIDENAV_START --&gt;</code> and
 * <code>&lt;!-- SIDENAV_END --&gt;</code>.
 * </p>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.StripBetweenTransformer"
 *          inclusive="true" &gt;
 *      &lt;stripBetween&gt;
 *          &lt;start&gt;&lt;![CDATA[&lt;!-- SIDENAV_START --&gt;]]&gt;&lt;/start&gt;
 *          &lt;end&gt;&lt;![CDATA[&lt;!-- SIDENAV_END --&gt;]]&gt;&lt;/end&gt;
 *      &lt;/stripBetween&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class StripBetweenTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private Comparator<Pair<String,String>> stripComparator = 
            new Comparator<Pair<String,String>>() {
        @Override
        public int compare(Pair<String,String> o1, Pair<String,String> o2) {
            return o1.getLeft().length() - o2.getLeft().length();
        }
    };
    private List<Pair<String, String>> stripPairs = new ArrayList<>();
    private boolean inclusive;
    private boolean caseSensitive;

    @Override
    protected void transformStringContent(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) {
        int flags = Pattern.DOTALL;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        for (Pair<String, String> pair : stripPairs) {
            List<Pair<Integer, Integer>> matches = new ArrayList<>();
            Pattern leftPattern = Pattern.compile(pair.getLeft(), flags);
            Matcher leftMatch = leftPattern.matcher(content);
            while (leftMatch.find()) {
                Pattern rightPattern = Pattern.compile(pair.getRight(), flags);
                Matcher rightMatch = rightPattern.matcher(content);
                if (rightMatch.find(leftMatch.end())) {
                    if (inclusive) {
                        matches.add(new ImmutablePair<Integer, Integer>(
                                leftMatch.start(), rightMatch.end()));
                    } else {
                        matches.add(new ImmutablePair<Integer, Integer>(
                                leftMatch.end(), rightMatch.start()));
                    }
                } else {
                    break;
                }
            }
            for (int i = matches.size() -1; i >= 0; i--) {
                Pair<Integer, Integer> matchPair = matches.get(i);
                content.delete(matchPair.getLeft(), matchPair.getRight());
            }
        }
    }

        
    public boolean isInclusive() {
        return inclusive;
    }
    /**
     * Sets whether start and end text pairs should themselves be stripped or 
     * not.
     * @param inclusive <code>true</code> to strip start and end text
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

    public synchronized void addStripEndpoints(String fromText, String toText) {
        if (StringUtils.isBlank(fromText) || StringUtils.isBlank(toText)) {
            return;
        }
        stripPairs.add(new ImmutablePair<String, String>(fromText, toText));
        Collections.sort(stripPairs, stripComparator);
    }
    public List<Pair<String, String>> getStripEndpoints() {
        return new ArrayList<>(stripPairs);
    }
    
    @Override
    protected void loadStringTransformerFromXML(XMLConfiguration xml)
            throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
        setInclusive(xml.getBoolean("[@inclusive]", false));
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("stripBetween");
        for (HierarchicalConfiguration node : nodes) {
            addStripEndpoints(
                    node.getString("start", null), node.getString("end", null));
        }
    }
    
    @Override
    protected void saveStringTransformerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
        writer.writeAttribute("inclusive", Boolean.toString(isInclusive()));
        for (Pair<String, String> pair : stripPairs) {
            writer.writeStartElement("stripBetween");
            writer.writeStartElement("start");
            writer.writeCharacters(pair.getLeft());
            writer.writeEndElement();
            writer.writeStartElement("end");
            writer.writeCharacters(pair.getRight());
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(caseSensitive)
            .append(inclusive)
            .append(stripPairs)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof StripBetweenTransformer)) {
            return false;
        }
        StripBetweenTransformer castOther = (StripBetweenTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(caseSensitive, castOther.caseSensitive)
                .append(inclusive, castOther.inclusive)
                .append(stripPairs, castOther.stripPairs)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("stripPairs", stripPairs)
                .append("inclusive", inclusive)
                .append("caseSensitive", caseSensitive)
                .toString();
    }

}
