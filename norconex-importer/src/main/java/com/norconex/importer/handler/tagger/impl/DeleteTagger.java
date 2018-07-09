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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
/**
 * <p>
 * Delete the metadata fields provided. Exact field names (case-insensitive)
 * to delete can be provided as well as a regular expression that matches
 * one or many fields.
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.DeleteTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;fields&gt;(coma-separated list of fields to delete)&lt;/fields&gt;
 *      &lt;fieldsRegex&gt;(regular expression matching fields to delete)&lt;/fieldsRegex&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following deletes all metadata fields starting with "X-"
 * (sometimes found after parsing):
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.DeleteTagger"&gt;
 *      &lt;fieldsRegex&gt;^[Xx]-.*&lt;/fieldsRegex&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 */
public class DeleteTagger extends AbstractDocumentTagger {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteTagger.class);

    private final List<String> fieldsToRemove = new ArrayList<>();
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
    protected void loadHandlerFromXML(XML xml) throws IOException {
        String fieldsStr = xml.getString("@fields",
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