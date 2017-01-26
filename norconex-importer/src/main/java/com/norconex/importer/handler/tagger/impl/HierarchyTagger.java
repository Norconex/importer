/* Copyright 2014-2017 Norconex Inc.
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
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
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
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.HierarchyTagger"&gt;
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
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * <h4>Usage example:</h4>
 * <p>
 * The following will expand a slash-separated vegetable hierarchy found in a 
 * "vegetable" field into a "vegetableHierarchy" field.
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.HierarchyTagger"&gt;
 *      &lt;hierarchy fromField="vegetable" toField="vegetableHierarchy" 
 *                 fromSeparator="/"/&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class HierarchyTagger extends AbstractDocumentTagger {

    private static class HierarchyDetails {
        private String fromField;
        private String toField;
        private String fromSeparator;
        private String toSeparator;
        private boolean overwrite;
        
        HierarchyDetails(String from, String to, 
                String fromSeparator, String toSeparator, boolean overwrite) {
            this.fromField = from;
            this.toField = to;
            this.fromSeparator = fromSeparator;
            this.toSeparator = toSeparator;
            this.overwrite = overwrite;
        }


        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(
                    this, ToStringStyle.SHORT_PREFIX_STYLE);
            builder.append("fromField", fromField);
            builder.append("toField", toField);
            builder.append("fromSeparator", fromSeparator);
            builder.append("toSeparator", toSeparator);
            builder.append("overwrite", overwrite);
            return builder.toString();
        }


        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof HierarchyDetails)) {
                return false;
            }
            HierarchyDetails castOther = (HierarchyDetails) other;
            return new EqualsBuilder().append(fromField, castOther.fromField)
                    .append(toField, castOther.toField)
                    .append(fromSeparator, castOther.fromSeparator)
                    .append(toSeparator, castOther.toSeparator)
                    .append(overwrite, castOther.overwrite).isEquals();
        }


        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(fromField).append(toField)
                    .append(fromSeparator).append(toSeparator)
                    .append(overwrite).toHashCode();
        }
        
    }
    
    private final List<HierarchyDetails> list = 
            new ArrayList<HierarchyDetails>();

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
        List<String> nodes = new ArrayList<String>();
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
            metadata.setString(field, nodesArray);
        } else {
            metadata.addString(field, nodesArray);
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
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        List<HierarchicalConfiguration> nodes =
                xml.configurationsAt("hierarchy");
        for (HierarchicalConfiguration node : nodes) {
            addHierarcyDetails(
                    node.getString("[@fromField]", null),
                    node.getString("[@toField]", null),
                    node.getString("[@fromSeparator]", null),
                    node.getString("[@toSeparator]", null),
                    node.getBoolean("[@overwrite]", false));
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (HierarchyDetails details : list) {
            writer.writeStartElement("hierarchy");
            if (StringUtils.isNotBlank(details.fromField)) {
                writer.writeAttribute("fromField", details.fromField);
            }
            if (StringUtils.isNotBlank(details.toField)) {
                writer.writeAttribute("toField", details.toField);
            }
            if (StringUtils.isNotBlank(details.fromSeparator)) {
                writer.writeAttribute(
                        "fromSeparator", details.fromSeparator);
            }
            if (StringUtils.isNotBlank(details.toSeparator)) {
                writer.writeAttribute("toSeparator", details.toSeparator);
            }
            writer.writeAttribute("overwrite", 
                    Boolean.toString(details.overwrite));
            writer.writeEndElement();
        }
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof HierarchyTagger)) {
            return false;
        }
        HierarchyTagger castOther = (HierarchyTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(list, castOther.list)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(list)
                .toHashCode();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
        builder.appendSuper(super.toString());
        builder.append("list", list);
        return builder.toString();
    }
}
