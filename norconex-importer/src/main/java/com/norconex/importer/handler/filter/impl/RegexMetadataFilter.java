/* Copyright 2010-2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
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
        if (regex != null) {
            if (caseSensitive) {
                this.pattern = Pattern.compile(regex);
            } else {
                this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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
            String strVal = ObjectUtils.toString(value);
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

