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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;


/**
 * Replaces an existing metadata value with another one.  The "toField" argument
 * is optional (the same field will be used for the replacement if no
 * "toField" is specified").
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.ReplaceTagger"&gt;
 *      &lt;replace fromField="sourceFieldName" toField="targetFieldName" 
 *               regex="[false|true]"&gt
 *          &lt;fromValue&gtSource Value&lt;/fromValue&gt
 *          &lt;toValue&gtTarget Value&lt;/toValue&gt
 *      &lt;/replace&gt
 *      &lt;!-- multiple replace tags allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]" &gt;
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 *
 */
@SuppressWarnings("nls")
public class ReplaceTagger extends AbstractDocumentTagger {

    private static final long serialVersionUID = -6062036871216739761L;
    
    private final List<Replacement> replacements = new ArrayList<>();
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        for (Replacement repl : replacements) {
            if (metadata.containsKey(repl.getFromField())) {
                String[] metaValues = metadata.getStrings(repl.getFromField())
                        .toArray(ArrayUtils.EMPTY_STRING_ARRAY);
                for (int i = 0; i < metaValues.length; i++) {
                    String metaValue = metaValues[i];
                    String newValue = null;
                    if (repl.isRegex()) {
                        newValue = regexReplace(metaValue, 
                                repl.getFromValue(), repl.getToValue());
                    } else {
                        newValue = regularReplace(metaValue, 
                                repl.getFromValue(), repl.getToValue());
                    }
                    if (!Objects.equals(metaValue, newValue)) {
                        if (StringUtils.isNotBlank(repl.getToField())) {
                            metadata.addString(repl.getToField(), newValue);
                        } else {
                            metaValues[i] = newValue;
                            metadata.setString(repl.getFromField(), metaValues);
                        }
                    }
                }
            }
        }
    }
    

    private String regexReplace(
            String metaValue, String fromValue, String toValue) {
        Pattern p = Pattern.compile(fromValue);
        return p.matcher(metaValue).replaceFirst(toValue);
    }
    private String regularReplace(
            String metaValue, String fromValue, String toValue) {
        if (Objects.equals(metaValue, fromValue)) {
            return toValue;
        }
        return metaValue;
    }
        
    public List<Replacement> getReplacements() {
        return Collections.unmodifiableList(replacements);
    }

    public void removeReplacement(String fromField) {
        List<Replacement> toRemove = new ArrayList<>();
        for (Replacement replacement : replacements) {
            if (Objects.equals(replacement.getFromField(), fromField)) {
                toRemove.add(replacement);
            }
        }
        synchronized (replacements) {
            replacements.removeAll(toRemove);
        }
    }
    
    public void addReplacement(
            String fromValue, String toValue, String fromField) {
        addReplacement(fromValue, toValue, fromField, null, false);
    }
    public void addReplacement(
            String fromValue, String toValue, String fromField, boolean regex) {
        addReplacement(fromValue, toValue, fromField, null, regex);
    }
    public void addReplacement(String fromValue, String toValue, 
            String fromField, String toField) {
        addReplacement(fromValue, toValue, fromField, toField, false);
    }
    public void addReplacement(String fromValue, String toValue, 
            String fromField, String toField, boolean regex) {
        replacements.add(new Replacement(
                fromField, fromValue, toField, toValue, regex));
    }

    
    public class Replacement implements Serializable {
        private static final long serialVersionUID = 9206061804991938873L;
        private final String fromField;
        private final String fromValue;
        private final String toField;
        private final String toValue;
        private final boolean regex;
        public Replacement(String fromField, String fromValue, String toField,
                String toValue, boolean regex) {
            super();
            this.fromField = fromField;
            this.fromValue = fromValue;
            this.toField = toField;
            this.toValue = toValue;
            this.regex = regex;
        }
        public String getFromField() {
            return fromField;
        }
        public String getFromValue() {
            return fromValue;
        }
        public String getToField() {
            return toField;
        }
        public String getToValue() {
            return toValue;
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
            result = prime * result
                    + ((fromValue == null) ? 0 : fromValue.hashCode());
            result = prime * result + (regex ? 1231 : 1237);
            result = prime * result
                    + ((toField == null) ? 0 : toField.hashCode());
            result = prime * result
                    + ((toValue == null) ? 0 : toValue.hashCode());
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
            Replacement other = (Replacement) obj;
            if (fromField == null) {
                if (other.fromField != null) {
                    return false;
                }
            } else if (!fromField.equals(other.fromField)) {
                return false;
            }
            if (fromValue == null) {
                if (other.fromValue != null) {
                    return false;
                }
            } else if (!fromValue.equals(other.fromValue)) {
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
            if (toValue == null) {
                if (other.toValue != null) {
                    return false;
                }
            } else if (!toValue.equals(other.toValue)) {
                return false;
            }
            return true;
        }
        @Override
        public String toString() {
            return "Replacement [fromField=" + fromField + ", fromValue="
                    + fromValue + ", toField=" + toField + ", toValue=" + toValue
                    + ", regex=" + regex + "]";
        }
    }
    
    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("replace");
        for (HierarchicalConfiguration node : nodes) {
            addReplacement(
                    node.getString("fromValue"),
                    node.getString("toValue"),
                    node.getString("[@fromField]"),
                    node.getString("[@toField]", null),
                    node.getBoolean("[@regex]", false));
        }
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (Replacement replacement : replacements) {
            writer.writeStartElement("replace");
            writer.writeAttribute("fromField", replacement.getFromField());
            if (replacement.getToField() != null) {
                writer.writeAttribute("toField", replacement.getToField());
            }
            writer.writeAttribute("regex", 
                    Boolean.toString(replacement.isRegex()));
            writer.writeStartElement("fromValue");
            writer.writeCharacters(replacement.getFromValue());
            writer.writeEndElement();
            writer.writeStartElement("toValue");
            writer.writeCharacters(replacement.getToValue());
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    @Override
    public String toString() {
        return "ReplaceTagger [replacements=" + replacements + "]";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((replacements == null) ? 0 : replacements.hashCode());
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
        ReplaceTagger other = (ReplaceTagger) obj;
        if (replacements == null) {
            if (other.replacements != null) {
                return false;
            }
        } else if (!replacements.equals(other.replacements)) {
            return false;
        }
        return true;
    }
}
