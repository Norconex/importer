/* Copyright 2010-2014 Norconex Inc.
 * 
 * This file is part of Norconex HTTP Collector.
 * 
 * Norconex HTTP Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex HTTP Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex HTTP Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
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
 * Accepts or rejects a document based on whether specified metadata fields
 * are empty or not.  Any control characters (char < 32) are removed 
 * before evaluating if a property is empty or not.
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.EmptyMetadataFilter"
 *          onMatch="[include|exclude]" 
 *          fields="(coma separated list of fields to match)" /&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 1.2
 */
public class EmptyMetadataFilter extends AbstractDocumentFilter {

    private static final long serialVersionUID = -8029862304058855686L;

    private String[] fields;
    

    public EmptyMetadataFilter() {
        this(OnMatch.INCLUDE, (String) null);
    }
    public EmptyMetadataFilter(
            OnMatch onMatch, String... properties) {
        super();
        this.fields = properties;
        setOnMatch(onMatch);
    }

    public String[] getFields() {
        return fields;
    }
    public void setFields(String... properties) {
        this.fields = properties;
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (ArrayUtils.isEmpty(fields)) {
            return true;
        }
        for (String prop : fields) {
            Collection<String> values =  metadata.getStrings(prop);
            
            boolean isPropEmpty = true;
            for (String value : values) {
                if (!StringUtils.isBlank(StringUtils.trim(value))) {
                    isPropEmpty = false;
                    break;
                }
            }
            if (isPropEmpty) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        String fieldsStr = xml.getString("[@fields]");
        String[] props = StringUtils.split(fieldsStr, ",");
        if (ArrayUtils.isEmpty(props)) {
            props = ArrayUtils.EMPTY_STRING_ARRAY;
        }
        setFields(props);
    }
    
    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute(
                "fields", StringUtils.join(fields, ","));
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .appendSuper(super.toString())
            .append("fields", fields)
            .toString();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(fields)
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
        if (!(obj instanceof EmptyMetadataFilter)) {
            return false;
        }
        EmptyMetadataFilter other = (EmptyMetadataFilter) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(fields, other.fields)
            .isEquals();
    }

}

