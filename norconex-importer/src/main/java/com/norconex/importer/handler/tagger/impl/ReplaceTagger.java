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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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

    
    public static class Replacement {
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
        public String toString() {
            return "Replacement [fromField=" + fromField + ", fromValue="
                    + fromValue + ", toField=" + toField + ", toValue=" + toValue
                    + ", regex=" + regex + "]";
        }
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof Replacement))
                return false;
            Replacement castOther = (Replacement) other;
            return new EqualsBuilder().append(fromField, castOther.fromField)
                    .append(fromValue, castOther.fromValue)
                    .append(toField, castOther.toField)
                    .append(toValue, castOther.toValue)
                    .append(regex, castOther.regex).isEquals();
        }
        private transient int hashCode;
        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = new HashCodeBuilder().append(fromField)
                        .append(fromValue).append(toField).append(toValue)
                        .append(regex).toHashCode();
            }
            return hashCode;
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
