/* Copyright 2010-2018 Norconex Inc.
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.xml.XML;
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
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.KeepOnlyTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;fields&gt;(coma-separated list of fields to keep)&lt;/fields&gt;
 *      &lt;fieldsRegex&gt;(regular expression matching fields to keep)&lt;/fieldsRegex&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <p>
 * The following keeps only the title and description fields from all
 * extracted fields.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.KeepOnlyTagger"&gt;
 *      &lt;fields&gt;title, description&lt;/fields&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @see Pattern
 */
public class KeepOnlyTagger extends AbstractDocumentTagger {

    private static final Logger LOG =
            LoggerFactory.getLogger(KeepOnlyTagger.class);

    private final List<String> fields = new ArrayList<>();
    private String fieldsRegex;

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        // If fields is empty, it means we should keep nothing
        if (fields.isEmpty() && StringUtils.isBlank(fieldsRegex)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clear all metadata from: {}", reference);
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
        return StringUtils.isNotBlank(fieldsRegex)
                && Pattern.matches(fieldsRegex, fieldToMatch);
    }

    public List<String> getFields() {
        return Collections.unmodifiableList(fields);
    }
    /**
     * Sets the fields to keep.
     * @param fields fields to keep
     * @since 3.0.0
     */
    public void setFields(List<String> fields) {
        CollectionUtil.setAll(this.fields, fields);
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
    protected void loadHandlerFromXML(XML xml) {
        setFields(xml.getDelimitedStringList("fields", fields));
        setFieldsRegex(xml.getString("fieldsRegex", fieldsRegex));
    }
    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.addDelimitedElementList("fields", fields);
        xml.addElement("fieldsRegex", fieldsRegex);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
