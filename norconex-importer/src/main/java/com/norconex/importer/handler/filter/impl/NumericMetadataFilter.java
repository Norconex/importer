/* Copyright 2015-2020 Norconex Inc.
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
package com.norconex.importer.handler.filter.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
/**
 * <p>
 * Accepts or rejects a document based on the numeric value(s) of matching
 * metadata fields, supporting decimals. If multiple values are found for a
 * field, only one of them needs to match for this filter to take effect.
 * If the value is not a valid number, it is considered not to be matching.
 * The decimal character is expected to be a dot.
 * To reject decimals or to deal with
 * non-numeric fields in your own way, you can use {@link TextFilter}.
 * </p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.filter.impl.NumericMetadataFilter"
 *   {@nx.include com.norconex.importer.handler.filter.AbstractDocumentFilter#attributes}>
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#attributes}>
 *     (expression matching numeric fields to filter)
 *   </fieldMatcher>
 *
 *   <!-- Use one or two (for ranges) conditions,
 *        where possible operators are:
 *
 *          gt -> greater than
 *          ge -> greater equal
 *          lt -> lower than
 *          le -> lowe equal
 *          eq -> equals
 *   -->
 *
 *   <condition operator="[gt|ge|lt|le|eq]" number="(number)" />
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.filter.impl.NumericMetadataFilter"
 *     onMatch="include">
 *   <fieldMatcher>age</fieldMatcher>
 *   <condition operator="ge" number="20" />
 *   <condition operator="lt" number="30" />
 *  </handler>
 * }
 * <p>
 * Let's say you are importing customer profile documents
 * and you have a field called "age" and you need to only consider documents
 * for customers in their twenties (greater or equal to
 * 20, but lower than 30). The above example would achieve that.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.2.0
 */
@SuppressWarnings("javadoc")
public class NumericMetadataFilter extends AbstractDocumentFilter {

    private static final Logger LOG =
            LoggerFactory.getLogger(NumericMetadataFilter.class);

    public enum Operator {
        GREATER_THAN("gt") {@Override
        public boolean evaluate(double fieldNumber, double conditionNumber) {
            return fieldNumber > conditionNumber;
        }},
        GREATER_EQUAL("ge") {@Override
        public boolean evaluate(double fieldNumber, double conditionNumber) {
            return fieldNumber >= conditionNumber;
        }},
        EQUALS("eq") {@Override
        public boolean evaluate(double fieldNumber, double conditionNumber) {
            return fieldNumber == conditionNumber;
        }},
        LOWER_EQUAL("le") {@Override
        public boolean evaluate(double fieldNumber, double conditionNumber) {
            return fieldNumber <= conditionNumber;
        }},
        LOWER_THAN("lt") {@Override
        public boolean evaluate(double fieldNumber, double conditionNumber) {
            return fieldNumber < conditionNumber;
        }};
        String abbr;
        private Operator(String abbr) {
            this.abbr = abbr;
        }
        public static Operator getOperator(String op) {
            if (StringUtils.isBlank(op)) {
                return null;
            }
            for (Operator c : Operator.values()) {
                if (c.abbr.equalsIgnoreCase(op)) {
                    return c;
                }
            }
            return null;
        }
        @Override
        public String toString() {
            return abbr;
        }
        public abstract boolean evaluate(
                double fieldNumber, double conditionNumber);
    }

    private final TextMatcher fieldMatcher = new TextMatcher();
    private final List<Condition> conditions = new ArrayList<>(2);

    public NumericMetadataFilter() {
        super();
    }
    /**
     * Constructor.
     * @param field field to apply numeric filtering
     * @deprecated Since 3.0.0, use {@link #NumericMetadataFilter(TextMatcher)}
     */
    @Deprecated
    public NumericMetadataFilter(String field) {
        this(field, OnMatch.INCLUDE);
    }
    /**
     * Constructor.
     * @param field field to apply numeric filtering
     * @param onMatch include or exclude on match
     * @deprecated Since 3.0.0, use
     *             {@link #NumericMetadataFilter(TextMatcher, OnMatch)}
     */
    @Deprecated
    public NumericMetadataFilter(String field, OnMatch onMatch) {
        this(TextMatcher.basic(field), onMatch);
    }

    /**
     * Constructor.
     * @param fieldMatcher matcher for fields on which to apply date filtering
     * @since 3.0.0
     */
    public NumericMetadataFilter(TextMatcher fieldMatcher) {
        this(fieldMatcher, OnMatch.INCLUDE);
    }
    /**
     *
     * @param fieldMatcher matcher for fields on which to apply date filtering
     * @param onMatch include or exclude on match
     * @since 3.0.0
     */
    public NumericMetadataFilter(TextMatcher fieldMatcher, OnMatch onMatch) {
        super();
        this.fieldMatcher.copyFrom(fieldMatcher);
        setOnMatch(onMatch);
    }

    /**
     * Deprecated.
     * @return field name
     * @deprecated Since 3.0.0, use {@link #getFieldMatcher()}.
     */
    @Deprecated
    public String getField() {
        return fieldMatcher.getPattern();
    }
    /**
     * Deprecated.
     * @param field field name
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)}
     */
    @Deprecated
    public void setField(String field) {
        this.fieldMatcher.setPattern(field);
    }

    public TextMatcher getFieldMatcher() {
        return fieldMatcher;
    }
    public void setFieldMatcher(TextMatcher fieldMatcher) {
        this.fieldMatcher.copyFrom(fieldMatcher);
    }

    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }
    public void setConditions(Condition... conditions) {
        CollectionUtil.setAll(this.conditions, conditions);
    }
    public void addCondition(Operator operator, double number) {
        conditions.add(new Condition(operator, number));
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (fieldMatcher.getPattern() == null) {
            throw new IllegalArgumentException(
                    "\"fieldMatcher\" pattern cannot be empty.");
        }
        for (String value : metadata.matchKeys(fieldMatcher).valueList()) {
            if (meetsAllConditions(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean meetsAllConditions(String fieldValue) {
        if (!NumberUtils.isCreatable(fieldValue)) {
            return false;
        }
        double fieldNumber = NumberUtils.toDouble(fieldValue);
        for (Condition condition : conditions) {
            if (!condition.getOperator().evaluate(
                    fieldNumber, condition.getNumber())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void loadFilterFromXML(XML xml) {
        xml.checkDeprecated("@field", "fieldMatcher", true);
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
        List<XML> nodes = xml.getXMLList("condition");
        for (XML node : nodes) {
            String op = node.getString("@operator", null);
            String num = node.getString("@number", null);
            if (StringUtils.isBlank(op) || StringUtils.isBlank(num)) {
                LOG.warn("Both \"operator\" and \"number\" must be provided.");
                break;
            }
            Operator operator = Operator.getOperator(op);
            if (operator == null) {
                LOG.warn("Unsupported operator: {}", op);
                break;
            }
            if (!NumberUtils.isCreatable(num)) {
                LOG.debug("Not a valid number: {}", num);
                break;
            }
            double number = NumberUtils.toDouble(num);
            addCondition(operator, number);
        }
    }

    @Override
    protected void saveFilterToXML(XML xml) {
        for (Condition condition : conditions) {
            xml.addElement("condition")
                    .setAttribute("operator", condition.operator.abbr)
                    .setAttribute("number", condition.number);
        }
        fieldMatcher.saveToXML(xml.addElement("fieldMatcher"));
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

    public static class Condition {
        private final Operator operator;
        private final double number;
        public Condition(Operator operator, double number) {
            super();
            this.operator = operator;
            this.number = number;
        }
        public Operator getOperator() {
            return operator;
        }
        public double getNumber() {
            return number;
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

