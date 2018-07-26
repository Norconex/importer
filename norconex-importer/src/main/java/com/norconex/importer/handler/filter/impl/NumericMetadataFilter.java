/* Copyright 2015-2018 Norconex Inc.
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
import java.util.Arrays;
import java.util.Collection;
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

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
/**
 * <p>
 * Accepts or rejects a document based on the numeric value(s) of a metadata
 * field, supporting decimals. If multiple values are found for a field, only
 * one of them needs to match for this filter to take effect.
 * If the value is not a valid number, it is considered not to be matching.
 * The decimal character is expected to be a dot.
 * To reject decimals or to deal with
 * non-numeric fields in your own way, you can use {@link RegexMetadataFilter}.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.NumericMetadataFilter"
 *          onMatch="[include|exclude]"
 *          field="(name of metadata field to match)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;!-- Use one or two (for ranges) conditions,
 *           where possible operators are:
 *
 *               gt -&gt; greater than
 *               ge -&gt; greater equal
 *               lt -&gt; lower than
 *               le -&gt; lowe equal
 *               eq -&gt; equals
 *        --&gt;
 *
 *      &lt;condition operator="[gt|ge|lt|le|eq]" number="(number)" /&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>For example, let's say you are importing customer profile documents
 *    and you have a field called "age" and you need to only consider documents
 *    for customers in their twenties (greater or equal to
 *    20, but lower than 30). The following would achieve that</p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.NumericMetadataFilter"
 *          onMatch="include" field="age" &gt;
 *      &lt;condition operator="ge" number="20" /&gt;
 *      &lt;condition operator="lt" number="30" /&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.2.0
 */
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

    private String field;
    private final List<Condition> conditions = new ArrayList<>(2);

    public NumericMetadataFilter() {
        this(null);
    }
    public NumericMetadataFilter(String field) {
        this(field, OnMatch.INCLUDE);
    }
    public NumericMetadataFilter(String field, OnMatch onMatch) {
        super();
        this.field = field;
        setOnMatch(onMatch);
    }

    public String getField() {
        return field;
    }
    public void setField(String property) {
        this.field = property;
    }
    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }
    public void setConditions(Condition... conditions) {
        this.conditions.clear();
        this.conditions.addAll(Arrays.asList(conditions));
    }
    public void addCondition(Operator operator, double number) {
        conditions.add(new Condition(operator, number));
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (StringUtils.isBlank(field)) {
            throw new IllegalArgumentException("\"field\" cannot be empty.");
        }
        Collection<String> values =  metadata.getStrings(field);
        for (String value : values) {
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
        setField(xml.getString("@field", getField()));
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
        xml.setAttribute("field", field);
        for (Condition condition : conditions) {
            xml.addElement("condition")
                    .setAttribute("operator", condition.operator.abbr)
                    .setAttribute("number", condition.number);
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

