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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Define and add constant values to documents.  To add multiple constant 
 * values under the same constant name, repeat the constant entry with a 
 * different value.
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.ConstantTagger"&gt;
 *      &lt;constant name="CONSTANT_NAME"&gt;Constant Value&lt;/constant&gt;
 *      &lt;!-- multiple constant tags allowed --&gt;
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
public class ConstantTagger extends AbstractDocumentTagger{

    private final Map<String, List<String>> constants = 
            new HashMap<String, List<String>>();
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document, 
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
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
    protected void loadHandlerFromXML(XMLConfiguration xml) {
        List<HierarchicalConfiguration> nodes =
                xml.configurationsAt("constant");
        for (HierarchicalConfiguration node : nodes) {
            String name = node.getString("[@name]");
            String value = node.getString("");
            addConstant(name, value);
        }
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
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
        ConstantTagger other = (ConstantTagger) obj;
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
