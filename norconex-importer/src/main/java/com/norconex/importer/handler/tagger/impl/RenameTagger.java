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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Rename metadata fields to different names.
 * </p>
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 * <p>
 * When using regular expressions, "toField" can also hold replacement patterns
 * (e.g. $1, $2, etc).
 * </p>
 *
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.RenameTagger">
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <!-- multiple rename tags allowed -->
 *   <rename
 *       toField="(to field)"
 *       {@nx.include com.norconex.commons.lang.map.PropertySetter#attributes}>
 *     <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#attributes}>
 *       (one or more matching fields to rename)
 *     </fieldMatcher>
 *   </rename>
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.tagger.impl.RenameTagger">
 *   <rename toField="title" onSet="replace">
 *     <fieldMatcher>dc.title</fieldMatcher>
 *   </rename>
 * </handler>
 * }
 * <p>
 * The above example renames a "dc.title" field to "title", overwriting
 * any existing values in "title".
 * </p>
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public class RenameTagger extends AbstractDocumentTagger {

    private final List<RenameDetails> renames = new ArrayList<>();

    @Override
    public void tagApplicableDocument(
            HandlerDoc doc, InputStream document, ParseState parseState)
                    throws ImporterHandlerException {
        for (RenameDetails ren : renames) {
            for (Entry<String, List<String>> en :
                    doc.getMetadata().matchKeys(ren.fieldMatcher).entrySet()) {
                String field = en.getKey();
                List<String> values = en.getValue();
                String newField = ren.fieldMatcher.replace(field, ren.toField);
                if (!Objects.equals(field, newField)) {
                    doc.getMetadata().remove(field);
                    PropertySetter.orDefault(ren.onSet).apply(
                            doc.getMetadata(), newField, values);
                }
            }
        }
    }

    public void addRename(
            TextMatcher fieldMatcher, String toField, PropertySetter onSet) {
        renames.add(new RenameDetails(fieldMatcher, toField, onSet));
    }
    @Deprecated
    public void addRename(String fromField, String toField, boolean overwrite) {
        addRename(fromField, toField, overwrite, false);
    }
    @Deprecated
    public void addRename(String fromField, String toField,
            boolean overwrite, boolean regex) {
        addRename(new TextMatcher(
                fromField).setMethod(regex ? Method.REGEX : Method.BASIC),
                toField,
                overwrite ? PropertySetter.REPLACE : PropertySetter.APPEND);
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("rename");
        for (XML node : nodes) {
            node.checkDeprecated("@overwrite", "onSet", true);
            node.checkDeprecated("@fromField", "fieldMatcher", true);
            TextMatcher fieldMatcher = new TextMatcher();
            fieldMatcher.loadFromXML(node.getXML("fieldMatcher"));
            addRename(fieldMatcher,
                      node.getString("@toField", null),
                      PropertySetter.fromXML(node, null));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (RenameDetails details : renames) {
            XML node = xml.addElement("rename")
                    .setAttribute("toField", details.toField);
            PropertySetter.toXML(node, details.onSet);
            details.fieldMatcher.saveToXML(node.addElement("fieldMatcher"));
        }
    }

    public static class RenameDetails {
        private final TextMatcher fieldMatcher = new TextMatcher();
        private final String toField;
        private PropertySetter onSet;
        public RenameDetails(TextMatcher fieldMatcher, String toField,
                PropertySetter onSet) {
            super();
            this.fieldMatcher.copyFrom(fieldMatcher);
            this.toField = toField;
            this.onSet = onSet;
        }
        @Deprecated
        public RenameDetails(
                String fromField, String toField, boolean overwrite) {
            this(fromField, toField, overwrite, false);
        }
        @Deprecated
        public RenameDetails(String fromField, String toField,
                boolean overwrite, boolean regex) {
            this(new TextMatcher(
                    fromField).setMethod(regex ? Method.REGEX : Method.BASIC),
                    toField,
                    overwrite ? PropertySetter.REPLACE : PropertySetter.APPEND);
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
