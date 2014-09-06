/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.IDocumentTagger;


/**
 * Splits an existing metadata value into multiple values based on a given
 * value separator.  The "toField" argument
 * is optional (the same field will be used to store the splits if no
 * "toField" is specified").
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.SplitTagger"&gt;
 *      &lt;split fromField="sourceFieldName" toField="targetFieldName" 
 *               regex="[false|true]"&gt
 *          &lt;separator&gt(separator value)&lt;/separator&gt
 *      &lt;/split&gt
 *      &lt;!-- multiple split tags allowed --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 1.3.0
 */
@SuppressWarnings("nls")
public class SplitTagger implements IDocumentTagger, IXMLConfigurable {

    private static final long serialVersionUID = -6062036871216739761L;
    
    private final List<Split> splits = new ArrayList<>();
    
    @Override
    public void tagDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        for (Split split : splits) {
            if (metadata.containsKey(split.getFromField())) {
                String[] metaValues = metadata.getStrings(split.getFromField())
                        .toArray(ArrayUtils.EMPTY_STRING_ARRAY);
                List<String> sameFieldValues = 
                        SetUniqueList.setUniqueList(new ArrayList<String>());
                for (int i = 0; i < metaValues.length; i++) {
                    String metaValue = metaValues[i];
                    String[] splitValues = null;
                    if (split.isRegex()) {
                        splitValues = regexSplit(
                                metaValue, split.getSeparator());
                    } else {
                        splitValues = regularSplit(
                                metaValue, split.getSeparator());
                    }
                    if (ArrayUtils.isNotEmpty(splitValues)) {
                        if (StringUtils.isNotBlank(split.getToField())) {
                            metadata.addString(split.getToField(), splitValues);
                        } else {
                            sameFieldValues.addAll(Arrays.asList(splitValues));
                        }
                    }
                }
                if (StringUtils.isBlank(split.getToField())) {
                    metadata.setString(split.getFromField(), 
                            sameFieldValues.toArray(
                                    ArrayUtils.EMPTY_STRING_ARRAY));
                }
            }
        }
    }
    

    private String[] regexSplit(String metaValue, String separator) {
        String[] values = metaValue.split(separator);
        List<String> cleanValues = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                cleanValues.add(value);
            }
        }
        return cleanValues.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }
    private String[] regularSplit(String metaValue, String separator) {
        return StringUtils.splitByWholeSeparator(metaValue, separator);
    }
        
    public List<Split> getSplits() {
        return Collections.unmodifiableList(splits);
    }

    public void removeSplit(String fromField) {
        List<Split> toRemove = new ArrayList<>();
        for (Split split : splits) {
            if (Objects.equals(split.getFromField(), fromField)) {
                toRemove.add(split);
            }
        }
        synchronized (splits) {
            splits.removeAll(toRemove);
        }
    }
    
    public void addSplit(
            String fromField, String separator, boolean regex) {
        splits.add(new Split(fromField, null, separator, regex));
    }
    public void addSplit(
            String fromField, String toField, String separator, boolean regex) {
        splits.add(new Split(fromField, toField, separator, regex));
    }

    
    public class Split implements Serializable {
        private static final long serialVersionUID = 9206061804991938873L;
        private final String fromField;
        private final String toField;
        private final String separator;
        private final boolean regex;
        public Split(String fromField, String toField,
                String separator, boolean regex) {
            super();
            this.fromField = fromField;
            this.toField = toField;
            this.separator = separator;
            this.regex = regex;
        }
        public String getFromField() {
            return fromField;
        }
        public String getToField() {
            return toField;
        }
        public String getSeparator() {
            return separator;
        }
        public boolean isRegex() {
            return regex;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((fromField == null) ? 0 : fromField.hashCode());
            result = prime * result + (regex ? 1231 : 1237);
            result = prime * result
                    + ((toField == null) ? 0 : toField.hashCode());
            result = prime * result
                    + ((separator == null) ? 0 : separator.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Split other = (Split) obj;
            if (fromField == null) {
                if (other.fromField != null) {
                    return false;
                }
            } else if (!fromField.equals(other.fromField)) {
                return false;
            }
            if (regex != other.regex) {
                return false;
            }
            if (toField == null) {
                if (other.toField != null) {
                    return false;
                }
            } else if (!toField.equals(other.toField)) {
                return false;
            }
            if (separator == null) {
                if (other.separator != null) {
                    return false;
                }
            } else if (!separator.equals(other.separator)) {
                return false;
            }
            return true;
        }
        @Override
        public String toString() {
            return "Split [fromField=" + fromField
                    + ", toField=" + toField + ", separator=" + separator
                    + ", regex=" + regex + "]";
        }
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
            List<HierarchicalConfiguration> nodes = 
                    xml.configurationsAt("split");
            for (HierarchicalConfiguration node : nodes) {
                addSplit(
                        node.getString("[@fromField]"),
                        node.getString("[@toField]", null),
                        node.getString("separator"),
                        node.getBoolean("[@regex]", false));
            }
        } catch (ConfigurationException e) {
            throw new IOException("Cannot load XML.", e);
        }
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("tagger");
            writer.writeAttribute("class", getClass().getCanonicalName());

            for (Split split : splits) {
                writer.writeStartElement("split");
                writer.writeAttribute("fromField", split.getFromField());
                if (split.getToField() != null) {
                    writer.writeAttribute("toField", split.getToField());
                }
                writer.writeAttribute("regex", 
                        Boolean.toString(split.isRegex()));
                writer.writeStartElement("separator");
                writer.writeCharacters(split.getSeparator());
                writer.writeEndElement();
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
    public String toString() {
        return "SplitTagger [splits=" + splits + "]";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((splits == null) ? 0 : splits.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SplitTagger other = (SplitTagger) obj;
        if (splits == null) {
            if (other.splits != null) {
                return false;
            }
        } else if (!splits.equals(other.splits)) {
            return false;
        }
        return true;
    }
}
