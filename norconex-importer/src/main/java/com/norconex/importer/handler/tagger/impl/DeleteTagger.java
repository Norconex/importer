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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
/**
 * <p>
 * Delete the metadata fields provided.
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DeleteTagger"
 *      fields="[coma-separated list of fields to delete]" &gt
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
public class DeleteTagger extends AbstractDocumentTagger {

    private static final Logger LOG = LogManager.getLogger(DeleteTagger.class);
    
    private final List<String> fieldsToRemove = new ArrayList<String>();
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document, 
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        String[] metaFields = metadata.keySet().toArray(
                ArrayUtils.EMPTY_STRING_ARRAY);
        if (LOG.isDebugEnabled()) {
            LOG.debug("All meta fields: " + ArrayUtils.toString(metaFields));
            LOG.debug("All fields to remove: "
                    + ArrayUtils.toString(fieldsToRemove.toArray()));
        }
        for (String metaField : metaFields) {
            if (exists(metaField)) {
                metadata.remove(metaField);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed field: " + metaField);
                }
            }
        }
    }

    private boolean exists(String metaField) {
        for (String fieldToRemove : fieldsToRemove) {
            if (fieldToRemove.trim().equalsIgnoreCase(metaField.trim())) {
                return true;
            }
        }
        return false;
    }
    
    
    public List<String> getFields() {
        return Collections.unmodifiableList(fieldsToRemove);
    }

    public void addField(String field) {
        fieldsToRemove.add(field);
    }
    public void removeField(String field) {
        fieldsToRemove.remove(field);
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        String fieldsStr = xml.getString("[@fields]");
        String[] configFields = StringUtils.split(fieldsStr, ",");
        for (String field : configFields) {
            addField(field.trim());
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute("fields", StringUtils.join(fieldsToRemove, ","));
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeleteTagger [{");
        builder.append(StringUtils.join(fieldsToRemove, ","));
        builder.append("}]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldsToRemove == null) ? 0 : fieldsToRemove.hashCode());
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
        DeleteTagger other = (DeleteTagger) obj;
        if (fieldsToRemove == null) {
            if (other.fieldsToRemove != null) {
                return false;
            }
        } else if (!fieldsToRemove.equals(other.fieldsToRemove)) {
            return false;
        }
        return true;
    }
}
