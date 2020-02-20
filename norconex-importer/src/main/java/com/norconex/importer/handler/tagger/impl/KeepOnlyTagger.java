/* Copyright 2010-2020 Norconex Inc.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;
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
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.KeepOnlyTagger">
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *     (one or more matching fields to keep)
 *   </fieldMatcher>
 *  </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.tagger.impl.KeepOnlyTagger">
 *   <fieldMatcher method="regex">(title|description)</fieldMatcher>
 * </handler>
 * }
 * <p>
 * The above example keeps only the title and description fields from all
 * extracted fields.
 * </p>
 *
 * @author Pascal Essiembre
 * @see Pattern
 */
@SuppressWarnings("javadoc")
public class KeepOnlyTagger extends AbstractDocumentTagger {

    private static final Logger LOG =
            LoggerFactory.getLogger(KeepOnlyTagger.class);

    private final TextMatcher fieldMatcher = new TextMatcher();

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            Properties metadata, boolean parsed)
            throws ImporterHandlerException {

        for (String field : new HashSet<>(metadata.keySet())) {
            if (!fieldMatcher.matches(field)) {
                metadata.remove(field);
                LOG.debug("Not kept: {}", field);
            }
        }
    }

    /**
     * Gets field matcher.
     * @return field matcher
     * @since 3.0.0
     */
    public TextMatcher getFieldMatcher() {
        return fieldMatcher;
    }
    /**
     * Sets field matcher.
     * @param fieldMatcher field matcher
     * @since 3.0.0
     */
    public void setFieldMatcher(TextMatcher fieldMatcher) {
        this.fieldMatcher.copyFrom(fieldMatcher);
    }

    /**
     * Gets the pattern for fields to keep as first element.
     * @return fields to keep
     * @deprecated Since 3.0.0, use {@link #getFieldMatcher()}
     */
    @Deprecated
    public List<String> getFields() {
        return Arrays.asList(fieldMatcher.getPattern());
    }
    /**
     * Adds the pattern for fields to keep.
     * @param field fields to add
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)}
     */
    @Deprecated
    public void addField(String field) {
        fieldMatcher.setPattern(field);
    }
    /**
     * Does nothing.
     * @param field field to keep
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)}
     */
    @Deprecated
    public void removeField(String field) {
        //NOOP
    }
    /**
     * Gets field matcher pattern.
     * @return field matcher pattern
     * @deprecated Since 3.0.0, use {@link #getFieldMatcher()}
     */
    @Deprecated
    public String getFieldsRegex() {
        return fieldMatcher.getPattern();
    }
    /**
     * Sets field matcher pattern.
     * @param fieldsRegex field matcher pattern.
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)}
     */
    @Deprecated
    public void setFieldsRegex(String fieldsRegex) {
        this.fieldMatcher.setPattern(fieldsRegex).setMethod(Method.REGEX);
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        xml.checkDeprecated("fields", "fieldMatcher", true);
        xml.checkDeprecated("fieldsRegex", "fieldMatcher", true);
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
    }
    @Override
    protected void saveHandlerToXML(XML xml) {
        fieldMatcher.saveToXML(xml.addElement("fieldMatcher"));
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
