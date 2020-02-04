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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
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
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.ForceSingleValueTagger"
 *     action="[keepFirst|keepLast|mergeWith:<separator>]">
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (one or more matching fields to force having a single value)
 *   </fieldMatcher>
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.tagger.impl.ForceSingleValueTagger"
 *     action="keepFirst">
 *   <fieldMatcher>title</fieldMatcher>
 * </handler>
 * }
 * <p>
 * For documents where multiple title fields are found, the above only
 * keeps the first title value captured.
 * </p>
 *
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public class ForceSingleValueTagger extends AbstractDocumentTagger {

    private final TextMatcher fieldMatcher = new TextMatcher();
    private String action;

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        for (Entry<String, List<String>> en :
                metadata.matchKeys(fieldMatcher).entrySet()) {
            String field = en.getKey();
            List<String> values = en.getValue();
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
                metadata.set(field, singleValue);
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
     * Gets action.
     * @return action action to be performed
     * @since 3.0.0
     */
    public String getAction() {
        return action;
    }
    /**
     * Sets the action. One of: keepFirst, keepLast, or mergeWith:&lt;sep&gt;.
     * @param action action to be performed
     * @since 3.0.0
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Deprecated.
     * @return empty map
     * @deprecated Since 3.0.0, use {@link #getFieldMatcher()}.
     */
    @Deprecated
    public Map<String, String> getSingleValueFields() {
        return new HashMap<>();
    }
    /**
     * Add a field to be made single value.
     * @param field field
     * @param action action
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)} and
     *             {@link #setAction(String)}.
     */
    @Deprecated
    public void addSingleValueField(String field, String action) {
        fieldMatcher.setPattern(field);
        setAction(action);
    }
    /**
     * Does nothing.
     * @param name field name
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)}.
     */
    @Deprecated
    public void removeSingleValueField(String name) {
        //NOOP
    }


    @Override
    protected void loadHandlerFromXML(XML xml) {
        xml.checkDeprecated("singleValue", "fieldMatcher", true);
        setAction(xml.getString("@action", action));
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("action", action);
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
