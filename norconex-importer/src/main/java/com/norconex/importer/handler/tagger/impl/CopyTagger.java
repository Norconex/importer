/* Copyright 2014 Norconex Inc.
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * <p>Copies metadata fields. If a target field already
 * exists, the values of the original field name will be <i>added</i>, unless
 * "overwrite" is set to <code>true</code>.</p>
 * 
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * <p>XML configuration usage:</p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.CopyTagger"&gt;
 *      &lt;copy fromField="(from field)" toField="(to field)" overwrite="[false|true]" /&gt;
 *      &lt;-- multiple copy tags allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Dimassimo
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class CopyTagger extends AbstractDocumentTagger {

    private static class CopyDetails {
        private String fromField;
        private String toField;
        private boolean overwrite;
        
        CopyDetails(String from, String to, boolean overwrite) {
            this.fromField = from;
            this.toField = to;
            this.overwrite = overwrite;
        }
        
        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(
                    this, ToStringStyle.SHORT_PREFIX_STYLE);
            builder.append("fromField", fromField);
            builder.append("toField", toField);
            builder.append("overwrite", overwrite);
            return builder.toString();
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof CopyDetails)) {
                return false;
            }
            CopyDetails castOther = (CopyDetails) other;
            return new EqualsBuilder().append(fromField, castOther.fromField)
                    .append(toField, castOther.toField)
                    .append(overwrite, castOther.overwrite).isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(fromField).append(toField)
                    .append(overwrite).toHashCode();
        }
        
    }
    
    
    private final List<CopyDetails> list = new ArrayList<CopyDetails>();

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException {

        for (CopyDetails details : list) {
            for (String value : metadata.getStrings(details.fromField)) {
                if (details.overwrite) {
                    metadata.setString(details.toField, value);
                } else {
                    metadata.addString(details.toField, value);
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
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        List<HierarchicalConfiguration> nodes = xml.configurationsAt("copy");
        if (!nodes.isEmpty()) {
            list.clear();
        }
        for (HierarchicalConfiguration node : nodes) {
            addCopyDetails(node.getString("[@fromField]", null),
                      node.getString("[@toField]", null),
                      node.getBoolean("[@overwrite]", false));
        }
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (CopyDetails details : list) {
            writer.writeStartElement("copy");
            writer.writeAttribute("fromField", details.fromField);
            writer.writeAttribute("toField", details.toField);
            writer.writeAttribute("overwrite", 
                    Boolean.toString(details.overwrite));
            writer.writeEndElement();
        }
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((list == null) ? 0 : list.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CopyTagger other = (CopyTagger) obj;
        if (list == null) {
            if (other.list != null)
                return false;
        } else if (!list.equals(other.list))
            return false;
        return true;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
        builder.append("list", list);
        return builder.toString();
    }
}
