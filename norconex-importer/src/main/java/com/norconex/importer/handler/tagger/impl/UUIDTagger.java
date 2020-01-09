/* Copyright 2017-2020 Norconex Inc.
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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Generates a random Universally unique identifier (UUID) and stores it
 * in the specified <code>field</code>.
 * If no <code>field</code> is provided, the UUID will be added to
 * <code>document.uuid</code>.
 * </p>
 *
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 *
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.UUIDTagger"
 *      field="(target field)"
 *      onSet="[append|prepend|replace|optional]" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <p>
 * The following generates a UUID and stores it in a "uuid" field, overwriting
 * any existing values under that field.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.UUIDTagger"
 *      field="uuid" onSet="replace" /&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.7.0
 */
public class UUIDTagger extends AbstractDocumentTagger {

    public static final String DEFAULT_FIELD = "document.uuid";

    private String field = DEFAULT_FIELD;
    private PropertySetter onSet;

    /**
     * Constructor.
     */
    public UUIDTagger() {
        super();
    }

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        String uuid = UUID.randomUUID().toString();

        String finalField = field;
        if (StringUtils.isBlank(finalField)) {
            finalField = DEFAULT_FIELD;
        }
        PropertySetter.orDefault(onSet).apply(metadata, finalField, uuid);
    }

    public String getField() {
        return field;
    }
    public void setField(String toField) {
        this.field = toField;
    }

    /**
     * Gets whether existing value for the same field should be overwritten.
     * @return <code>true</code> if overwriting existing value.
     * @deprecated Since 3.0.0 use {@link #getOnSet()}.
     */
    @Deprecated
    public boolean isOverwrite() {
        return PropertySetter.REPLACE == onSet;
    }
    /**
     * Sets whether existing value for the same field should be overwritten.
     * @param overwrite <code>true</code> if overwriting existing value.
     * @deprecated Since 3.0.0 use {@link #setOnSet(PropertySetter)}.
     */
    @Deprecated
    public void setOverwrite(boolean overwrite) {
        this.onSet = overwrite
                ? PropertySetter.REPLACE : PropertySetter.APPEND;
    }
    /**
     * Gets the property setter to use when a value is set.
     * @return property setter
     * @since 3.0.0
     */
    public PropertySetter getOnSet() {
        return onSet;
    }
    /**
     * Sets the property setter to use when a value is set.
     * @param onSet property setter
     * @since 3.0.0
     */
    public void setOnSet(PropertySetter onSet) {
        this.onSet = onSet;
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        xml.checkDeprecated("@overwrite", "onSet", true);
        field = xml.getString("@field", field);
        setOnSet(PropertySetter.fromXML(xml, onSet));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("field", field);
        PropertySetter.toXML(xml, onSet);
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
