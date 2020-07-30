/* Copyright 2017-2020 Norconex Inc.
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
package com.norconex.importer.util.regex;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.RegexFieldValueExtractor;

/**
 * Simplify extraction of field names and values from any text,
 * using regular expression match groups for the field name and value.
 * At least one of "field" or "fieldGroup" must be specified.  If fieldGroup
 * is specified without a "field" and finds no matches, the matching
 * of the value is ignored.
 * @author Pascal Essiembre
 * @since 2.8.0
 * @deprecated Since 3.0.0, use {@link RegexFieldValueExtractor} from
 *         Norconex Commons Lang
 */
@Deprecated
public class RegexFieldExtractor {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegexFieldExtractor.class);

    public static final RegexFieldExtractor[] EMPTY_ARRAY =
            new RegexFieldExtractor[] {};

    private String field;
    private String regex;
    private boolean caseSensitive;
    private int fieldGroup = -1;
    private int valueGroup = -1;

    public RegexFieldExtractor() {
        super();
    }
    public RegexFieldExtractor(String regex) {
        this(regex, null);
    }
    public RegexFieldExtractor(String regex, String field) {
        this(regex, field, -1);
    }
    public RegexFieldExtractor(String regex, String field, int valueGroup) {
        this.regex = regex;
        this.field = field;
        this.valueGroup = valueGroup;
    }
    public RegexFieldExtractor(String regex, int fieldGroup, int valueGroup) {
        super();
        this.regex = regex;
        this.fieldGroup = fieldGroup;
        this.valueGroup = valueGroup;
    }

    public String getRegex() {
        return regex;
    }
    public RegexFieldExtractor setRegex(String regex) {
        this.regex = regex;
        return this;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    public RegexFieldExtractor setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }
    public int getFieldGroup() {
        return fieldGroup;
    }
    public RegexFieldExtractor setFieldGroup(int fieldGroup) {
        this.fieldGroup = fieldGroup;
        return this;
    }
    public int getValueGroup() {
        return valueGroup;
    }
    public RegexFieldExtractor setValueGroup(int valueGroup) {
        this.valueGroup = valueGroup;
        return this;
    }
    public String getField() {
        return field;
    }
    public RegexFieldExtractor setField(String field) {
        this.field = field;
        return this;
    }

    public void extractFields(Properties dest, CharSequence text) {
        if (StringUtils.isBlank(field) && !hasFieldGroup()) {
            throw new IllegalArgumentException(
                    "At least one of 'field' or 'fieldGroup' expected.");
        }
        Matcher m = matcher(text);
        while (m.find()) {
            String fieldName = extractFieldName(m);
            String fieldValue = extractFieldValue(m);
            if (StringUtils.isBlank(fieldName)) {
                LOG.debug("No field name for value: {}", fieldValue);
            } else if (fieldValue == null) {
                LOG.debug("Null value for field: {}", field);
            } else {
                dest.add(fieldName, fieldValue);
            }
        }
    }
    public Properties extractFields(CharSequence text) {
        Properties dest = new Properties();
        extractFields(dest, text);
        return dest;
    }

    private Matcher matcher(CharSequence text) {
        return RegexUtil.compileDotAll(regex, isCaseSensitive()).matcher(text);
    }
    private String extractFieldName(Matcher m) {
        String f = StringUtils.isNotBlank(getField()) ? getField() : "";
        if (hasFieldGroup()) {
            if (m.groupCount() < getFieldGroup()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No match group {} for field name in regex \"{}\""
                            + "for match value \"{}\". Defaulting to field "
                            + "name: \"{}\".",
                            getFieldGroup(), getRegex(), m.group(), field);
                }
            } else {
                f = m.group(getFieldGroup());
            }
        }
        return f;
    }
    private String extractFieldValue(Matcher m) {
        if (hasValueGroup()) {
            if (m.groupCount() < getValueGroup()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No match group {} "
                            + "for field value in regex \"{}\" "
                            + "for match value \"{}\". "
                            + "Defaulting to entire match.",
                            getFieldGroup(), getRegex(), m.group());
                }
            } else {
                return m.group(getValueGroup());
            }
        }
        return m.group();
    }
    private boolean hasFieldGroup() {
        return getFieldGroup() > -1;
    }
    private boolean hasValueGroup() {
        return getValueGroup() > -1;
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
