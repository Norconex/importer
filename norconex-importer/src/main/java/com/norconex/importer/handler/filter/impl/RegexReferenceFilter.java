/* Copyright 2017 Norconex Inc.
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
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
/**
 * <p>Accepts or rejects a document based on its reference (e.g. URL).
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.RegexReferenceFilter"
 *          onMatch="[include|exclude]" 
 *          caseSensitive="[false|true]"&gt;
 *          
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *          
 *      &lt;regex&gt;(regular expression of reference to match)&lt;/regex&gt;
 *  &lt;/filter&gt;
 * </pre>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * 
 * <h4>Usage example:</h4> 
 * <p>
 * The following will reject documents having "/login/" in their reference. 
 * </p> 
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.RegexReferenceFilter"
 *          onMatch="exclude"&gt;
 *      .*&#47;login/.*
 *  &lt;/filter&gt; 
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.7.0
 */
public class RegexReferenceFilter extends AbstractDocumentFilter {

    private boolean caseSensitive;
    private String regex;
    private Pattern pattern;

    public RegexReferenceFilter() {
        this(null, OnMatch.INCLUDE);
    }
    public RegexReferenceFilter(String regex) {
        this(regex, OnMatch.INCLUDE);
    }
    public RegexReferenceFilter(String regex, OnMatch onMatch) {
        this(regex, onMatch, false);
    }
    public RegexReferenceFilter(
            String regex, OnMatch onMatch, boolean caseSensitive) {
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
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (StringUtils.isBlank(regex)) {
            return true;
        }
        return pattern.matcher(reference).matches();
    }

    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", caseSensitive));
        setRegex(xml.getString("regex", regex));
    }
    
    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeBoolean("caseSensitive", caseSensitive);
        writer.writeElementString("regex", regex);
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .appendSuper(super.toString())
            .append("regex", regex)
            .append("caseSensitive", caseSensitive)
            .toString();
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RegexReferenceFilter)) {
            return false;
        }
        RegexReferenceFilter other = (RegexReferenceFilter) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(caseSensitive, other.caseSensitive)
            .append(regex, other.regex)
            .isEquals();
    }
}

