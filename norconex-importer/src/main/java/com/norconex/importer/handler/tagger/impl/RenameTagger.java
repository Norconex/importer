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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 *      &lt;rename fromField="(from field)" toField="(to field)" overwrite="[false|true]" /&gt;
 *      &lt;-- multiple rename tags allowed --&gt;
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

    private final Map<String, RenameDetails> renames =
            new HashMap<>();

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        for (Entry<String, RenameDetails> entry : renames.entrySet()) {
            String from = entry.getKey();
            RenameDetails details = entry.getValue();
            List<String> fromValues = metadata.get(from);
            List<String> toValues = metadata.get(details.toField);
            if (details.overwrite || toValues == null) {
                toValues = fromValues;
            } else if (fromValues != null) {
                fromValues.removeAll(toValues);
                toValues.addAll(fromValues);
            }
            metadata.put(details.toField, toValues);
            metadata.remove(from);
        }
    }

    public void addRename(String fromField, String toField, boolean overwrite) {
        if (StringUtils.isNotBlank(fromField)
                && StringUtils.isNotBlank(toField)) {
            renames.put(fromField,
                    new RenameDetails(fromField, toField, overwrite));
        }
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("rename");
        for (XML node : nodes) {
            addRename(node.getString("@fromField", null),
                      node.getString("@toField", null),
                      node.getBoolean("@overwrite", false));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (RenameDetails details : renames.values()) {
            xml.addElement("rename")
                    .setAttribute("fromField", details.fromField)
                    .setAttribute("toField", details.toField)
                    .setAttribute("overwrite", details.overwrite);
        }
    }

    public static class RenameDetails {
        private final String fromField;
        private final String toField;
        private final boolean overwrite;
        public RenameDetails(
                String fromField, String toField, boolean overwrite) {
            super();
            this.fromField = fromField;
            this.toField = toField;
            this.overwrite = overwrite;
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
