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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.tagger.IDocumentTagger;

/**
 * Detects a document language and assigns it to a metadata field.  
 * Optionally add the language precision detected as a separate field.
 * Also, one can have stored in separate fields any other languages detected
 * with a lower precision score.
 * <p />
 * Can be used both as a pre-parse (on text only) or post-parse handler.
 * <p />
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.tagger.impl.LanguageTagger"
 *          shortText="(false|true)" &gt;
 *      &lt;languages&gt
 *        (Considered language list. Defaults to all languages described higher.)
 *      &lt;/languages&gt
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class LanguageTagger 
        implements IDocumentTagger, IXMLConfigurable {

    //TODO Check if doc.size is defined in metadata? If so, use it to 
    //determine if we are going with small or long text?
    
    //TODO provide ways to overwrite language profiles form the config?
    
    private static final long serialVersionUID = -6062036871216739761L;
    
    private final Map<String, List<String>> constants = 
            new HashMap<String, List<String>>();
    
    @Override
    public void tagDocument(
            String reference, InputStream document, 
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        DetectorFactory.loadProfile(profileDirectory);

        
        Detector detector = DetectorFactory.create();
        //detector.set
        
        
        
        
        for (String name : constants.keySet()) {
            List<String> values = constants.get(name);
            if (values != null) {
                for (String value : values) {
                    metadata.addString(name, value);
                }
            }
        }
    }

    public Map<String, List<String>> getConstants() {
        return Collections.unmodifiableMap(constants);
    }

    public void addConstant(String name, String value) {
        if (name != null && value != null) {
            List<String> values = constants.get(name);
            if (values == null) {
                values = new ArrayList<String>(1);
                constants.put(name, values);
            }
            values.add(value);
        }
    }
    public void removeConstant(String name) {
        constants.remove(name);
    }

    @Override
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationLoader.loadXML(in);
            List<HierarchicalConfiguration> nodes =
                    xml.configurationsAt("constant");
            for (HierarchicalConfiguration node : nodes) {
                String name = node.getString("[@name]");
                String value = node.getString("");
                addConstant(name, value);
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
            
            for (String name : constants.keySet()) {
                List<String> values = constants.get(name);
                for (String value : values) {
                    if (value != null) {
                        writer.writeStartElement("constant");
                        writer.writeAttribute("name", name);
                        writer.writeCharacters(value);
                        writer.writeEndElement();
                    }
                }
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
        StringBuilder builder = new StringBuilder();
        builder.append("ConstantTagger [{");
        boolean first = true;
        for (String name : constants.keySet()) {
            List<String> values = constants.get(name);
            for (String value : values) {
                if (value != null) {
                    if (!first) {
                        builder.append(", ");
                    }
                    builder.append("[name=").append(name)
                        .append(", value=").append(value)
                        .append("]");
                    first = false;
                }
            }
        }
        builder.append("}]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((constants == null) ? 0 : constants.hashCode());
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
        LanguageTagger other = (LanguageTagger) obj;
        if (constants == null) {
            if (other.constants != null) {
                return false;
            }
        } else if (!constants.equals(other.constants)) {
            return false;
        }
        return true;
    }
}
