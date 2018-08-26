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
 * <p>Given a separator, split a field string into multiple segments
 * representing each node of a hierarchical branch. This is useful
 * when faceting, to find out how many documents fall under each
 * node of a hierarchy. For example, take this hierarchical string:</p>
 * <pre>
 *   /vegetable/potato/sweet
 * </pre>
 * <p>We specify a slash (/) separator and it will produce the folowing entries
 * in the specified document metadata field:</p>
 *
 * <pre>
 *   /vegetable
 *   /vegetable/potato
 *   /vegetable/potato/sweet
 * </pre>
 * <p>
 * If no target field is specified (<code>toField</code>) the
 * source field (<code>fromField</code>) will be used to store the resulting
 * values. The same applies to the source and target hierarchy separators
 * (<code>fromSeparator</code> and <code>toSeparator</code>).
 * </p>
 * <p>
 * Can be used both as a pre-parse or post-parse handler.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.HierarchyTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;hierarchy fromField="(from field)"
 *              toField="(optional to field)"
 *              fromSeparator="(original separator)"
 *              toSeparator="(optional new separator)"
 *              overwrite="[false|true]" /&gt;
 *      &lt;!-- multiple hierarchy tags allowed --&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <p>
 * The following will expand a slash-separated vegetable hierarchy found in a
 * "vegetable" field into a "vegetableHierarchy" field.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.HierarchyTagger"&gt;
 *      &lt;hierarchy fromField="vegetable" toField="vegetableHierarchy"
 *                 fromSeparator="/"/&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class HierarchyTagger extends AbstractDocumentTagger {

    private static class HierarchyDetails {
        private final String fromField;
        private final String toField;
        private final String fromSeparator;
        private final String toSeparator;
        private final boolean overwrite;

        HierarchyDetails(String from, String to,
                String fromSeparator, String toSeparator, boolean overwrite) {
            this.fromField = from;
            this.toField = to;
            this.fromSeparator = fromSeparator;
            this.toSeparator = toSeparator;
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

    private final List<HierarchyDetails> list = new ArrayList<>();

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {
        for (HierarchyDetails details : list) {
            breakSegments(metadata, details);
        }
    }

    private void breakSegments(
            ImporterMetadata metadata, HierarchyDetails details) {
        List<String> nodes = new ArrayList<>();
        String sep = details.fromSeparator;
        if (StringUtils.isNotEmpty(details.toSeparator)) {
            sep = details.toSeparator;
        }
        for (String value : metadata.getStrings(details.fromField)) {
            String[] segs = StringUtils.splitByWholeSeparatorPreserveAllTokens(
                    value, details.fromSeparator);
            StringBuilder b = new StringBuilder();
            for (String seg : segs) {
                if (seg.equals(details.fromSeparator)) {
                    b.append(sep);
                } else {
                    b.append(seg);
                }
                nodes.add(b.toString());
            }
        }
        String field = details.fromField;
        if (StringUtils.isNotBlank(details.toField)) {
            field = details.toField;
        }
        String[] nodesArray = nodes.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        if (details.overwrite) {
            metadata.set(field, nodesArray);
        } else {
            metadata.add(field, nodesArray);
        }
    }

    /**
     * Adds hierarchy instructions.
     * @param fromField source field name
     * @param toField target optional target field name
     * @param fromSeparator source separator
     * @param toSeparator optional target separator
     * @param overwrite whether to overwrite target field if it exists
     */
    public void addHierarcyDetails(
            String fromField, String toField,
            String fromSeparator, String toSeparator, boolean overwrite) {
        if (StringUtils.isAnyBlank(fromField, fromSeparator)) {
            return;
        }
        list.add(new HierarchyDetails(fromField, toField,
                fromSeparator, toSeparator, overwrite));
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("hierarchy");
        for (XML node : nodes) {
            addHierarcyDetails(
                    node.getString("@fromField", null),
                    node.getString("@toField", null),
                    node.getString("@fromSeparator", null),
                    node.getString("@toSeparator", null),
                    node.getBoolean("@overwrite", false));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (HierarchyDetails details : list) {
            xml.addElement("hierarchy")
                    .setAttribute("fromField", details.fromField)
                    .setAttribute("toField", details.toField)
                    .setAttribute("fromSeparator", details.fromSeparator)
                    .setAttribute("toSeparator", details.toSeparator)
                    .setAttribute("overwrite", details.overwrite);
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
