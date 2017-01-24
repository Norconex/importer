/* Copyright 2016-2017 Norconex Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractStringTagger;

/**
 * <p>
 * Counts the number of matches of a given string (or string pattern) and
 * store the resulting value in a field in the specified "toField".
 * </p>
 * <p>
 * If no "fromField" is specified, the document content will be used.
 * If the "toField" already exists before counting begins, it will be 
 * overwritten with the result of the match count.
 * If within this tagger the "toField" is repeated,
 * the sum of all count will be added. 
 * If the fromField has multiple values, the total count of all matches
 * will be stored as a single value.
 * </p>
 * <p>Can be used as a pre-parse tagger on text document only when matching
 * strings on document content, or both as a pre-parse or post-parse handler
 * when the "fromField" is used.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.CountMatchesTagger"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" &gt;  
 *          
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *          
 *      &lt;countMatches 
 *              fromField="(optional source field)" 
 *              toField="(target field)"
 *              caseSensitive="[false|true]"
 *              regex="[false|true]"&gt;
 *          (text to match or regular expression)
 *      &lt;/countMatches&gt;
 *      &lt;!-- multiple countMatches tags allowed --&gt;
 *
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * <h3>XML example:</h3>
 * <p>
 * The following will count the number of segments in a URL: 
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.CountMatchesTagger"&gt;  
 *      &lt;countMatches 
 *              fromField="document.reference" 
 *              toField="urlSegmentCount"
 *              regex="true"&gt;
 *          /[^/]+
 *      &lt;/countMatches&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @see Pattern
 * @since 2.6.0
 */
public class CountMatchesTagger extends AbstractStringTagger {

    private static final Logger LOG = 
            LogManager.getLogger(CountMatchesTagger.class);
    
    private final List<MatchDetails> matchesDetails = new ArrayList<>();
    
    @Override
    protected void tagStringContent(String reference, StringBuilder content,
            ImporterMetadata metadata, boolean parsed, int sectionIndex) 
            throws ImporterHandlerException {
        // initialize all toFields to 0 so values can then be added 
        // without concerns with previous data
        if (sectionIndex == 0) {
            for (MatchDetails md : matchesDetails) {
                if (StringUtils.isNotBlank(md.getToField())) {
                    metadata.setInt(md.getToField(), 0);;
                }
            }
        }
        
        // perform the match counts
        for (MatchDetails md : matchesDetails) {
            // "toField" and value must be present.
            if (StringUtils.isBlank(md.getToField())) {
                LOG.debug("No \"toField\" specified: "
                        + "no match will be attempted.");
                continue;
            }
            if (StringUtils.isBlank(md.getValue())) {
                LOG.debug("No value to match specified: "
                        + "no match will be attempted.");
                continue;
            }
            
            boolean isFieldUsed = StringUtils.isNotBlank(md.getFromField());
            // if we have done the field matching already in the first section,
            // move on
            if (isFieldUsed && sectionIndex > 0) {
                continue;
            }
            
            List<String> sourceValues = null;
            if (isFieldUsed) {
                sourceValues = metadata.getStrings(md.getFromField());
            } else if (content != null) {
                sourceValues = new ArrayList<>(1);
                sourceValues.add(content.toString());
            }
            // if no content to perform matches on, so move on
            if (sourceValues.isEmpty()) {
                continue;
            }

            // perform the count
            for (String sourceValue : sourceValues) {
                int count = 0;
                if (md.isRegex()) {
                    count = countRegexMatches(
                            sourceValue, md.getValue(), md.isCaseSensitive());
                } else {
                    count = countSubstringMatches(
                            sourceValue, md.getValue(), md.isCaseSensitive());
                }
                
                int newCount = count + metadata.getInt(md.getToField());
                metadata.setInt(md.getToField(), newCount);
            }
        }
    }
    

    private int countRegexMatches(
            String haystack, String needle, boolean caseSensitive) {
        int flags = Pattern.DOTALL;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        Pattern p = Pattern.compile(needle, flags);
        Matcher m = p.matcher(haystack);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }
    private int countSubstringMatches(
            String haystack, String needle, boolean caseSensitive) {
        return countRegexMatches(
                haystack, Pattern.quote(needle), caseSensitive);
    }
        
    public List<MatchDetails> getMatchesDetails() {
        return Collections.unmodifiableList(matchesDetails);
    }

    public void removeMatchDetails(MatchDetails matchDetails) {
        matchesDetails.remove(matchDetails);
    }
    
    /**
     * Adds a match details.
     * @param matchDetails the match details
     */
    public void addMatchDetails(MatchDetails matchDetails) {
        if (matchesDetails != null) {
            matchesDetails.add(matchDetails);
        }
    }

    
    public static class MatchDetails {
        private String fromField;
        private String toField;
        private String value;
        private boolean regex;
        private boolean caseSensitive;
        public MatchDetails() {
            super();
        }
        public MatchDetails(
                String fromField, String toField, String value) {
            super();
            this.fromField = fromField;
            this.toField = toField;
            this.value = value;
        }
        public String getFromField() {
            return fromField;
        }
        public String getValue() {
            return value;
        }
        public String getToField() {
            return toField;
        }
        public boolean isRegex() {
            return regex;
        }
        /**
         * Whether the matching should be case sensitive or not.
         * @return <code>true</code> if case sensitive
         */
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
        /**
         * Sets the field with the value we want to perform matches on.
         * @param fromField field with the value to perform matches on
         */
        public void setFromField(String fromField) {
            this.fromField = fromField;
        }
        /**
         * Sets the text or regular expression to match
         * @param value the substring to match or regular expression
         */
        public void setValue(String value) {
            this.value = value;
        }
        /**
         * Sets the field to store the match count.
         * @param toField field to store the match count
         */
        public void setToField(String toField) {
            this.toField = toField;
        }
        /**
         * Sets whether the <code>value</code> to match is a regular expression.
         * @param regex <code>true</code> if <code>value</code> is a 
         *              regular expression
         */
        public void setRegex(boolean regex) {
            this.regex = regex;
        }
        /**
         * Sets whether to do a case sensitive match or not.
         * Matches are not case sensitive by default.
         * @param caseSensitive <code>true</code> if doing a case sensitive
         *                      match
         */
        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }
        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("fromField", fromField)
                    .append("toField", toField)
                    .append("value", value)
                    .append("regex", regex)
                    .append("caseSensitive", caseSensitive)
                    .toString();
        }
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof MatchDetails))
                return false;
            MatchDetails castOther = (MatchDetails) other;
            return new EqualsBuilder()
                    .append(fromField, castOther.fromField)
                    .append(toField, castOther.toField)
                    .append(value, castOther.value)
                    .append(regex, castOther.regex)
                    .append(caseSensitive, castOther.caseSensitive)
                    .isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                        .append(fromField)
                        .append(toField)
                        .append(value)
                        .append(regex)
                        .append(caseSensitive)
                        .toHashCode();
        }
    }
    
    @Override
    protected void loadStringTaggerFromXML(XMLConfiguration xml)
            throws IOException {
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("countMatches");
        for (HierarchicalConfiguration node : nodes) {
            MatchDetails m = new MatchDetails();
            m.setFromField(node.getString("[@fromField]"));
            m.setToField(node.getString("[@toField]", null));
            m.setRegex(node.getBoolean("[@regex]", false));
            m.setCaseSensitive(node.getBoolean("[@caseSensitive]", false));
            m.setValue(node.getString(""));
            addMatchDetails(m);
        }
    }

    @Override
    protected void saveStringTaggerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (MatchDetails match : matchesDetails) {
            writer.writeStartElement("countMatches");
            writer.writeAttributeString("fromField", match.getFromField());
            writer.writeAttributeString("toField", match.getToField());
            writer.writeAttributeBoolean("regex", match.isRegex());
            writer.writeAttributeBoolean(
                    "caseSensitive", match.isCaseSensitive());
            writer.writeCharacters(match.getValue());
            writer.writeEndElement();
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CountMatchesTagger)) {
            return false;
        }
        CountMatchesTagger castOther = (CountMatchesTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(matchesDetails, castOther.matchesDetails)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(matchesDetails)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("matchesDetails", matchesDetails)
                .toString();
    }
}
