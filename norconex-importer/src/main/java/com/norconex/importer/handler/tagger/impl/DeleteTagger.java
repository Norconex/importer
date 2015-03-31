/* Copyright 2010-2014 Norconex Inc.
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

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
 * Delete the metadata fields provided. Exact field names (case-insensitive)
 * to delete can be provided as well as a regular expression that matches
 * one or many fields (since 2.1.0).
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DeleteTagger"&gt;
 *      &lt;fields&gt;(coma-separated list of fields to delete)&lt;/fields&gt;
 *      &lt;fieldsRegex&gt;(regular expression matching fields to delete)&lt;/fieldsRegex&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
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
    private String fieldsRegex;
    
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
            if (mustDelete(metaField)) {
                metadata.remove(metaField);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed field: " + metaField);
                }
            }
        }
    }

    private boolean mustDelete(String metaField) {
        // Check with exact field names
        for (String fieldToRemove : fieldsToRemove) {
            if (fieldToRemove.trim().equalsIgnoreCase(metaField.trim())) {
                return true;
            }
        }

        // Check with regex
        if (StringUtils.isNotBlank(fieldsRegex)
                && Pattern.matches(fieldsRegex, metaField)) {
            return true;
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
    
    public String getFieldsRegex() {
        return fieldsRegex;
    }
    public void setFieldsRegex(String fieldsRegex) {
        this.fieldsRegex = fieldsRegex;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        String fieldsStr = xml.getString("[@fields]", 
                StringUtils.join(fieldsToRemove, ","));
        if (StringUtils.isNotBlank(fieldsStr)) {
            LOG.warn("Configuring fields to delete via the \"fields\" "
                   + "attribute is now deprecated. Now use the <fields> "
                   + "element instead.");
        }
        String fieldsElement = xml.getString(
                "fields", StringUtils.join(fieldsToRemove, ","));
        if (StringUtils.isNotBlank(fieldsElement)) {
            fieldsStr = fieldsElement;
        }
        
        String[] configFields = StringUtils.split(fieldsStr, ",");
        for (String field : configFields) {
            addField(field.trim());
        }
        setFieldsRegex(xml.getString("fieldsRegex", fieldsRegex));
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeElementString(
                "fields", StringUtils.join(fieldsToRemove, ","));
        writer.writeElementString("fieldsRegex", fieldsRegex);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeleteTagger [fieldsToRemove={");
        builder.append(StringUtils.join(fieldsToRemove, ","));
        builder.append("}, fieldsRegex=" + fieldsRegex + "]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((fieldsRegex == null) ? 0 : fieldsRegex.hashCode());
        result = prime * result
                + ((fieldsToRemove == null) ? 0 : fieldsToRemove.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DeleteTagger)) {
            return false;
        }
        DeleteTagger other = (DeleteTagger) obj;
        if (fieldsRegex == null) {
            if (other.fieldsRegex != null) {
                return false;
            }
        } else if (!fieldsRegex.equals(other.fieldsRegex)) {
            return false;
        }
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