/* Copyright 2021 Norconex Inc.
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
package com.norconex.importer.handler.condition.impl;

import static com.norconex.commons.lang.Operator.EQUALS;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.Operator;
import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.condition.IImporterCondition;
import com.norconex.importer.parser.ParseState;
import com.norconex.importer.util.FormatUtil;

/**
 * <p>
 * A condition based on the date value(s) of matching
 * metadata fields given the supplied date format. If multiple values are
 * found for a field, only one of them needs to match for this condition to
 * be true.
 * If the value is not a valid date, it is considered not to be matching
 * (i.e., <code>false</code>).
 * The default operator is "eq" (equals).
 * </p>
 *
 * <h3>Single date vs range of dates:</h3>
 * <p>
 * This condition accepts zero, one, or two value matchers:
 * </p>
 * <ul>
 *   <li>
 *     <b>0:</b> Use no value matcher to simply evaluate
 *     whether the value is a date.
 *   </li>
 *   <li>
 *     <b>1:</b> Use one value matcher to evaluate if the value is
 *     lower/greater and/or the same as the specified date.
 *   </li>
 *   <li>
 *     <b>2:</b> Use two value matchers to define a date range to evaluate
 *     (both matches have to evaluate to <code>true</code>).
 *   </li>
 * </ul>
 *
 * <h3>Metadata date field format:</h3>
 * <p>To successfully parse a date, you can specify a date format,
 * as per the formatting options found on {@link DateTimeFormatter}.
 * The default format when not specified is EPOCH (the difference, measured in
 * milliseconds, between the date and midnight, January 1, 1970).</p>
 *
 * <h3>Absolute date conditions:</h3>
 * <p>When defining a date value matcher, you can specify an absolute
 * date (i.e. a constant date value) to be used for comparison.
 * Supported formats for configuring an absolute date are:
 * </p>
 * <pre>
 *   yyyy-MM-dd                -&gt; date (e.g. 2015-05-31)
 *   yyyy-MM-ddThh:mm:ss[.SSS] -&gt; date and time with optional
 *                                milliseconds (e.g. 2015-05-31T22:44:15)
 * </pre>
 *
 * <h3>Relative date conditions:</h3>
 * <P>Date value matchers can also specify a moment in time relative to the
 * current date using the <code>TODAY</code> or <code>NOW</code> keyword,
 * optionally followed by a number of time units to add/remove.
 * <code>TODAY</code> is the current day without the hours, minutes, and
 * seconds, where as <code>NOW</code> is the current day with the hours,
 * minutes, and seconds. You can also decide whether you want the
 * current date to be fixed for the lifetime of this condition (does not change
 * after being set for the first time), or whether
 * it should be refreshed on every invocation to reflect the passing of time.
 * </p>
 *
 * <h3>Time zones:</h3>
 * <p>
 * When comparing dates at a more granular level (e.g., hours, minutes,
 * seconds), it may be important to take time zones into account.
 * If the time zone (id or offset) is part of a document field date value
 * and the configured date format supports time zones, it will be be
 * interpreted as a date in the encountered time zone.
 * </p>
 * <p>
 * In cases where you want to overwrite the value's existing time zone or
 * specify one for dates without time zones, you can do so with
 * the {@link #setDocZoneId(ZoneId)} method.
 * Explicitly setting a time zone will not "convert" a date to that time zone,
 * but will rather assume it was created in the supplied time zone.
 * </p>
 * <p>
 * When using XML configuration to define the condition dates, you can
 * specify the time zone using the <code>conditionZoneId</code> option.
 * </p>
 *
 * {@nx.include com.norconex.commons.lang.Operator#operators}
 *
 * {@nx.xml.usage
 * <condition class="com.norconex.importer.handler.condition.impl.DateCondition"
 *     format="(document field date format)"
 *     docZoneId="(force a time zone on evaluated fields.)"
 *     conditionZoneId="(time zone of configured condition dates.)">
 *
 *
 *     <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (expression matching date fields to evaluate)
 *     </fieldMatcher>
 *
 *     <!-- Use one or two (for ranges) conditions where:
 *
 *       Possible operators are:
 *
 *         gt -> greater than
 *         ge -> greater equal
 *         lt -> lower than
 *         le -> lower equal
 *         eq -> equals
 *
 *       Condition date value format are either one of:
 *
 *         yyyy-MM-dd                -> date (e.g. 2015-05-31)
 *         yyyy-MM-ddThh:mm:ss[.SSS] -> date and time with optional
 *                                      milliseconds (e.g. 2015-05-31T22:44:15)
 *         TODAY[-+]9[YMDhms][*]     -> the string "TODAY" (at 0:00:00) minus
 *                                      or plus a number of years, months, days,
 *                                      hours, minutes, or seconds
 *                                      (e.g. 1 week ago: TODAY-7d).
 *                                      * means TODAY can change from one
 *                                      invocation to another to adjust to a
 *                                      change of current day
 *         NOW[-+]9[YMDhms][*]       -> the string "NOW" (at current time) minus
 *                                      or plus a number of years, months, days,
 *                                      hours, minutes, or seconds
 *                                      (e.g. 1 week ago: NOW-7d).
 *                                      * means NOW changes from one invocation
 *                                      to another to adjust to the current time.
 *    -->
 *
 *     <valueMatcher operator="[gt|ge|lt|le|eq]" date="(a date)" />
 *
 * </condition>
 * }
 *
 * {@nx.xml.example
 * <condition class="DateCondition"
 *     format="yyyy-MM-dd'T'HH:mm:ssZ"
 *     conditionZoneId="America/New_York">
 *   <fieldMatcher>publish_date</fieldMatcher>
 *   <valueMatcher operator="ge" date="TODAY-7" />
 *   <valueMatcher operator="lt" date="TODAY" />
 * </condition>
 * }
 * <p>
 * The above example will only keep documents from the last
 * seven days, not including today.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 3.0.0
 */
@SuppressWarnings("javadoc")
public class DateCondition implements IImporterCondition, IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(DateCondition.class);

    public enum TimeUnit {
        YEAR(ChronoUnit.YEARS, "Y"),
        MONTH(ChronoUnit.MONTHS, "M"),
        DAY(ChronoUnit.DAYS, "D"),
        HOUR(ChronoUnit.HOURS, "h"),
        MINUTE(ChronoUnit.MINUTES, "m"),
        SECOND(ChronoUnit.SECONDS, "s");
        private final TemporalUnit temporalUnit;
        private final String abbr;
        TimeUnit(TemporalUnit temporalUnit, String abbr) {
            this.temporalUnit = temporalUnit;
            this.abbr = abbr;
        }
        public TemporalUnit toTemporal() {
            return temporalUnit;
        }
        @Override
        public String toString() {
            return abbr;
        }
        public static TimeUnit getTimeUnit(String unit) {
            if (StringUtils.isBlank(unit)) {
                return null;
            }
            for (TimeUnit tu : TimeUnit.values()) {
                if (tu.abbr.equalsIgnoreCase(unit)) {
                    return tu;
                }
            }
            return null;
        }
    }

    private final TextMatcher fieldMatcher = new TextMatcher();
    private ValueMatcher valueMatcher;
    private ValueMatcher valueMatcherRangeEnd;
    private String format;
    private ZoneId docZoneId;
    // condition zoneId is only kept here for when we save to XML.
    private ZoneId conditionZoneId;


    public DateCondition() {
        super();
    }
    public DateCondition(TextMatcher fieldMatcher) {
        this(fieldMatcher, null, null);
    }
    public DateCondition(
            TextMatcher fieldMatcher, ValueMatcher valueMatcher) {
        this(fieldMatcher, valueMatcher, null);
    }
    public DateCondition(
            TextMatcher fieldMatcher,
            ValueMatcher rangeStart,
            ValueMatcher rangeEnd) {
        setFieldMatcher(fieldMatcher);
        this.valueMatcher = rangeStart;
        this.valueMatcherRangeEnd = rangeEnd;
    }

    public TextMatcher getFieldMatcher() {
        return fieldMatcher;
    }
    /**
     * Sets the text matcher of field names. Copies it.
     * @param fieldMatcher text matcher
     */
    public void setFieldMatcher(TextMatcher fieldMatcher) {
        this.fieldMatcher.copyFrom(fieldMatcher);
    }

    public ValueMatcher getValueMatcher() {
        return valueMatcher;
    }
    public void setValueMatcher(ValueMatcher firstValueMatcher) {
        this.valueMatcher = firstValueMatcher;
    }

    public ValueMatcher getValueMatcherRangeEnd() {
        return valueMatcherRangeEnd;
    }
    public void setValueMatcherRangeEnd(ValueMatcher secondValueMatcher) {
        this.valueMatcherRangeEnd = secondValueMatcher;
    }

    /**
     * Gets the time zone id documents are considered to be.
     * @return zone id
     */
    public ZoneId getDocZoneId() {
        return docZoneId;
    }
    /**
     * Sets the time zone id documents are considered to be.
     * @param docZoneId zone id
     */
    public void setDocZoneId(ZoneId docZoneId) {
        this.docZoneId = docZoneId;
    }


    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public boolean testDocument(
            HandlerDoc doc, InputStream input, ParseState parseState)
                    throws ImporterHandlerException {
        if (fieldMatcher.getPattern() == null) {
            throw new IllegalArgumentException(
                    "\"fieldMatcher\" pattern cannot be empty.");
        }
        for (Entry<String, List<String>> en :
                doc.getMetadata().matchKeys(fieldMatcher).entrySet()) {
            for (String value : en.getValue()) {
                if (matches(valueMatcher, en.getKey(), value)
                        && matches(valueMatcherRangeEnd, en.getKey(), value)) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean matches(
            ValueMatcher matcher, String fieldName, String fieldValue) {
        if (matcher == null) {
            return true;
        }

        ZonedDateTime dt = FormatUtil.parseZonedDateTimeString(
                fieldValue, format, null, fieldName, docZoneId);
        if (dt == null) {
            return false;
        }

        // if the date obtained by the supplier (the date value or logic
        // configured) starts with TODAY, we truncate that date to
        // ensure we are comparing apples to apples. Else, one must ensure
        // the date format matches for proper comparisons.
        if (StringUtils.startsWithIgnoreCase(
                matcher.getDateTimeSupplier().toString(), "today")) {
            dt = dt.truncatedTo(ChronoUnit.DAYS);
        }

        Operator op = defaultIfNull(matcher.operator, EQUALS);
        boolean evalResult = op.evaluate(dt, matcher.getDateTime());
        if (LOG.isDebugEnabled()) {
            LOG.debug("{}: {} [{}] {} = {}",
                    fieldName, fieldValue, op,
                    matcher.getDateTime(), evalResult);
        }
        return evalResult;
    }

    @Override
    public void loadFromXML(XML xml) {
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
        setFormat(xml.getString("@format", format));

        ZoneId dZoneId = null;
        String dZoneIdStr = xml.getString("@docZoneId", null);
        if (StringUtils.isNotBlank(dZoneIdStr)) {
            dZoneId = ZoneId.of(dZoneIdStr);
        }
        setDocZoneId(dZoneId);

        ZoneId cZoneId = null;
        String cZoneIdStr = xml.getString("@conditionZoneId", null);
        if (StringUtils.isNotBlank(cZoneIdStr)) {
            cZoneId = ZoneId.of(cZoneIdStr);
        }
        this.conditionZoneId = cZoneId;

        List<XML> nodes = xml.getXMLList("valueMatcher");
        if (nodes.size() >= 1) {
            setValueMatcher(toValueMatcher(nodes.get(0)));
        }
        if (nodes.size() >= 2) {
            setValueMatcherRangeEnd(toValueMatcher(nodes.get(1)));
        }
    }

    private ValueMatcher toValueMatcher(XML xml) {
        Operator operator = Operator.of(
                xml.getString("@operator", EQUALS.toString()));
        if (operator == null) {
            throw new IllegalArgumentException(
                    "Unsupported operator: " + xml.getString("@operator"));
        }
        return new ValueMatcher(operator,
                toDateTimeSupplier(xml.getString("@date", null)));
    }

    private static final Pattern RELATIVE_PARTS = Pattern.compile(
            //1              23            4         5
            "^(NOW|TODAY)\\s*(([-+]{1})\\s*(\\d+)\\s*([YMDhms]{1})\\s*)?"
            //6
           + "(\\*{0,1})$");
    private Supplier<ZonedDateTime> toDateTimeSupplier(String dateStr) {
        String d = StringUtils.trimToNull(dateStr);
        if (d == null) {
            throw new IllegalArgumentException("\"date\" must not be blank.");
        }
        try {
            // NOW[-+]9[YMDhms][*]
            // TODAY[-+]9[YMDhms][*]
            Matcher m = RELATIVE_PARTS.matcher(d);
            if (m.matches()) {
                //--- Dynamic ---
                TimeUnit unit = null;
                int amount = NumberUtils.toInt(m.group(4), -1);
                if (amount > -1) {
                    if  ("-".equals(m.group(3))) {
                        amount = -amount;
                    }
                    String unitStr = m.group(5);
                    unit = TimeUnit.getTimeUnit(unitStr);
                    if (unit == null) {
                        throw new ConfigurationException(
                                "Invalid time unit: " + unitStr);
                    }
                }
                boolean fixed = !"*".equals(m.group(6));
                boolean today = "TODAY".equals(m.group(1));

                if (fixed) {
                    return new DynamicFixedDateTimeSupplier(
                            unit, amount, today, conditionZoneId);
                }
                return new DynamicFloatingDateTimeSupplier(
                        unit, amount, today, conditionZoneId);
            }
            
            String dateFormat = null;
            DateFormat py;
            
            py = d.contains(".") ? new DateFormat1() : (d.contains("T") || d.contains(":")) ? new DateFormat2() : new DateFormat3();
            dateFormat=py.giveDateFormat();
            
            ZonedDateTime dt = FormatUtil.parseZonedDateTimeString(
                    dateStr, dateFormat, null, null, conditionZoneId);
            return new StaticDateTimeSupplier(dt);
        } catch (DateTimeParseException e) {
            throw new ConfigurationException(
                    "Date parse error for value: " + dateStr, e);
        }
    }

    @Override
    public void saveToXML(XML xml) {
        xml.setAttribute("format", format);
        xml.setAttribute("docZoneId", docZoneId);
        xml.setAttribute("conditionZoneId", conditionZoneId);
        fieldMatcher.saveToXML(xml.addElement("fieldMatcher"));
        if (valueMatcher != null) {
            xml.addElement("valueMatcher")
                    .setAttribute("operator", valueMatcher.operator)
                    .setAttribute("date", valueMatcher.dateTimeSupplier);
        }
        if (valueMatcherRangeEnd != null) {
            xml.addElement("valueMatcher")
                    .setAttribute("operator", valueMatcher.operator)
                    .setAttribute("date", valueMatcher.dateTimeSupplier);
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

    public static class ValueMatcher {
        private final Operator operator;
        private final Supplier<ZonedDateTime> dateTimeSupplier;
        public ValueMatcher(
                Operator operator,
                Supplier<ZonedDateTime> dateTimeSupplier) {
            super();
            this.operator = operator;
            this.dateTimeSupplier = dateTimeSupplier;
        }
        public ZonedDateTime getDateTime() {
            return dateTimeSupplier.get();
        }
        protected Supplier<ZonedDateTime> getDateTimeSupplier() {
            return dateTimeSupplier;
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
            return new ReflectionToStringBuilder(this,
                    ToStringStyle.SHORT_PREFIX_STYLE).toString();
        }
    }

    // Static local date, assumed to be of the zone Id supplied
    // (the ZoneId argument is ignored).
    public static class StaticDateTimeSupplier
            implements Supplier<ZonedDateTime> {
        private final ZonedDateTime dateTime;
        private final String toString;
        public StaticDateTimeSupplier(ZonedDateTime dateTime) {
            super();
            this.dateTime = Objects.requireNonNull(
                    dateTime, "'dateTime' must not be null.");
            this.toString = dateTime.format(DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
        @Override
        public ZonedDateTime get() {
            return dateTime;
        }
        @Override
        public String toString() {
            return toString;
        }
        @Override
        public boolean equals(final Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    // Dynamically generated date time, that changes with every invocation.
    public static class DynamicFloatingDateTimeSupplier
            implements Supplier<ZonedDateTime> {
        private final TimeUnit unit;
        private final int amount;
        private final boolean today; // default is false == NOW
        private final ZoneId zoneId;
        public DynamicFloatingDateTimeSupplier(
                TimeUnit unit, int amount, boolean today, ZoneId zoneId) {
            super();
            this.unit = unit;
            this.amount = amount;
            this.today = today;
            this.zoneId = zoneId;
        }
        @Override
        public ZonedDateTime get() {
            return dynamicDateTime(unit, amount, today, zoneId);
        }
        @Override
        public String toString() {
            return dynamicToString(unit, amount, today, true);
        }
        @Override
        public boolean equals(final Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    // Dynamically generated date time, that once generated, never changes
    public static class DynamicFixedDateTimeSupplier
            implements Supplier<ZonedDateTime> {
        private final TimeUnit unit;
        private final int amount;
        private final boolean today; // default is false == NOW
        private final ZoneId zoneId;
        private final String toString;
        private ZonedDateTime dateTime;
        public DynamicFixedDateTimeSupplier(
                TimeUnit unit, int amount, boolean today, ZoneId zoneId) {
            super();
            this.unit = unit;
            this.amount = amount;
            this.today = today;
            this.zoneId = zoneId;
            this.toString = dynamicToString(unit, amount, today, false);
        }
        @Override
        public ZonedDateTime get() {
            if (dateTime == null) {
                dateTime = createDateTime(zoneId);
            }
            return dateTime;
        }
        public synchronized ZonedDateTime createDateTime(ZoneId zoneId) {
            if (dateTime == null) {
                return dynamicDateTime(unit, amount, today, zoneId);
            }
            return dateTime;
        }
        @Override
        public String toString() {
            return toString;
        }
        @Override
        public boolean equals(final Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private static ZonedDateTime dynamicDateTime(
            TimeUnit unit, int amount, boolean today, ZoneId zoneId) {
        ZonedDateTime dt = ZonedDateTime.now();
        if (zoneId != null) {
            dt = dt.withZoneSameLocal(zoneId);
        }

        if (today) {
            dt = dt.truncatedTo(ChronoUnit.DAYS);
        }
        if (unit != null) {
            dt = dt.plus(amount, unit.toTemporal());
        }
        return dt;
    }
    private static String dynamicToString(
            TimeUnit unit,
            int amount,
            boolean today,
            boolean floating) {
        StringBuilder b = new StringBuilder();
        if (today) {
            b.append("TODAY");
        } else {
            b.append("NOW");
        }
        if (unit != null) {
            if (amount >= 0) {
                b.append('+');
            }
            b.append(amount);
            b.append(unit.toString());
        }
        if (floating) {
            b.append('*');
        }
        return b.toString();
    }
}
class DateFormat1 implements DateFormat
{
    public String giveDateFormat()
    {
        String dateFormat;
        dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        return dateFormat;
    }
}
class DateFormat2 implements DateFormat
{
    public String giveDateFormat()
    {
        String dateFormat;
        dateFormat = "yyyy-MM-dd'T'HH:mm:ss";
        return dateFormat;
    }
}
class DateFormat3 implements DateFormat
{
    public String giveDateFormat()
    {
        String dateFormat;
        dateFormat = "yyyy-MM-dd";
        return dateFormat;
    }
}
