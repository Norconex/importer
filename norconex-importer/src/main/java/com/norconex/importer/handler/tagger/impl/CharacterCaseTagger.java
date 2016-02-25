/* Copyright 2014-2015 Norconex Inc.
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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

//TODO offer sentences and capitalizations?

/**
 * <p>Changes the character case of field values according to one of the 
 * following methods:</p>
 * <ul>
 *   <li>uppper: Changes all characters to upper case.</li>
 *   <li>lower: Changes all characters values to lower case.</li>
 *   <li>words: Converts the first letter of each words to upper case, and the
 *              rest to lowercase.</li>
 * </ul>
 * <p>Since 2.3.0, the change of character case can be applied to one of the 
 * following (defaults to "value" when unspecified):</p>
 * <ul>
 *   <li>value: Applies to the field values.</li>
 *   <li>field: Applies to the field name.</li>
 *   <li>both: Applies to both the field name and its values.</li>
 * </ul>
 * <p>Field names are referenced in a case insensitive manner.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"&gt;
 *      &lt;characterCase fieldName="(field to change)"
 *                     type="[upper|lower|words]" 
 *                     applyTo="[value|field|both]" /&gt;
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

    public static final String APPLY_VALUE = "value";
    public static final String APPLY_FIELD = "field";
    public static final String APPLY_BOTH = "both";

    private final Map<String, CaseChangeDetails> fieldCases = new HashMap<>();
    
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {

        for (String fieldName : fieldCases.keySet()) {
            CaseChangeDetails d = fieldCases.get(fieldName);
            boolean validApplyTo = false;
            
            String newField = fieldName;
            
            // Do field
            if (EqualsUtil.equalsAny(d.applyTo, APPLY_FIELD, APPLY_BOTH)) {
                newField = changeFieldCase(fieldName, d, metadata);
                validApplyTo = true;
            }
                
            // Do values
            if (StringUtils.isBlank(d.applyTo) || EqualsUtil.equalsAny(
                    d.applyTo, APPLY_VALUE, APPLY_BOTH)) {
                changeValuesCase(newField, d, metadata);
                validApplyTo = true;
            }
            
            if (!validApplyTo) {
                LOG.warn("Unsupported \"applyTo\": " + d.applyTo);
            }
        }
    }
    
    private String changeFieldCase(
            String field, CaseChangeDetails d, ImporterMetadata metadata) {
        List<String> values = metadata.getStrings(field);
        String newField = changeCase(field, d.caseType);
        metadata.remove(field);
        if (values != null && !values.isEmpty()) {
            metadata.setString(
                    newField, values.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        }
        return newField;
    }
    private void changeValuesCase(
            String field, CaseChangeDetails d, ImporterMetadata metadata) {
        List<String> values = metadata.getStrings(field);
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                values.set(i, changeCase(value, d.caseType));
            }
            metadata.setString(field, 
                    values.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        }
    }

    private String changeCase(String value, String type) {
        if (CASE_UPPER.equals(type)) {
            return StringUtils.upperCase(value);
        } else if (CASE_LOWER.equals(type)) {
            return StringUtils.lowerCase(value);
        } else if (CASE_WORDS.equals(type)) {
            return WordUtils.capitalizeFully(value);
        } else {
            LOG.warn("Unsupported character case type: " + type);
            return value;
        }
    }

    
    public void addFieldCase(String field, String caseType) {
        addFieldCase(field, caseType, null);
    }
    /**
     * Adds field case changing instructions.
     * @param field the field to apply the case changing
     * @param caseType the type of case change to apply
     * @param applyTo what to apply the case change to
     * @since 2.4.0
     */
    public void addFieldCase(String field, String caseType, String applyTo) {
        fieldCases.put(field, new CaseChangeDetails(caseType, applyTo));
    }
    public Set<String> getFieldNames() {
        return fieldCases.keySet();
    }
    public String getCaseType(String fieldName) {
        CaseChangeDetails d = fieldCases.get(fieldName);
        if (d != null) {
            return d.caseType;
        }
        return null;
    }
    /**
     * Gets what the case changing instructions apply to.
     * @param fieldName the field name
     * @return what the case changing instructions apply to
     * @since 2.4.0
     */
    public String getApplyTo(String fieldName) {
        CaseChangeDetails d = fieldCases.get(fieldName);
        if (d != null) {
            return d.applyTo;
        }
        return null;
    }
    
    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) {
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("characterCase");
        fieldCases.clear();
        for (HierarchicalConfiguration node : nodes) {
            addFieldCase(
                    node.getString("[@fieldName]"),
                    node.getString("[@type]"),
                    node.getString("[@applyTo]"));
        }
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (String fieldName : fieldCases.keySet()) {
            writer.writeStartElement("characterCase");
            writer.writeAttributeString("fieldName", fieldName);
            CaseChangeDetails d = fieldCases.get(fieldName);
            if (d != null) {
                writer.writeAttributeString("type", d.caseType); 
                writer.writeAttributeString("applyTo", d.applyTo); 
            }
            writer.writeEndElement();
        }
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CharacterCaseTagger)) {
            return false;
        }
        CharacterCaseTagger castOther = (CharacterCaseTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(fieldCases, castOther.fieldCases)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(fieldCases)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("fieldCases", fieldCases)
                .toString();
    }
    
    private class CaseChangeDetails {
        private String caseType;
        private String applyTo;
        public CaseChangeDetails(String caseType, String applyTo) {
            super();
            this.caseType = caseType;
            this.applyTo = applyTo;
        }
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof CaseChangeDetails)) {
                return false;
            }
            CaseChangeDetails castOther = (CaseChangeDetails) other;
            return new EqualsBuilder()
                    .append(caseType, castOther.caseType)
                    .append(applyTo, castOther.applyTo)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(caseType)
                    .append(applyTo)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("caseType", caseType)
                    .append("applyTo", applyTo)
                    .toString();
        }
    }
}
