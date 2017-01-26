/* Copyright 2014-2017 Norconex Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.util.FormatUtil;

/**
 * <p>Formats a date from any given format to a format of choice, as per the 
 * formatting options found on {@link SimpleDateFormat} with the exception 
 * of the string "EPOCH" which represents the difference, measured in 
 * milliseconds, between the date and midnight, January 1, 1970.  
 * The default format
 * for <code>fromFormat</code> or <code>toFormat</code> when not specified
 * is EPOCH.</p>
 * 
 * <p>When omitting the <code>toField</code>, the value will replace the one
 * in the same field.</p>
 * 
 * <p>If the <code>toField</code> already
 * exists, the newly formatted date will be <i>added</i> to the list of 
 * existing values, unless "overwrite" is set to <code>true</code>.</p>
 * 
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * 
 * <p><b>Since 2.5.2</b>, it is possible to specify a locale used for parsing
 * and formatting dates. 
 * The locale is the ISO two-letter language code, with an optional
 * ISO country code, separated with an underscore (e.g., "fr" for French, 
 * "fr_CA" for Canadian French). When no locale is specified, the default is 
 * "en_US" (US English).</p> 
 * 
 * <p>
 * <b>Since 2.6.0</b>, it is possible to specify multiple 
 * <code>fromFormat</code> values. Each formats will be tried in the order
 * provided and the first format that succeed in parsing a date will be used.
 * A date will be considered "bad" only if none of the formats could parse the 
 * date.
 * </p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DateFormatTagger"
 *          fromField="(from field)" toField="(to field)" 
 *          fromLocale="(locale)"    toLocale="(locale)"
 *          toFormat="(date format)"
 *          keepBadDates="(false|true)"
 *          overwrite="[false|true]" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *      
 *      &lt;fromFormat&gt;(date format)&lt;/fromFormat&gt;
 *      &lt;!-- multiple "fromFormat" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * <h4>Usage example:</h4>
 * <p>
 * The following converts a date that is sometimes obtained from the 
 * HTTP header "Last-Modified" and sometimes is an EPOCH date, 
 * into an Apache Solr date format:
 * </p> 
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DateFormatTagger"
 *          fromField="Last-Modified"
 *          toField="solr_date" 
 *          toFormat="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" &gt;
 *      &lt;fromFormat&gt;EEE, dd MMM yyyy HH:mm:ss zzz&lt;/fromFormat&gt;
 *      &lt;fromFormat&gt;EPOCH&lt;/fromFormat&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class DateFormatTagger extends AbstractDocumentTagger {

    private static final Logger LOG = 
            LogManager.getLogger(DateFormatTagger.class);
    
    private String fromField;
    private String toField;
    private String[] fromFormats;
    private String toFormat;
    private Locale fromLocale;
    private Locale toLocale;
    private boolean overwrite;
    private boolean keepBadDates;
    
    /**
     * Constructor.
     */
    public DateFormatTagger() {
        super();
    }

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        validateArguments();

        List<String> fromDates = metadata.getStrings(fromField);
        List<String> toDates = new ArrayList<>(fromDates.size());
        for (String fromDate : fromDates) {
            String toDate = formatDate(fromDate);
            if (StringUtils.isNotBlank(toDate)) {
                toDates.add(toDate);
            } else if (keepBadDates) {
                toDates.add(fromDate);
            }
        }
        
        String finalToField = toField;
        if (StringUtils.isBlank(finalToField)) {
            finalToField = fromField;
        }
        if (overwrite) {
            metadata.put(finalToField, toDates);
        } else {
            metadata.addString(finalToField, 
                    toDates.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        }
    }
    
    private String formatDate(String fromDate) {
        String[] formats = fromFormats; 
        if (ArrayUtils.isEmpty(fromFormats)) {
            formats = new String[] { "EPOCH" };
        }
        for (String fromFormat : formats) {
            String toDate = FormatUtil.formatDateString(
                    fromDate, fromFormat, fromLocale, 
                    toFormat, toLocale, fromField);
            if (StringUtils.isNotBlank(toDate)) {
                return toDate;
            }
        }
        return null;
    }
    
    
    public String getFromField() {
        return fromField;
    }
    public void setFromField(String fromField) {
        this.fromField = fromField;
    }

    public String getToField() {
        return toField;
    }
    public void setToField(String toField) {
        this.toField = toField;
    }

    /**
     * Gets the source format to match.
     * @return source date formats
     * @deprecated Since 2.6.0, user {@link #getFromFormats()}
     */
    @Deprecated
    public String getFromFormat() {
        if (ArrayUtils.isEmpty(fromFormats)) {
            return null;
        }
        return fromFormats[0];
    }
    /**
     * Sets the source format to match.
     * @param fromFormat source date formats
     * @deprecated Since 2.6.0, user {@link #setFromFormats(String...)}
     */
    @Deprecated
    public void setFromFormat(String fromFormat) {
        setFromFormats(fromFormats);
    }

    /**
     * Gets the source date formats to match.
     * @return source date formats
     * @since 2.6.0
     */
    public String[] getFromFormats() {
        return fromFormats;
    }
    /**
     * Sets the source date formats to match.
     * @param fromFormats source date formats
     * @since 2.6.0
     */
    public void setFromFormats(String... fromFormats) {
        this.fromFormats = fromFormats;
    }

    public String getToFormat() {
        return toFormat;
    }
    public void setToFormat(String toFormat) {
        this.toFormat = toFormat;
    }
    
    public boolean isOverwrite() {
        return overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isKeepBadDates() {
        return keepBadDates;
    }
    public void setKeepBadDates(boolean keepBadDates) {
        this.keepBadDates = keepBadDates;
    }

    /**
     * Gets the locale used for parsing the source date. 
     * @return locale
     * @since 2.5.2
     */
    public Locale getFromLocale() {
        return fromLocale;
    }
    /**
     * Sets the locale used for parsing the source date.
     * @param fromLocale locale
     * @since 2.5.2
     */    
    public void setFromLocale(Locale fromLocale) {
        this.fromLocale = fromLocale;
    }

    /**
     * Gets the locale used for formatting the target date. 
     * @return locale
     * @since 2.5.2
     */
    public Locale getToLocale() {
        return toLocale;
    }
    /**
     * Sets the locale used for formatting the source date.
     * @param toLocale locale
     * @since 2.5.2
     */    
    public void setToLocale(Locale toLocale) {
        this.toLocale = toLocale;
    }

    private void validateArguments() {
        if (StringUtils.isBlank(fromField)) {
            throw new IllegalArgumentException(
                    "\"fromField\" cannot be empty.");
        }
        if (StringUtils.isBlank(fromField) && StringUtils.isBlank(toField)) {
            throw new IllegalArgumentException(
                    "One of \"fromField\" or \"toField\" is required.");
        }
    }
    
    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        fromField = xml.getString("[@fromField]", fromField);
        toField = xml.getString("[@toField]", toField);
        
        String fromFormat = xml.getString("[@fromFormat]", null);
        if (StringUtils.isNotBlank(fromFormat)) {
            LOG.warn("\"fromFormat\" attribute is now deprecated and is "
                    + "now specified as a repeatable <fromFormat> tag.");
            setFromFormats(fromFormat);
        }

        toFormat = xml.getString("[@toFormat]", toFormat);
        overwrite = xml.getBoolean("[@overwrite]", overwrite);
        keepBadDates = xml.getBoolean("[@keepBadDates]", keepBadDates);
        String fromLocaleStr = xml.getString("[@fromLocale]", null);
        if (StringUtils.isNotBlank(fromLocaleStr)) {
            setFromLocale(LocaleUtils.toLocale(fromLocaleStr));
        }
        String toLocaleStr = xml.getString("[@toLocale]", null);
        if (StringUtils.isNotBlank(toLocaleStr)) {
            setToLocale(LocaleUtils.toLocale(toLocaleStr));
        }

        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("fromFormat");
        if (!nodes.isEmpty()) {
            List<String> fromFormats = new ArrayList<>(nodes.size());
            for (HierarchicalConfiguration node : nodes) {
                fromFormats.add(node.getString("", null));
            }
            setFromFormats(fromFormats.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("fromField", fromField);
        writer.writeAttributeString("toField", toField);
        writer.writeAttributeString("toFormat", toFormat);
        writer.writeAttributeBoolean("overwrite", overwrite);
        writer.writeAttributeBoolean("keepBadDates", keepBadDates);
        if (fromLocale != null) {
            writer.writeAttributeString("fromLocale", fromLocale.toString());
        }        
        if (toLocale != null) {
            writer.writeAttributeString("toLocale", toLocale.toString());
        }
        
        if (getFromFormats() != null) {
            for (String fromFormat : fromFormats) {
                writer.writeElementString("fromFormat", fromFormat);
            }
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("fromField", fromField)
                .append("toField", toField)
                .append("fromFormats", fromFormats)
                .append("toFormat", toFormat)
                .append("overwrite", overwrite)
                .append("keepBadDates", keepBadDates)
                .append("fromLocale", fromLocale)
                .append("toLocale", toLocale)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DateFormatTagger))
            return false;
        DateFormatTagger castOther = (DateFormatTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(other))
                .append(fromField, castOther.fromField)
                .append(toField, castOther.toField)
                .append(fromFormats, castOther.fromFormats)
                .append(toFormat, castOther.toFormat)
                .append(overwrite, castOther.overwrite)
                .append(keepBadDates, castOther.keepBadDates)
                .append(fromLocale, castOther.fromLocale)
                .append(toLocale, castOther.toLocale)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(fromField)
                .append(toField)
                .append(fromFormats)
                .append(toFormat)
                .append(overwrite)
                .append(keepBadDates)
                .append(fromLocale)
                .append(toLocale)
                .toHashCode();
    }
    
    
}
