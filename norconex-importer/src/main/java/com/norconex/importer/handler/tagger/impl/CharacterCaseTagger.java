/* Copyright 2014-2020 Norconex Inc.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Changes the character case of matching fields and values according to
 * one of the following methods:</p>
 * <ul>
 *   <li>upper: Changes all characters to upper case.</li>
 *   <li>lower: Changes all characters values to lower case.</li>
 *   <li>words: Converts the first letter of each words to upper case,
 *           and leaves the character case of other characters unchanged.</li>
 *   <li>wordsFully: Converts the first letter of each words to upper case,
 *           and the rest to lower case.</li>
 *   <li>sentences: Converts the first letter of each sentence to upper case,
 *           and leaves the character case of other characters unchanged.</li>
 *   <li>sentencesFully: Converts the first letter of each sentence to upper
 *           case, and converts other characters to lower case.</li>
 *   <li>string: Converts the first letter of a string to upper case, and
 *           leaves the character case of other characters unchanged.</li>
 *   <li>stringFully: Converts the first letter of a string to upper
 *           case, and converts other characters to lower case.</li>
 *   <li>swap: Converts all upper case characters to lower case, and all
 *           lower case to upper case.</li>
 * </ul>
 * <p>The change of character case can be applied to one of the
 * following (defaults to "value" when unspecified):</p>
 * <ul>
 *   <li>value: Applies to the field values.</li>
 *   <li>field: Applies to the field name.</li>
 *   <li>both: Applies to both the field name and its values.</li>
 * </ul>
 * <p>Field names are referenced in a case insensitive manner.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"
 *     type="[upper|lower|words|wordsFully|sentences|sentencesFully|string|stringFully|swap]"
 *     applyTo="[value|field|both]">
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *     (expression to narrow by matching fields)
 *   </fieldMatcher>
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <!-- Converts title to lowercase -->
 * <handler class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"
 *     type="lower" applyTo="field">
 *   <fieldMatcher>title</fieldMatcher>
 * </handler>
 * <!-- Make first title character uppercase -->
 * <handler class="com.norconex.importer.handler.tagger.impl.CharacterCaseTagger"
 *     type="string" applyTo="value">
 *   <fieldMatcher>title</fieldMatcher>
 * </handler>
 * }
 * <p>
 * The above examples first convert a title to lower case except for the
 * first character.
 * </p>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("javadoc")
public class CharacterCaseTagger extends AbstractDocumentTagger {

    private static final Logger LOG =
            LoggerFactory.getLogger(CharacterCaseTagger.class);

    public static final String CASE_WORDS = "words";
    public static final String CASE_WORDS_FULLY = "wordsFully";
    public static final String CASE_UPPER = "upper";
    public static final String CASE_LOWER = "lower";
    public static final String CASE_SWAP = "swap";
    public static final String CASE_SENTENCES = "sentences";
    public static final String CASE_SENTENCES_FULLY = "sentencesFully";
    public static final String CASE_STRING = "string";
    public static final String CASE_STRING_FULLY = "stringFully";

    public static final String APPLY_VALUE = "value";
    public static final String APPLY_FIELD = "field";
    public static final String APPLY_BOTH = "both";

    private final TextMatcher fieldMatcher = new TextMatcher();
    private String caseType;
    private String applyTo;

    /**
     * Gets field matcher.
     * @return field matcher
     * @since 3.0.0
     */
    public TextMatcher getFieldMatcher() {
        return fieldMatcher;
    }
    /**
     * Sets field matcher.
     * @param fieldMatcher field matcher
     * @since 3.0.0
     */
    public void setFieldMatcher(TextMatcher fieldMatcher) {
        this.fieldMatcher.copyFrom(fieldMatcher);
    }
    /**
     * Gets the type of character case transformation.
     * @return type of case transformation
     * @since 3.0.0
     */
    public String getCaseType() {
        return caseType;
    }
    /**
     * Sets the type of character case transformation.
     * @param caseType type of case transformation
     * @since 3.0.0
     */
    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }
    /**
     * Gets whether to apply the case transformation to fields, values,
     * or both.
     * @return one of "field", "value", or "both"
     * @since 3.0.0
     */
    public String getApplyTo() {
        return applyTo;
    }
    /**
     * Sets whether to apply the case transformation to fields, values,
     * or both.
     * @param applyTo one of "field", "value", or "both"
     * @since 3.0.0
     */
    public void setApplyTo(String applyTo) {
        this.applyTo = applyTo;
    }

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {

        if (StringUtils.isNotBlank(applyTo)
                &&  !StringUtils.equalsAnyIgnoreCase(
                        applyTo, APPLY_FIELD, APPLY_VALUE, APPLY_BOTH)) {
            LOG.warn("Unsupported \"applyTo\": {}", applyTo);
            return;
        }

        for (Entry<String, List<String>> en :
                metadata.matchKeys(fieldMatcher).entrySet()) {

            String field = en.getKey();
            String newField = field;

            // Do field
            if (EqualsUtil.equalsAny(applyTo, APPLY_FIELD, APPLY_BOTH)) {
                newField = changeFieldCase(field, metadata);
            }

            // Do values
            if (StringUtils.isBlank(applyTo) || EqualsUtil.equalsAny(
                    applyTo, APPLY_VALUE, APPLY_BOTH)) {
                changeValuesCase(newField, metadata);
            }
        }
    }

    private String changeFieldCase(String field, ImporterMetadata metadata) {
        List<String> values = metadata.getStrings(field);
        String newField = changeCase(field, caseType);
        metadata.remove(field);
        if (values != null && !values.isEmpty()) {
            metadata.setList(newField, values);
        }
        return newField;
    }
    private void changeValuesCase(
            String field, ImporterMetadata metadata) {
        List<String> values = metadata.getStrings(field);
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                values.set(i, changeCase(values.get(i), caseType));
            }
            metadata.setList(field, values);
        }
    }

    private String changeCase(String value, String type) {
        if (CASE_UPPER.equals(type)) {
            return StringUtils.upperCase(value);
        } else if (CASE_LOWER.equals(type)) {
            return StringUtils.lowerCase(value);
        } else if (CASE_WORDS.equals(type)) {
            return WordUtils.capitalize(value);
        } else if (CASE_WORDS_FULLY.equals(type)) {
            return WordUtils.capitalizeFully(value);
        } else if (CASE_SWAP.equals(type)) {
            return WordUtils.swapCase(value);
        } else if (CASE_STRING.equals(type)) {
            return capitalizeString(value);
        } else if (CASE_STRING_FULLY.equals(type)) {
            return capitalizeStringFully(value);
        } else if (CASE_SENTENCES.equals(type)) {
            return capitalizeSentences(value);
        } else if (CASE_SENTENCES_FULLY.equals(type)) {
            return capitalizeSentencesFully(value);
        } else {
            LOG.warn("Unsupported character case type: {}", type);
            return value;
        }
    }

    /**
     * Adds field case changing instructions.
     * @param field the field to apply the case changing
     * @param caseType the type of case change to apply
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)},
     * {@link #setCaseType(String)}.
     */
    @Deprecated
    public void addFieldCase(String field, String caseType) {
        addFieldCase(field, caseType, null);
    }
    /**
     * Adds field case changing instructions.
     * @param field the field to apply the case changing
     * @param caseType the type of case change to apply
     * @param applyTo what to apply the case change to
     * @since 2.4.0
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)},
     * {@link #setCaseType(String)}, and {@link #setApplyTo(String)}.
     */
    @Deprecated
    public void addFieldCase(String field, String caseType, String applyTo) {
        setFieldMatcher(TextMatcher.basic(field));
        setCaseType(caseType);
        setApplyTo(applyTo);
    }
    /**
     * Gets the field matcher pattern.
     * @return field matcher pattern
     * @deprecated Since 3.0.0 use {@link #getFieldMatcher()}
     */
    @Deprecated
    public Set<String> getFieldNames() {
        return new HashSet<>(Arrays.asList(fieldMatcher.getPattern()));
    }
    /**
     * Get the case conversion type.
     * @param fieldName field name
     * @return case type
     * @deprecated Since 3.0.0 use {@link #getCaseType()}
     */
    @Deprecated
    public String getCaseType(String fieldName) {
        return caseType;
    }
    /**
     * Gets what if the case change applies to fields, values, or both.
     * @param fieldName the field name
     * @return "field", "value", or "both"
     * @since 2.4.0
     * @deprecated Since 3.0.0 use {@link #getApplyTo()}
     */
    @Deprecated
    public String getApplyTo(String fieldName) {
        return applyTo;
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        xml.checkDeprecated("characterCase", "[see class documentation]", true);
        setCaseType(xml.getString("@type"));
        setApplyTo(xml.getString("@applyTo"));
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("type", caseType);
        xml.setAttribute("applyTo", applyTo);
        fieldMatcher.saveToXML(xml.addElement("fieldMatcher"));
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
    private String capitalizeStringFully(String value) {
        return capitalizeString(StringUtils.lowerCase(value));
    }
    private String capitalizeSentences(String value) {
        if (StringUtils.isNotBlank(value)) {
            StringBuffer b = new StringBuffer();
            Pattern re = Pattern.compile(
                    "[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?\\s|$)[^.!?]*)*"
                  + "[.!?]?['\"]?(?=\\s|$)",
                    Pattern.MULTILINE);
            Matcher m = re.matcher(value);
            while (m.find()) {
                m.appendReplacement(b, capitalizeString(m.group()));
            }
            m.appendTail(b);
            return b.toString();
        }
        return value;
    }
    private String capitalizeSentencesFully(String value) {
        return capitalizeSentences(StringUtils.lowerCase(value));
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
