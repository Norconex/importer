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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
/**
 * <p>
 * Forces a metadata field to be single-value.  The action can be one of the 
 * following:
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <pre>
 *    keepFirst          Keeps the first occurrence found.
 *    keepLast           Keeps the first occurrence found.
 *    mergeWith:&lt;sep&gt;    Merges all occurrences, joining them with the
 *                       specified separator (&lt;sep&gt;). 
 * </pre>
 * <p>
 * If you do not specify any action, the default behavior is to merge all
 * occurrences, joining values with a comma.
 * </p> 
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.SingleValueTagger"&gt;
 *      &lt;singleValue field="FIELD_NAME" action="[keepFirst|keepLast|mergeWith:&lt;separator&gt;]"/&gt
 *      &lt;-- multiple single value fields allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]" &gt;
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class ForceSingleValueTagger extends AbstractDocumentTagger {

    private static final long serialVersionUID = -430885800148300053L;

    private final Map<String, String> singleFields = 
            new HashMap<String, String>();
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document, 
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        for (String name : singleFields.keySet()) {
            List<String> values = metadata.getStrings(name);  
            String action = singleFields.get(name);
            if (values != null && !values.isEmpty() 
                    && StringUtils.isNotBlank(action)) {
                String singleValue = null;
                if ("keepFirst".equalsIgnoreCase(action)) {
                    singleValue = values.get(0);
                } else if ("keepLast".equalsIgnoreCase(action)) {
                    singleValue = values.get(values.size() - 1);
                } else if (StringUtils.startsWithIgnoreCase(
                        action, "mergeWith")) {
                    String sep = StringUtils.substringAfter(action, ":");
                    singleValue = StringUtils.join(values, sep);
                } else {
                    singleValue = StringUtils.join(values, ",");
                }
                metadata.setString(name, singleValue);
            }
        }
    }

    public Map<String, String> getSingleValueFields() {
        return Collections.unmodifiableMap(singleFields);
    }

    public void addSingleValueField(String field, String action) {
        if (field != null && action != null) {
            singleFields.put(field, action);
        }
    }
    public void removeSingleValueField(String name) {
        singleFields.remove(name);
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("singleValue");
        for (HierarchicalConfiguration node : nodes) {
            String name = node.getString("[@field]");
            String action = node.getString("[@action]");
            addSingleValueField(name, action);
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (String name : singleFields.keySet()) {
            String action = singleFields.get(name);
            if (action != null) {
                writer.writeStartElement("singleValue");
                writer.writeAttribute("field", name);
                writer.writeAttribute("action", action);
                writer.writeEndElement();
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ForceSingleValueTagger [{");
        boolean first = true;
        for (String name : singleFields.keySet()) {
            String action = singleFields.get(name);
            if (action != null) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append("[field=").append(name)
                    .append(", value=").append(action)
                    .append("]");
                first = false;
            }
        }
        builder.append("}]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((singleFields == null) ? 0 : singleFields.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ForceSingleValueTagger other = (ForceSingleValueTagger) obj;
        if (singleFields == null) {
            if (other.singleFields != null) {
                return false;
            }
        } else if (!singleFields.equals(other.singleFields)) {
            return false;
        }
        return true;
    }
}
