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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.IDocumentTagger;

//TODO offer sentences and words capitalizations?

/**
 * Changes the character case of a field value according to one of the following
 * methods:
 * <p />
 * <ul>
 *   <li>uppper: Changes all characters to upper case.</li>
 *   <li>lower: Changes all characters values to lower case.</li>
 * </ul>
 * <p />
 * Can be used both as a pre-parse or post-parse handler.
 * <p>
 * XML configuration usage:
 * <p />
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"&gt;
 *      &lt;characterCase type="(upper|lower)" 
 *                     fieldName="(field to change)" /&gt
 *      &lt;!-- multiple characterCase tags allowed --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("nls")
public class CharacterCaseTagger implements IDocumentTagger, IXMLConfigurable {

    private static final long serialVersionUID = 2008414745944904813L;
    private static final Logger LOG = 
            LogManager.getLogger(CharacterCaseTagger.class);
    
    public static final String CASE_UPPER = "upper";
    public static final String CASE_LOWER = "lower";
    
    private final Map<String, String> fieldCases = new HashMap<>();
    
    @Override
    public void tagDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {
        
        for (String fieldName : fieldCases.keySet()) {
            String type = fieldCases.get(fieldName);
            List<String> values = metadata.getStrings(fieldName);
            if (values != null) {
                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i);
                    if (CASE_UPPER.equals(type)) {
                        values.set(i, StringUtils.upperCase(value));
                    } else if (CASE_LOWER.equals(type)) {
                        values.set(i, StringUtils.lowerCase(value));
                    } else {
                        LOG.warn("Unsupported character case type: " + type);
                    }
                }
            }
            metadata.setString(
                    fieldName, values.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        }
    }

    public void addFieldCase(String field, String caseType) {
        fieldCases.put(field, caseType);
    }
    public Set<String> getFieldNames() {
        return fieldCases.keySet();
    }
    public String getCaseType(String fieldName) {
        return fieldCases.get(fieldName);
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationLoader.loadXML(in);
            List<HierarchicalConfiguration> nodes = 
                    xml.configurationsAt("characterCase");
            fieldCases.clear();
            for (HierarchicalConfiguration node : nodes) {
                addFieldCase(
                        node.getString("[@fieldName]"),
                        node.getString("[@type]"));
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

            for (String fieldName : fieldCases.keySet()) {
                writer.writeStartElement("characterCase");
                writer.writeAttribute("fieldName", fieldName);
                writer.writeAttribute("type", fieldCases.get(fieldName)); 
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
        return "CharacterCaseTagger [fieldCases=" + fieldCases + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((fieldCases == null) ? 0 : fieldCases.hashCode());
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
        if (!(obj instanceof CharacterCaseTagger)) {
            return false;
        }
        CharacterCaseTagger other = (CharacterCaseTagger) obj;
        if (fieldCases == null) {
            if (other.fieldCases != null) {
                return false;
            }
        } else if (!fieldCases.equals(other.fieldCases)) {
            return false;
        }
        return true;
    }
}
