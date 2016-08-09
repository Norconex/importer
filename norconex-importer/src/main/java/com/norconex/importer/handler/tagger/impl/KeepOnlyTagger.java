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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Keep only the metadata fields provided, delete all other ones.  
 * Exact field names (case-insensitive)
 * to keep can be provided as well as a regular expression that matches
 * one or many fields (since 2.1.0).</p>
 * 
 * <p><b>Note:</b> Unless you have good reasons for doing otherwise, it is 
 * recommended to use this handler as one of the last ones to be executed.
 * This is a good practice to ensure all metadata fields are available
 * to other handlers that may require them even if they are not otherwise
 * required.</p>
 * 
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * 
 * <p>XML configuration usage:</p>
 * 
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.KeepOnlyTagger"&gt;
 *      &lt;fields&gt;(coma-separated list of fields to keep)&lt;/fields&gt;
 *      &lt;fieldsRegex&gt;(regular expression matching fields to keep)&lt;/fieldsRegex&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see Pattern
 */
@SuppressWarnings("nls")
public class KeepOnlyTagger extends AbstractDocumentTagger {

    private static final Logger LOG = 
            LogManager.getLogger(KeepOnlyTagger.class);

    private final List<String> fields = new ArrayList<String>();
    private String fieldsRegex;
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        // If fields is empty, it means we should keep nothing
        if (fields.isEmpty() && StringUtils.isBlank(fieldsRegex)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clear all metadata from " + reference);
            }
            metadata.clear();
        } else {
            // Remove metadata not in fields
            Iterator<String> iter = metadata.keySet().iterator();
            List<String> removeList = new ArrayList<>();
            while (iter.hasNext()) {
                String name = iter.next();
                if (!mustKeep(name)) {
                    removeList.add(name);
                }
            }
            for (String key : removeList) {
                metadata.remove(key);
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removed metadata fields \""
                        + StringUtils.join(removeList, ",")
                        + "\" from " + reference);
            }
        }
    }

    private boolean mustKeep(String fieldToMatch) {
        // Check with exact field names
        for (String field : fields) {
            if (field.trim().equalsIgnoreCase(fieldToMatch.trim())) {
                return true;
            }
        }
        
        // Check with regex
        if (StringUtils.isNotBlank(fieldsRegex)
                && Pattern.matches(fieldsRegex, fieldToMatch)) {
            return true;
        }
        return false;
    }
    
    
    public List<String> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void addField(String field) {
        fields.add(field);
    }
    public void removeField(String field) {
        fields.remove(field);
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
                StringUtils.join(fields, ","));
        if (StringUtils.isNotBlank(fieldsStr)) {
            LOG.warn("Configuring fields to keep via the \"fields\" "
                   + "attribute is now deprecated. Now use the <fields> "
                   + "element instead.");
        }
        String fieldsElement = xml.getString(
                "fields", StringUtils.join(fields, ","));
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
        writer.writeElementString("fields", StringUtils.join(fields, ","));
        writer.writeElementString("fieldsRegex", fieldsRegex);
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof KeepOnlyTagger)) {
            return false;
        }
        KeepOnlyTagger castOther = (KeepOnlyTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(fieldsRegex, castOther.fieldsRegex)
                .append(fields, castOther.fields)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(fieldsRegex)
                .append(fields)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("fieldsRegex", fieldsRegex)
                .append("fields", fields)
                .toString();
    }
}
