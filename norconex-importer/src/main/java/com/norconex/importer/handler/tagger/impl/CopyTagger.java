/* Copyright 2014-2020 Norconex Inc.
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
import java.util.List;

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
 * <p>Copies metadata fields.</p>
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
 *
 * <h3>XML configuration usage:</h3>
 * <pre>{@code
 * <handler class="com.norconex.importer.handler.tagger.impl.CopyTagger">
 *
 *     <restrictTo caseSensitive="[false|true]"
 *             field="(name of header/metadata field name to match)">
 *         (regular expression of value to match)
 *     </restrictTo>
 *     <!-- multiple "restrictTo" tags allowed (only one needs to match) -->
 *
 *     <copy fromField="(from field)" toField="(to field)"
 *             onSet="[append|prepend|replace|optional]" />
 *     <-- multiple copy tags allowed -->
 * </handler>
 * }</pre>
 * <h4>Usage example:</h4>
 * <p>
 * Copies the value of a "creator" and "publisher" fields into an "author"
 * field, adding to any existing values in the "author" field.
 * </p>
 *
 * <pre>{@code
 * <handler class="com.norconex.importer.handler.tagger.impl.CopyTagger">
 *     <copy fromField="creator"   toField="author" onSet="replace" />
 *     <copy fromField="publisher" toField="author" onSet="replace" />
 * </handler>
 * }</pre>
 *
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class CopyTagger extends AbstractDocumentTagger {

    private final List<CopyDetails> copyDetailsList = new ArrayList<>();

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {
        for (CopyDetails copy : copyDetailsList) {
            PropertySetter.orDefault(copy.onSet).apply(metadata,
                    copy.toField, metadata.getStrings(copy.fromField));
        }
    }

    /**
     * Adds copy instructions, adding to any existing values on the target
     * field.
     * @param fromField source field name
     * @param toField target field name
     */
    public void addCopyDetails(String fromField, String toField) {
        addCopyDetails(fromField, toField, null);
    }
    /**
     * Adds copy instructions.
     * @param fromField source field name
     * @param toField target field name
     * @param onSet strategy to use when a value is copied over an existing one
     */
    public void addCopyDetails(
            String fromField, String toField, PropertySetter onSet) {
        if (StringUtils.isBlank(fromField)) {
            throw new IllegalArgumentException(
                    "'fromField' argument cannot be blank.");
        }
        if (StringUtils.isBlank(toField)) {
            throw new IllegalArgumentException(
                    "'toField' argument cannot be blank.");
        }
        copyDetailsList.add(new CopyDetails(fromField, toField, onSet));
    }
    /**
     * Adds copy instructions.
     * @param fromField source field name
     * @param toField target field name
     * @param overwrite whether toField overwrite target field if it exists
     * @deprecated Since 3.0.0, use 
     *             {@link #addCopyDetails(String, String, PropertySetter)}.
     */
    @Deprecated
    public void addCopyDetails(
            String fromField, String toField, boolean overwrite) {
        addCopyDetails(fromField, toField, overwrite 
                ? PropertySetter.REPLACE : PropertySetter.APPEND);
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("copy");
        if (!nodes.isEmpty()) {
            copyDetailsList.clear();
        }
        for (XML node : nodes) {
            node.checkDeprecated("@overwrite", "onSet", true);
            addCopyDetails(node.getString("@fromField", null),
                      node.getString("@toField", null),
                      PropertySetter.fromXML(node, null));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (CopyDetails details : copyDetailsList) {
            XML node = xml.addElement("copy")
                    .setAttribute("fromField", details.fromField)
                    .setAttribute("toField", details.toField);
            PropertySetter.toXML(node, details.onSet);
        }
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

    private static class CopyDetails {
        private final String fromField;
        private final String toField;
        private final PropertySetter onSet;

        CopyDetails(String from, String to, PropertySetter onSet) {
            this.fromField = from;
            this.toField = to;
            this.onSet = onSet;
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
}
