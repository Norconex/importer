/* Copyright 2010-2015 Norconex Inc.
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
import java.util.Collection;
import java.util.Objects;
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
 * Accepts or rejects a document based on its field values using 
 * regular expression.  
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.RegexMetadataFilter"
 *          onMatch="[include|exclude]" 
 *          caseSensitive="[false|true]"
 *          field="(name of metadata name to match)" &gt;
 *      (regular expression of value to match)
 *  &lt;/filter&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see Pattern
 */
public class RegexMetadataFilter extends AbstractDocumentFilter {

    private boolean caseSensitive;
    private String field;
    private String regex;
    private Pattern pattern;

    public RegexMetadataFilter() {
        this(null, null, OnMatch.INCLUDE);
    }
    public RegexMetadataFilter(String field, String regex) {
        this(field, regex, OnMatch.INCLUDE);
    }
    public RegexMetadataFilter(String field, String regex, OnMatch onMatch) {
        this(field, regex, onMatch, false);
    }
    public RegexMetadataFilter(
            String property, String regex, 
            OnMatch onMatch, boolean caseSensitive) {
        super();
        this.caseSensitive = caseSensitive;
        this.field = property;
        setOnMatch(onMatch);
        setRegex(regex);
    }
    
    public String getRegex() {
        return regex;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    public String getField() {
        return field;
    }
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    public void setField(String property) {
        this.field = property;
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
        Collection<String> values =  metadata.getStrings(field);
        for (Object value : values) {
            String strVal = Objects.toString(value, StringUtils.EMPTY);
            if (pattern.matcher(strVal).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        setField(xml.getString("[@field]"));
        setRegex(xml.getString(""));
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
    }
    
    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute("field", field);
        writer.writeAttribute("caseSensitive", 
                Boolean.toString(caseSensitive));
        writer.writeCharacters(regex == null ? "" : regex);
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .appendSuper(super.toString())
            .append(field)
            .append("regex", regex)
            .append("caseSensitive", caseSensitive)
            .toString();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(caseSensitive)
            .append(field)
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
        if (!(obj instanceof RegexMetadataFilter)) {
            return false;
        }
        RegexMetadataFilter other = (RegexMetadataFilter) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(caseSensitive, other.caseSensitive)
            .append(field, other.field)
            .append(regex, other.regex)
            .isEquals();
    }

}

