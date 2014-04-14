/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.tagger.IDocumentTagger;

/**
 * Given a separator, split a field string into multiple segments 
 * representing each node of a hierarchical branch. This is useful
 * when mixed when faceting, to find out how much documents fall under each 
 * node of a hierarchy. For example, take this hierarchical string:
 * <pre>
 *   /vegetable/potato/sweet
 * </pre>
 * We specify a slash (/) separator and it will produce the folowing entries 
 * in the specified document metadata field:
 * 
 * <pre>
 *   /vegetable
 *   /vegetable/potato
 *   /vegetable/potato/sweet
 * </pre>
 * <p/>
 * Can be used both as a pre-parse or post-parse handler.
 * <p/>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.tagger.impl.HierarchycalTagger"&gt;
 *      &lt;hierarchy fromField="(from field)" toField="(to field)" 
 *                 fromSeparator="(original separator)" toSeparator="(new separator)"
 *                 overwrite="[false|true]" /&gt
 *      &lt;-- multiple hierarchy tags allowed --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class HierarchyTagger implements IDocumentTagger, IXMLConfigurable {

    private static final long serialVersionUID = 3040227993725399299L;

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
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((fromField == null) ? 0 : fromField.hashCode());
            result = prime * result
                    + ((fromSeparator == null) ? 0 : fromSeparator.hashCode());
            result = prime * result + (overwrite ? 1231 : 1237);
            result = prime * result
                    + ((toField == null) ? 0 : toField.hashCode());
            result = prime * result
                    + ((toSeparator == null) ? 0 : toSeparator.hashCode());
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
            HierarchyDetails other = (HierarchyDetails) obj;
            if (fromField == null) {
                if (other.fromField != null)
                    return false;
            } else if (!fromField.equals(other.fromField))
                return false;
            if (fromSeparator == null) {
                if (other.fromSeparator != null)
                    return false;
            } else if (!fromSeparator.equals(other.fromSeparator))
                return false;
            if (overwrite != other.overwrite)
                return false;
            if (toField == null) {
                if (other.toField != null)
                    return false;
            } else if (!toField.equals(other.toField))
                return false;
            if (toSeparator == null) {
                if (other.toSeparator != null)
                    return false;
            } else if (!toSeparator.equals(other.toSeparator))
                return false;
            return true;
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
        
    }
    
    private final List<HierarchyDetails> list = new ArrayList<HierarchyDetails>();

    @Override
    public void tagDocument(String reference, InputStream document,
            Properties metadata, boolean parsed) throws IOException {
        for (HierarchyDetails details : list) {
            breakSegments(metadata, details);
        }
    }

    private void breakSegments(Properties metadata, HierarchyDetails details) {
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
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
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
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("tagger");
            writer.writeAttribute("class", getClass().getCanonicalName());
            
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
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
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
        HierarchyTagger other = (HierarchyTagger) obj;
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
