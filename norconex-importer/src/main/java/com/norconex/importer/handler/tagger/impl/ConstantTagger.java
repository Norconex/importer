/* Copyright 2010-2020 Norconex Inc.
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Define and add constant values to documents.  To add multiple constant
 * values under the same constant name, repeat the constant entry with a
 * different value.
 * </p>
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior
 * with {@link #setOnSet(PropertySetter)}.
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.ConstantTagger"
 *     {@nx.include com.norconex.commons.lang.map.PropertySetter#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <!-- multiple constant tags allowed -->
 *   <constant name="CONSTANT_NAME">Constant Value</constant>
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 *  <handler class="com.norconex.importer.handler.tagger.impl.ConstantTagger">
 *    <constant name="source">web</constant>
 *  </handler>
 * }
 * <p>
 * The above example adds a constant to incoming documents to identify they
 * were web documents.
 * </p>
 *
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public class ConstantTagger extends AbstractDocumentTagger{

    @Deprecated
    public enum OnConflict { ADD, REPLACE, NOOP };

    private final Map<String, List<String>> constants = new HashMap<>();
    private PropertySetter onSet;

    @Override
    public void tagApplicableDocument(
            HandlerDoc doc, InputStream document, ParseState parseState)
                    throws ImporterHandlerException {
        for (Entry<String, List<String>> entry : constants.entrySet()) {
            PropertySetter.orAppend(onSet).apply(
                    doc.getMetadata(), entry.getKey(), entry.getValue());
        }
    }

    /**
     * Gets the property setter to use when a value is set.
     * @return property setter
     * @since 3.0.0
     */
    public PropertySetter getOnSet() {
        return onSet;
    }
    /**
     * Sets the property setter to use when a value is set.
     * @param onSet property setter
     * @since 3.0.0
     */
    public void setOnSet(PropertySetter onSet) {
        this.onSet = onSet;
    }

    /**
     * Gets the conflict resolution strategy.
     * @return conflict resolution strategy
     * @since 2.7.0
     * @deprecated Since 3.0.0, use {@link #getOnSet()} instead
     */
    @Deprecated
    public OnConflict getOnConflict() {
        if (PropertySetter.REPLACE == onSet) {
            return OnConflict.REPLACE;
        }
        if (PropertySetter.OPTIONAL == onSet) {
            return OnConflict.NOOP;
        }
        return OnConflict.ADD;
    }
    /**
     * Sets the conflict resolution strategy.
     * @param onConflict conflict resolution strategy.
     * @since 2.7.0
     * @deprecated Since 3.0.0, use {@link #setOnSet(PropertySetter)} instead
     */
    @Deprecated
    public void setOnConflict(OnConflict onConflict) {
        if (onConflict == null) {
            onSet = null;
        } else if (OnConflict.ADD == onConflict) {
            onSet = PropertySetter.APPEND;
        } else if (OnConflict.REPLACE == onConflict) {
            onSet = PropertySetter.REPLACE;
        } else if (OnConflict.NOOP == onConflict) {
            onSet = PropertySetter.OPTIONAL;
        }
    }

    public Map<String, List<String>> getConstants() {
        return Collections.unmodifiableMap(constants);
    }

    public void addConstant(String name, String value) {
        if (name != null && value != null) {
            constants.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
    }
    public void removeConstant(String name) {
        constants.remove(name);
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        xml.checkDeprecated("@onConflict", "onSet", true);
        setOnSet(PropertySetter.fromXML(xml, onSet));
        List<XML> nodes = xml.getXMLList("constant");
        for (XML node : nodes) {
            String name = node.getString("@name");
            String value = node.getString(".");
            addConstant(name, value);
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        PropertySetter.toXML(xml, getOnSet());
        for (Entry<String, List<String>> entry : constants.entrySet()) {
            List<String> values = entry.getValue();
            for (String value : values) {
                if (value != null) {
                    xml.addElement("constant", value)
                            .setAttribute("name", entry.getKey());
                }
            }
        }
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
