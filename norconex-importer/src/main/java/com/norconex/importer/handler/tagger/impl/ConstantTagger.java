/* Copyright 2010-2017 Norconex Inc.
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
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Define and add constant values to documents.  To add multiple constant 
 * values under the same constant name, repeat the constant entry with a 
 * different value.  
 * </p>
 * <h3>Conflict resolution</h3>
 * <p>
 * If a field with the same name already exists 
 * for a document, the constant value(s) will be added
 * to the list of already existing values.
 * </p>
 * <p><b>Since 2.7.0</b>, it is possible to change this default behavior
 * with {@link #setOnConflict(OnConflict)}. Possible values are:
 * </p>
 * <ul>
 *   <li><b>add</b>: add the constant value(s) to any existing ones (default).</li>
 *   <li><b>replace</b>: replace any existing values with constant ones.</li>
 *   <li><b>noop</b>: No operation (does nothing).</li>
 * </ul>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.ConstantTagger"
 *          onConflict="[add|replace|noop]" &gt;
 *  
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;constant name="CONSTANT_NAME"&gt;Constant Value&lt;/constant&gt;
 *      &lt;!-- multiple constant tags allowed --&gt;
 *      
 *  &lt;/tagger&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * Adds a constant to incoming documents to identify they were web documents. 
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.ConstantTagger"&gt;
 *      &lt;constant name="source"&gt;web&lt;/constant&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 */
public class ConstantTagger extends AbstractDocumentTagger{

    public enum OnConflict { ADD, REPLACE, NOOP };
    public static final OnConflict DEFAULT_ON_CONFLICT = OnConflict.ADD;
    
    
    private final Map<String, List<String>> constants = new HashMap<>();
    private OnConflict onConflict = DEFAULT_ON_CONFLICT;
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document, 
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        for (Entry<String, List<String>> entry : constants.entrySet()) {
            String name = entry.getKey();
            List<String> newValues = entry.getValue();
            if (newValues == null) {
                continue;
            }
            if (onConflict == OnConflict.REPLACE) {
                metadata.remove(name);
            }
            if (onConflict != OnConflict.NOOP
                    || CollectionUtils.isEmpty(metadata.get(name))) {
                for (String value : newValues) {
                    metadata.addString(name, value);
                }
            }
        }
    }

    /**
     * Gets the conflict resolution strategy.
     * @return conflict resolution strategy
     * @since 2.7.0
     */
    public OnConflict getOnConflict() {
        return onConflict;
    }
    /**
     * Sets the conflict resolution strategy.
     * @param onConflict conflict resolution strategy.
     * @since 2.7.0
     */
    public void setOnConflict(OnConflict onConflict) {
        if (onConflict == null) {
            throw new IllegalArgumentException("onConflict cannot be null.");
        }
        this.onConflict = onConflict;
    }

    public Map<String, List<String>> getConstants() {
        return Collections.unmodifiableMap(constants);
    }

    public void addConstant(String name, String value) {
        if (name != null && value != null) {
            List<String> values = constants.get(name);
            if (values == null) {
                values = new ArrayList<>(1);
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
        String xmlOC = xml.getString(
                "[@onConflict]", DEFAULT_ON_CONFLICT.toString()).toUpperCase();
        try {
            setOnConflict(OnConflict.valueOf(xmlOC));
        } catch (IllegalArgumentException e)  {
            throw new ConfigurationException("Configuration error: "
                    + "Invalid \"onConflict\" attribute value: \"" 
                    + xmlOC + "\".  Must be one of \"add\", \"replace\" "
                    + " or \"noop\"", e);
        }
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
        writer.writeAttribute("onConflict", 
                onConflict.toString().toLowerCase()); 
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
    public boolean equals(final Object other) {
        if (!(other instanceof ConstantTagger)) {
            return false;
        }
        ConstantTagger castOther = (ConstantTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(constants, castOther.constants)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(constants)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("constants", constants)
                .toString();
    }
}
