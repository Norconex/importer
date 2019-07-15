/* Copyright 2010-2019 Norconex Inc.
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Rename metadata fields to different names.  If the target name already
 * exists, the values of the original field name will be added, unless
 * "overwrite" is set to <code>true</code>.
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.RenameTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;rename regex="[false|true]" fromField="(from field)"
 *                 toField="(to field)" overwrite="[false|true]" /&gt;
 *      &lt;!-- multiple rename tags allowed --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following example renames a "dc.title" field to "title", overwriting
 * any existing values in "title".
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.RenameTagger"&gt;
 *      &lt;rename fromField="dc.title" toField="title" overwrite="true" /&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class RenameTagger extends AbstractDocumentTagger {

    private final Map<String, RenameDetails> renames = new ListOrderedMap<>();

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {
        for (RenameDetails details : renames.values()) {
            if (details.regex) {
                doRegexRename(details, metadata);
            } else {
                doRegularRename(details, metadata);
            }
        }
    }

    private void doRegularRename(RenameDetails details, ImporterMetadata metadata) {
        List<String> fromValues = metadata.get(details.fromField);
        List<String> toValues = metadata.get(details.toField);
        if (details.overwrite || toValues == null) {
            toValues = fromValues;
        } else if (fromValues != null) {
            fromValues.removeAll(toValues);
            toValues.addAll(fromValues);
        }
        metadata.put(details.toField, toValues);
        metadata.remove(details.fromField);
    }

    private void doRegexRename(RenameDetails details, ImporterMetadata metadata) {
        Pattern p = Pattern.compile(details.fromField);
        String[] keys = metadata.keySet().toArray(
                ArrayUtils.EMPTY_STRING_ARRAY);
        for (String key : keys) {
            Matcher m = p.matcher(key);
            if (!m.matches()) {
                continue;
            }
            String fromField = key;
            String toField = m.replaceFirst(details.toField);
            doRegularRename(new RenameDetails(
                    fromField, toField, details.overwrite), metadata);
        }
    }

    public void addRename(String fromField, String toField, boolean overwrite) {
        addRename(fromField, toField, overwrite, false);
    }

    public void addRename(String fromField, String toField,
            boolean overwrite, boolean regex) {
        if (StringUtils.isNotBlank(fromField)
                && StringUtils.isNotBlank(toField)) {
            renames.put(fromField,
                    new RenameDetails(fromField, toField, overwrite, regex));
        }
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("rename");
        for (XML node : nodes) {
            addRename(node.getString("@fromField", null),
                      node.getString("@toField", null),
                      node.getBoolean("@overwrite", false),
                      node.getBoolean("@regex", false));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (RenameDetails details : renames.values()) {
            xml.addElement("rename")
                    .setAttribute("fromField", details.fromField)
                    .setAttribute("toField", details.toField)
                    .setAttribute("overwrite", details.overwrite)
                    .setAttribute("regex", details.regex);
        }
    }

    public static class RenameDetails {
        private final String fromField;
        private final String toField;
        private final boolean overwrite;
        private final boolean regex;
        public RenameDetails(
                String fromField, String toField, boolean overwrite) {
            this(fromField, toField, overwrite, false);
        }
        public RenameDetails(String fromField, String toField,
                boolean overwrite, boolean regex) {
            super();
            this.fromField = fromField;
            this.toField = toField;
            this.overwrite = overwrite;
            this.regex = regex;
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
