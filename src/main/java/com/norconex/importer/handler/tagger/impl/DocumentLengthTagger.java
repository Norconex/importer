/* Copyright 2015-2020 Norconex Inc.
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Adds the document length (i.e., number of bytes) to
 * the specified <code>field</code>. The length is the document
 * content length as it is in its current processing stage. If for
 * instance you set this tagger after a transformer that modifies the content,
 * the obtained length will be for the modified content, and not the
 * original length. To obtain a document's length before any modification
 * was made to it, use this tagger as one of the first
 * handler in your pre-parse handlers.</p>
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
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.DocumentLengthTagger"
 *     toField="(mandatory target field)"
 *     {@nx.include com.norconex.commons.lang.map.PropertySetter#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.tagger.impl.DocumentLengthTagger"
 *     toField="docSize" />
 * }
 *
 * <p>
 * The following stores the document lenght into a "docSize" field.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.2.0
 */
@SuppressWarnings("javadoc")
public class DocumentLengthTagger extends AbstractDocumentTagger {

    private String toField;
    private PropertySetter onSet;

    @Override
    public void tagApplicableDocument(
            HandlerDoc doc, InputStream document, ParseState parseState)
                    throws ImporterHandlerException {
        if (StringUtils.isBlank(toField)) {
            throw new IllegalArgumentException("\"toField\" cannot be empty.");
        }

        int length = -1;
        if (document instanceof CachedInputStream) {
            length = ((CachedInputStream) document).length();
        } else {
            CountingInputStream is = new CountingInputStream(document);
            try {
                IOUtils.copy(is, new NullOutputStream());
            } catch (IOException e) {
                throw new ImporterHandlerException(e);
            }
            length = is.getCount();
        }

        PropertySetter.orAppend(onSet).apply(
                doc.getMetadata(), toField, length);
    }

    /**
     * Gets the target field.
     * @return target field
     * @since 3.0.0
     */
    public String getToField() {
        return toField;
    }
    /**
     * Sets the target field.
     * @param toField target field
     * @since 3.0.0
     */
    public void setToField(String toField) {
        this.toField = toField;
    }

    /**
     * Gets the target field.
     * @return target field
     * @deprecated Since 3.0.0, use {@link #getToField()}
     */
    @Deprecated
    public String getField() {
        return getToField();
    }
    /**
     * Sets the target field.
     * @param toField target field
     * @deprecated Since 3.0.0, use {@link #setToField(String)}
     */
    @Deprecated
    public void setField(String toField) {
        setToField(toField);
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
        this.onSet = overwrite ? PropertySetter.REPLACE : PropertySetter.APPEND;
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
        xml.checkDeprecated("@field", "toField", true);
        toField = xml.getString("@toField", toField);
        setOnSet(PropertySetter.fromXML(xml, onSet));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("toField", toField);
        PropertySetter.toXML(xml, getOnSet());
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
