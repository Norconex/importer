/* Copyright 2010-2013 Norconex Inc.
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.IDocumentTagger;


/**
 * Replaces an existing metadata value with another one.  The "toName" argument
 * is optional (the same field will be used for the replacement if no
 * "toName" is specified").
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.tagger.impl.ReplaceTagger"&gt;
 *      &lt;replace fromName="sourceFieldName" toName="targetFieldName" 
 *               regex="[false|true]"&gt
 *          &lt;fromValue&gtSource Value&lt;/fromValue&gt
 *          &lt;toValue&gtTarget Value&lt;/toValue&gt
 *      &lt;/replace&gt
 *      &lt;!-- multiple replace tags allowed --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 *
 */
@SuppressWarnings("nls")
public class ReplaceTagger implements IDocumentTagger, IXMLConfigurable {

    private static final long serialVersionUID = -6062036871216739761L;
    
    private final List<Replacement> replacements = new ArrayList<>();
    
    @Override
    public void tagDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        
        for (Replacement repl : replacements) {
            if (metadata.containsKey(repl.getFromName())) {
                String[] metaValues = metadata.getStrings(repl.getFromName())
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
                        if (StringUtils.isNotBlank(repl.getToName())) {
                            metadata.addString(repl.getToName(), newValue);
                        } else {
                            metaValues[i] = newValue;
                            metadata.setString(repl.getFromName(), metaValues);
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

    public void removeReplacement(String fromName) {
        List<Replacement> toRemove = new ArrayList<>();
        for (Replacement replacement : replacements) {
            if (Objects.equals(replacement.getFromName(), fromName)) {
                toRemove.add(replacement);
            }
        }
        synchronized (replacements) {
            replacements.removeAll(toRemove);
        }
    }
    
    public void addReplacement(
            String fromValue, String toValue, String fromName) {
        addReplacement(fromValue, toValue, fromName, null, false);
    }
    public void addReplacement(
            String fromValue, String toValue, String fromName, boolean regex) {
        addReplacement(fromValue, toValue, fromName, null, regex);
    }
    public void addReplacement(String fromValue, String toValue, 
            String fromName, String toName) {
        addReplacement(fromValue, toValue, fromName, toName, false);
    }
    public void addReplacement(String fromValue, String toValue, 
            String fromName, String toName, boolean regex) {
        replacements.add(new Replacement(
                fromName, fromValue, toName, toValue, regex));
    }

    
    public class Replacement implements Serializable {
        private static final long serialVersionUID = 9206061804991938873L;
        private final String fromName;
        private final String fromValue;
        private final String toName;
        private final String toValue;
        private final boolean regex;
        public Replacement(String fromName, String fromValue, String toName,
                String toValue, boolean regex) {
            super();
            this.fromName = fromName;
            this.fromValue = fromValue;
            this.toName = toName;
            this.toValue = toValue;
            this.regex = regex;
        }
        public String getFromName() {
            return fromName;
        }
        public String getFromValue() {
            return fromValue;
        }
        public String getToName() {
            return toName;
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
                    + ((fromName == null) ? 0 : fromName.hashCode());
            result = prime * result
                    + ((fromValue == null) ? 0 : fromValue.hashCode());
            result = prime * result + (regex ? 1231 : 1237);
            result = prime * result
                    + ((toName == null) ? 0 : toName.hashCode());
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
            if (fromName == null) {
                if (other.fromName != null) {
                    return false;
                }
            } else if (!fromName.equals(other.fromName)) {
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
            if (toName == null) {
                if (other.toName != null) {
                    return false;
                }
            } else if (!toName.equals(other.toName)) {
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
            return "Replacement [fromName=" + fromName + ", fromValue="
                    + fromValue + ", toName=" + toName + ", toValue=" + toValue
                    + ", regex=" + regex + "]";
        }
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationLoader.loadXML(in);
            List<HierarchicalConfiguration> nodes = 
                    xml.configurationsAt("replace");
            for (HierarchicalConfiguration node : nodes) {
                addReplacement(
                        node.getString("fromValue"),
                        node.getString("toValue"),
                        node.getString("[@fromName]"),
                        node.getString("[@toName]", null),
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

            for (Replacement replacement : replacements) {
                writer.writeStartElement("replace");
                writer.writeAttribute("fromName", replacement.getFromName());
                if (replacement.getToName() != null) {
                    writer.writeAttribute("toName", replacement.getToName());
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
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
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
