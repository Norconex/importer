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
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * Keep only the metadata fields provided, delete all other ones.
 * <p />
 * <b>Note:</b> Unless you have good reasons for doing otherwise, it is 
 * recommended to use this handler as one of the last ones to be executed.
 * This is a good practice to ensure all metadata fields are available
 * to other handlers that may require them even if they are not otherwise
 * required.
 * <p />
 * Can be used both as a pre-parse or post-parse handler.
 * <p />
 * XML configuration usage:
 * <p />
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.KeepOnlyTagger"
 *      fields="[coma-separated list of fields to keep]" &gt
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
public class KeepOnlyTagger extends AbstractDocumentTagger {

    private static final Logger LOG = 
            LogManager.getLogger(KeepOnlyTagger.class);

    private final List<String> fields = new ArrayList<String>();
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        // If fields is empty, it means we should keep nothing
        if (fields.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clear all metadata from " + reference);
            }
            metadata.clear();
        } else {
            // Remove metadata not in fields
            Iterator<String> iter = metadata.keySet().iterator();
            List<String> removed = null;
            while (iter.hasNext()) {
                String name = iter.next();
                if (!exists(name)) {
                    if (LOG.isDebugEnabled()) {
                        if (removed == null) {
                            removed = new ArrayList<String>();
                        }
                        removed.add(name);
                    }
                    iter.remove();
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removed metadata fields \""
                        + StringUtils.join(removed, ",")
                        + "\" from " + reference);
            }
        }
    }

    private boolean exists(String fieldToMatch) {
        for (String field : fields) {
            if (field.equalsIgnoreCase(fieldToMatch)) {
                return true;
            }
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
        writer.writeAttribute("fields", StringUtils.join(fields, ","));
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("KeepOnlyTagger [{");
        builder.append(StringUtils.join(fields, ","));
        builder.append("}]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
        KeepOnlyTagger other = (KeepOnlyTagger) obj;
        if (fields == null) {
            if (other.fields != null) {
                return false;
            }
        } else if (!fields.equals(other.fields)) {
            return false;
        }
        return true;
    }
}
