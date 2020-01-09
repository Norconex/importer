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
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

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
 * <h3>XML configuration usage:</h3>
 * <pre>{@code
 * <handler class="com.norconex.importer.handler.tagger.impl.DocumentLengthTagger"
 *     field="(mandatory target field)"
 *     onSet="[append|prepend|replace|optional]">
 *
 *     <restrictTo caseSensitive="[false|true]"
 *             field="(name of header/metadata field name to match)">
 *         (regular expression of value to match)
 *     </restrictTo>
 *     <!-- multiple "restrictTo" tags allowed (only one needs to match) -->
 * </handler>
 * }</pre>
 *
 * <h4>Usage example:</h4>
 * <p>
 * The following stores the document lenght into a "docSize" field.
 * </p>
 *
 * <pre>{@code
 * <handler class="com.norconex.importer.handler.tagger.impl.DocumentLengthTagger"
 *     field="docSize" />
 * }</pre>
 * @author Pascal Essiembre
 * @since 2.2.0
 */
public class DocumentLengthTagger extends AbstractDocumentTagger {

    private String field;
    private PropertySetter onSet;

    @Override
    protected void tagApplicableDocument(String reference,
            InputStream document, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        if (StringUtils.isBlank(field)) {
            throw new IllegalArgumentException("\"field\" cannot be empty.");
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

        PropertySetter.orDefault(onSet).apply(metadata, field, length);
    }

    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
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
        field = xml.getString("@field", field);
        setOnSet(PropertySetter.fromXML(xml, onSet));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("field", field);
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
