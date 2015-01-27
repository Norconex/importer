/* Copyright 2014 Norconex Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

//TODO offer sentences and capitalizations?

/**
 * Changes the character case of a field value according to one of the following
 * methods:
 * <p />
 * <ul>
 *   <li>uppper: Changes all characters to upper case.</li>
 *   <li>lower: Changes all characters values to lower case.</li>
 *   <li>words: Converts the first letter of each words to upper case, and the
 *              rest to lowercase.</li>
 * </ul>
 * <p />
 * Can be used both as a pre-parse or post-parse handler.
 * <p>
 * XML configuration usage:
 * <p />
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"&gt;
 *      &lt;characterCase type="(upper|lower|words)" 
 *                     fieldName="(field to change)" /&gt
 *      &lt;!-- multiple characterCase tags allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("nls")
public class CharacterCaseTagger extends AbstractDocumentTagger {

    private static final Logger LOG = 
            LogManager.getLogger(CharacterCaseTagger.class);
    
    public static final String CASE_WORDS = "words";
    public static final String CASE_UPPER = "upper";
    public static final String CASE_LOWER = "lower";
    
    private final Map<String, String> fieldCases = new HashMap<>();
    
    @Override
    public void tagApplicableDocument(
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
                    } else if (CASE_WORDS.equals(type)) {
                        values.set(i, WordUtils.capitalizeFully(value));
                    } else {
                        LOG.warn("Unsupported character case type: " + type);
                    }
                }
                metadata.setString(fieldName, 
                        values.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
            }
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
    protected void loadHandlerFromXML(XMLConfiguration xml) {
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("characterCase");
        fieldCases.clear();
        for (HierarchicalConfiguration node : nodes) {
            addFieldCase(
                    node.getString("[@fieldName]"),
                    node.getString("[@type]"));
        }
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (String fieldName : fieldCases.keySet()) {
            writer.writeStartElement("characterCase");
            writer.writeAttribute("fieldName", fieldName);
            writer.writeAttribute("type", fieldCases.get(fieldName)); 
            writer.writeEndElement();
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
