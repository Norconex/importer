/* Copyright 2014-2017 Norconex Inc.
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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

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
import com.norconex.importer.handler.filter.AbstractCharStreamFilter;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.AbstractStringFilter;
import com.norconex.importer.handler.filter.OnMatch;

/**
 * <p>Filters a document based on a pattern matching in its content.  Based
 * on document size, it is possible the pattern matching will be done
 * in chunks, sometimes not achieving expected results.  Consider
 * using {@link AbstractCharStreamFilter} if this is a concern.
 * Refer to {@link AbstractDocumentFilter} for the inclusion/exclusion logic.
 * </p>
 * <p>
 * <b>Since 2.2.0</b>, the following regular expression flags are always
 * active: {@link Pattern#MULTILINE} and {@link Pattern#DOTALL}.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.RegexContentFilter"
 *          onMatch="[include|exclude]" 
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
 *      &lt;regex&gt;(regular expression of value to match)&lt;/regex&gt;
 *  &lt;/filter&gt;
 * </pre>
 * <h4>Usage example:</h4> 
 * <p>
 * This example will accept only documents containing word "apple".
 * </p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.RegexContentFilter"
 *          onMatch="include" &gt;
 *      &lt;regex&gt;.*apple.*&lt;/regex&gt;
 *  &lt;/filter&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see Pattern
 */
public class RegexContentFilter extends AbstractStringFilter {

    private static final Logger LOG = 
            LogManager.getLogger(RegexContentFilter.class);
    
    private boolean caseSensitive;
    private String regex;
    private Pattern pattern;

    
    public RegexContentFilter() {
        this(null, OnMatch.INCLUDE);
    }
    public RegexContentFilter(String regex) {
        this(regex, OnMatch.INCLUDE);
    }
    public RegexContentFilter(String regex, OnMatch onMatch) {
        this(regex, onMatch, false);
    }
    public RegexContentFilter(String regex, 
            OnMatch onMatch, boolean caseSensitive) {
        super();
        this.caseSensitive = caseSensitive;
        setOnMatch(onMatch);
        setRegex(regex);
    }

    public String getRegex() {
        return regex;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    public final void setRegex(String regex) {
        this.regex = regex;
        int baseFlags = Pattern.DOTALL;
        if (regex != null) {
            if (caseSensitive) {
                this.pattern = Pattern.compile(regex, baseFlags);
            } else {
                this.pattern = Pattern.compile(regex, baseFlags 
                        | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            }
        } else {
            this.pattern = Pattern.compile(".*");
        }
    }
    
    @Override
    protected boolean isStringContentMatching(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) throws ImporterHandlerException {

        if (StringUtils.isBlank(regex)) {
            return true;
        }
        if (pattern.matcher(content).matches()) {
            return true;
        }
        return false;
    }
    @Override
    protected void saveStringFilterToXML(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException {
        writer.writeAttribute("caseSensitive", 
                Boolean.toString(caseSensitive));
        writer.writeElementString("regex", regex);
    }
    @Override
    protected void loadStringFilterFromXML(XMLConfiguration xml)
            throws IOException {
        String regexOld = xml.getString("");
        if (StringUtils.isNotBlank(regexOld)) {
            LOG.warn("Regular expression must now be in <regex> tag.");
        }
        String theRegex = xml.getString("regex");
        if (StringUtils.isBlank(theRegex)) {
            theRegex = regexOld;
        }
        setRegex(theRegex);
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof RegexContentFilter)) {
            return false;
        }
        RegexContentFilter castOther = (RegexContentFilter) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(caseSensitive, castOther.caseSensitive)
                .append(regex, castOther.regex)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(caseSensitive)
                .append(regex)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("caseSensitive", caseSensitive)
                .append("regex", regex)
                .toString();
    }    
}
