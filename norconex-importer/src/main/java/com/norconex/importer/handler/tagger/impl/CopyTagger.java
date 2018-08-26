/* Copyright 2014-2018 Norconex Inc.
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

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Copies metadata fields. If a target field already
 * exists, the values of the original field name will be <i>added</i>, unless
 * "overwrite" is set to <code>true</code>.</p>
 *
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CopyTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;copy fromField="(from field)" toField="(to field)" overwrite="[false|true]" /&gt;
 *      &lt;-- multiple copy tags allowed --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * Copies the value of a "creator" and "publisher" fields into an "author"
 * field, adding to any existing values in the "author" field.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CopyTagger"&gt;
 *      &lt;copy fromField="creator"   toField="author" overwrite="true" /&gt;
 *      &lt;copy fromField="publisher" toField="author" overwrite="true" /&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Dimassimo
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class CopyTagger extends AbstractDocumentTagger {

    private static class CopyDetails {
        private final String fromField;
        private final String toField;
        private final boolean overwrite;

        CopyDetails(String from, String to, boolean overwrite) {
            this.fromField = from;
            this.toField = to;
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


    private final List<CopyDetails> list = new ArrayList<>();

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {

        for (CopyDetails details : list) {
            for (String value : metadata.getStrings(details.fromField)) {
                if (details.overwrite) {
                    metadata.set(details.toField, value);
                } else {
                    metadata.add(details.toField, value);
                }
            }
        }
    }

    /**
     * Adds copy instructions.
     * @param fromField source field name
     * @param toField target field name
     * @param overwrite whether toField overwrite target field if it exists
     */
    public void addCopyDetails(
            String fromField, String toField, boolean overwrite) {
        if (StringUtils.isBlank(fromField)) {
            throw new IllegalArgumentException(
                    "'fromField' argument cannot be blank.");
        }
        if (StringUtils.isBlank(toField)) {
            throw new IllegalArgumentException(
                    "'toField' argument cannot be blank.");
        }
        list.add(new CopyDetails(fromField, toField, overwrite));
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("copy");
        if (!nodes.isEmpty()) {
            list.clear();
        }
        for (XML node : nodes) {
            addCopyDetails(node.getString("@fromField", null),
                      node.getString("@toField", null),
                      node.getBoolean("@overwrite", false));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (CopyDetails details : list) {
            xml.addElement("copy")
                    .setAttribute("fromField", details.fromField)
                    .setAttribute("toField", details.toField)
                    .setAttribute("overwrite",details.overwrite);
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
