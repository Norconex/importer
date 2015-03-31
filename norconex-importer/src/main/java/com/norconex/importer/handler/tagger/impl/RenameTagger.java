/* Copyright 2010-2014 Norconex Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Rename metadata fields to different names.  If the target name already
 * exists, the values of the original field name will be added, unless
 * "overwrite" is set to <code>true</code>. 
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.RenameTagger"&gt;
 *      &lt;rename fromField="(from field)" toField="(to field)" overwrite="[false|true]" /&gt;
 *      &lt;-- multiple rename tags allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class RenameTagger extends AbstractDocumentTagger {

    private final Map<String, RenameDetails> renames = 
            new HashMap<String, RenameDetails>();
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document, 
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        for (String from : renames.keySet()) {
            RenameDetails details = renames.get(from);
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
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        List<HierarchicalConfiguration> nodes =
                xml.configurationsAt("rename");
        for (HierarchicalConfiguration node : nodes) {
            addRename(node.getString("[@fromField]", null),
                      node.getString("[@toField]", null),
                      node.getBoolean("[@overwrite]", false));
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (String fromField : renames.keySet()) {
            RenameDetails details = renames.get(fromField);
            writer.writeStartElement("rename");
            writer.writeAttribute("fromField", details.fromField);
            writer.writeAttribute("toField", details.toField);
            writer.writeAttribute(
                    "overwrite", Boolean.toString(details.overwrite));
            writer.writeEndElement();
        }
    }
    
    public static class RenameDetails {
        private String fromField;
        private String toField;
        private boolean overwrite;
        public RenameDetails(
                String fromField, String toField, boolean overwrite) {
            super();
            this.fromField = fromField;
            this.toField = toField;
            this.overwrite = overwrite;
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(fromField)
                .append(toField)
                .append(overwrite)
                .toHashCode();
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof RenameTagger.RenameDetails)) {
                return false;
            }
            RenameTagger.RenameDetails other = (RenameTagger.RenameDetails) obj;
            return new EqualsBuilder()
                .append(fromField, other.fromField)
                .append(toField, other.toField)
                .append(overwrite, other.overwrite)
                .isEquals();
        }
        @Override
        public String toString() {
            return "RenameDetails [fromField=" + fromField + ", toField="
                    + toField + ", overwrite=" + overwrite + "]";
        }
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(renames)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RenameTagger)) {
            return false;
        }
        RenameTagger other = (RenameTagger) obj;
        return new EqualsBuilder()
            .append(renames, other.renames)
            .isEquals();
    }

    @Override
    public String toString() {
        return "RemapTagger [renames=" + renames + "]";
    }
}
