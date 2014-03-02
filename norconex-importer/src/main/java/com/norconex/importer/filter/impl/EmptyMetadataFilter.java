/* Copyright 2010-2013 Norconex Inc.
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
package com.norconex.importer.filter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.filter.AbstractOnMatchFilter;
import com.norconex.importer.filter.IDocumentFilter;
import com.norconex.importer.filter.OnMatch;
/**
 * Accepts or rejects a document based on whether specified metadata properties
 * are empty or not.  Any control characters (char < 32) are removed 
 * before evaluating if a property is empty or not.
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.filter.impl.EmptyMetadataFilter"
 *          onMatch="[include|exclude]" 
 *          properties="(coma separated list of properties to match)" /&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 1.2
 */
public class EmptyMetadataFilter extends AbstractOnMatchFilter
        implements IDocumentFilter, IXMLConfigurable {

    private static final long serialVersionUID = -8029862304058855686L;

    private String[] properties;
    

    public EmptyMetadataFilter() {
        this(OnMatch.INCLUDE, (String) null);
    }
    public EmptyMetadataFilter(
            OnMatch onMatch, String... properties) {
        super();
        this.properties = properties;
        setOnMatch(onMatch);
    }

    public String[] getProperties() {
        return properties;
    }
    public void setProperties(String... properties) {
        this.properties = properties;
    }

    @Override
    public final boolean acceptDocument(
            InputStream document, Properties metadata, boolean parsed)
            throws IOException {
        if (ArrayUtils.isEmpty(properties)) {
            return getOnMatch() == OnMatch.INCLUDE;
        }
        for (String prop : properties) {
            Collection<String> values =  metadata.getStrings(prop);
            
            boolean isPropEmpty = true;
            for (String value : values) {
                if (!StringUtils.isBlank(StringUtils.trim(value))) {
                    isPropEmpty = false;
                    break;
                }
            }
            if (isPropEmpty) {
                return getOnMatch() == OnMatch.INCLUDE;
            }
        }
        return getOnMatch() == OnMatch.EXCLUDE;
    }

    @Override
    public void loadFromXML(Reader in) {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
        String fieldsStr = xml.getString("[@properties]");
        String[] props = StringUtils.split(fieldsStr, ",");
        if (ArrayUtils.isEmpty(props)) {
            props = ArrayUtils.EMPTY_STRING_ARRAY;
        }
        setProperties(props);
        super.loadFromXML(xml);

    }
    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("filter");
            writer.writeAttribute("class", getClass().getCanonicalName());
            super.saveToXML(writer);
            writer.writeAttribute(
                    "properties", StringUtils.join(properties, ","));
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE)
            .appendSuper(super.toString())
            .append("properties", properties)
            .toString();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(properties)
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
            .append(properties, other.properties)
            .isEquals();
    }

}

