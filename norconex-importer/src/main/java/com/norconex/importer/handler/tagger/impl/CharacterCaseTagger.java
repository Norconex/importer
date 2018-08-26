/* Copyright 2014-2018 Norconex Inc.
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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Changes the character case of field values according to one of the
 * following methods:</p>
 * <ul>
 *   <li>uppper: Changes all characters to upper case.</li>
 *   <li>lower: Changes all characters values to lower case.</li>
 *   <li>words: Converts the first letter of each words to upper case, and the
 *           rest to lower case.</li>
 *   <li>string: Converts the first letter of a string if not numeric, and
 *           leaves the character case of other characters unchanged
 *           (since 2.7.0).</li>
 *   <li>swap: Converts all upper case characters to lower case, and all
 *           lower case to upper case (since 2.7.0).</li>
 *
 * </ul>
 * <p>Since 2.3.0, the change of character case can be applied to one of the
 * following (defaults to "value" when unspecified):</p>
 * <ul>
 *   <li>value: Applies to the field values.</li>
 *   <li>field: Applies to the field name.</li>
 *   <li>both: Applies to both the field name and its values.</li>
 * </ul>
 * <p>Field names are referenced in a case insensitive manner.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;characterCase fieldName="(field to change)"
 *                     type="[upper|lower|words|string|swap]"
 *                     applyTo="[value|field|both]" /&gt;
 *      &lt;!-- multiple characterCase tags allowed --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * Documents can use different character case for their title field names.
 * This example makes them all lower case. We also make sure the value
 * starts with an upper case.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"&gt;
 *      &lt;characterCase fieldName="title" type="lower" applyTo="field" /&gt;
 *      &lt;characterCase fieldName="title" type="string" applyTo="value" /&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class CharacterCaseTagger extends AbstractDocumentTagger {

    private static final Logger LOG =
            LoggerFactory.getLogger(CharacterCaseTagger.class);

    public static final String CASE_WORDS = "words";
    public static final String CASE_UPPER = "upper";
    public static final String CASE_LOWER = "lower";
    public static final String CASE_SWAP = "swap";
    public static final String CASE_STRING = "string";

    public static final String APPLY_VALUE = "value";
    public static final String APPLY_FIELD = "field";
    public static final String APPLY_BOTH = "both";

    private final Map<String, CaseChangeDetails> fieldCases = new HashMap<>();


    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {

        for (Entry<String, CaseChangeDetails>  entry : fieldCases.entrySet()) {
            CaseChangeDetails d = entry.getValue();
            boolean validApplyTo = false;

            String newField = entry.getKey();

            // Do field
            if (EqualsUtil.equalsAny(d.applyTo, APPLY_FIELD, APPLY_BOTH)) {
                newField = changeFieldCase(entry.getKey(), d, metadata);
                validApplyTo = true;
            }

            // Do values
            if (StringUtils.isBlank(d.applyTo) || EqualsUtil.equalsAny(
                    d.applyTo, APPLY_VALUE, APPLY_BOTH)) {
                changeValuesCase(newField, d, metadata);
                validApplyTo = true;
            }

            if (!validApplyTo) {
                LOG.warn("Unsupported \"applyTo\": {}", d.applyTo);
            }
        }
    }

    private String changeFieldCase(
            String field, CaseChangeDetails d, ImporterMetadata metadata) {
        List<String> values = metadata.getStrings(field);
        String newField = changeCase(field, d.caseType);
        metadata.remove(field);
        if (values != null && !values.isEmpty()) {
            metadata.set(
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
            metadata.set(field,
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
        } else if (CASE_SWAP.equals(type)) {
            return WordUtils.swapCase(value);
        } else if (CASE_STRING.equals(type)) {
            return capitalizeString(value);
        } else {
            LOG.warn("Unsupported character case type: {}", type);
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
    protected void loadHandlerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("characterCase");
        fieldCases.clear();
        for (XML node : nodes) {
            addFieldCase(
                    node.getString("@fieldName"),
                    node.getString("@type"),
                    node.getString("@applyTo"));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (Entry<String, CaseChangeDetails> entry : fieldCases.entrySet()) {
            XML ccXML = xml.addElement("characterCase")
                    .setAttribute("fieldName", entry.getKey());
            CaseChangeDetails d = entry.getValue();
            if (d != null) {
                ccXML.setAttribute("type", d.caseType);
                ccXML.setAttribute("applyTo", d.applyTo);
            }
        }
    }

    private String capitalizeString(String value) {
        if (StringUtils.isNotBlank(value)) {
            Matcher m = Pattern.compile(
                    "^(.*?)([\\p{IsAlphabetic}\\p{IsDigit}])").matcher(value);
            if (m.find()) {
                String firstChar =
                        StringUtils.upperCase(m.group(2), Locale.ENGLISH);
                return m.replaceFirst("$1" + firstChar);
            }
        }
        return value;
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

    private class CaseChangeDetails {
        private final String caseType;
        private final String applyTo;
        public CaseChangeDetails(String caseType, String applyTo) {
            super();
            this.caseType = caseType;
            this.applyTo = applyTo;
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
}
