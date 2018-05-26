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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration2.XMLConfiguration;
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
 * <p>Accepts or rejects a document based on whether any of the specified 
 * metadata fields are empty or not.  Any control characters (char &lt;= 32) 
 * are removed before evaluating if a property is empty or not.</p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.EmptyMetadataFilter"
 *          onMatch="[include|exclude]" 
 *          fields="(coma separated list of fields to match)" &gt;
 *          
 *    &lt;restrictTo caseSensitive="[false|true]"
 *            field="(name of header/metadata field name to match)"&gt;
 *        (regular expression of value to match)
 *    &lt;/restrictTo&gt;
 *    &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/filter&gt;
 * </pre>
 * <h4>Usage example:</h4> 
 * <p>
 * To exclude documents without titles:
 * </p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.EmptyMetadataFilter"
 *          onMatch="exclude" fields="title,dc:title" /&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 1.2
 */
public class EmptyMetadataFilter extends AbstractDocumentFilter {

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
        return ArrayUtils.clone(fields);
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

