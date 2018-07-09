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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Adds the current computer UTC date to the specified <code>field</code>.  
 * If no <code>field</code> is provided, the date will be added to 
 * <code>document.importedDate</code>.
 * </p>
 * <p>
 * The default date format is EPOCH
 * (the difference, measured in milliseconds, between the current time and 
 * midnight, January 1, 1970 UTC).
 * A custom date format can be specified with the <code>format</code> 
 * attribute, as per the 
 * formatting options found on {@link SimpleDateFormat}.
 * </p>
 * 
 * <p>If <code>field</code> already has one or more values, 
 * the new date will be <i>added</i> to the list of 
 * existing values, unless "overwrite" is set to <code>true</code>.</p>
 * 
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * 
 * <p>Since 2.5.2, it is possible to specify a locale used for formatting
 * dates. The locale is the ISO two-letter language code, 
 * with an optional ISO country code, separated with an underscore 
 * (e.g., "fr" for French, "fr_CA" for Canadian French). When no locale is 
 * specified, the default is "en_US" (US English).</p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CurrentDateTagger"
 *      field="(target field)"
 *      format="(date format)"
 *      locale="(locale)"
 *      overwrite="[false|true]" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/handler&gt;
 * </pre>
 * 
 * <h4>Usage example:</h4>
 * <p>
 * The following will store the current date along with hours and minutes
 * in a "crawl_date" field. 
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CurrentDateTagger"
 *      field="crawl_date" format="yyyy-MM-dd HH:mm" /&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.2.0
 */
public class CurrentDateTagger extends AbstractDocumentTagger {

    public static final String DEFAULT_FIELD = 
            ImporterMetadata.DOC_IMPORTED_DATE;
    
    private String field = DEFAULT_FIELD;
    private String format;
    private Locale locale;
    private boolean overwrite;
    
    /**
     * Constructor.
     */
    public CurrentDateTagger() {
        super();
    }

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        String date = formatDate(System.currentTimeMillis());
        String finalField = field;
        if (StringUtils.isBlank(finalField)) {
            finalField = DEFAULT_FIELD;
        }
        if (overwrite) {
            metadata.setString(finalField, date);
        } else {
            metadata.addString(finalField, date);
        }
    }
    
    private String formatDate(long time) {
        if (StringUtils.isBlank(format)) {
            return Long.toString(time);
        }
        Locale safeLocale = locale;
        if (safeLocale == null) {
            safeLocale = Locale.US;
        }
        return new SimpleDateFormat(
                format, safeLocale).format(new Date(time));
    }

    public String getField() {
        return field;
    }
    public void setField(String toField) {
        this.field = toField;
    }

    public String getFormat() {
        return format;
    }
    public void setFormat(String toFormat) {
        this.format = toFormat;
    }

    /**
     * Gets the locale used for formatting. 
     * @return locale
     * @since 2.5.2
     */
    public Locale getLocale() {
        return locale;
    }
    /**
     * Sets the locale used for formatting.
     * @param locale locale
     * @since 2.5.2
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public boolean isOverwrite() {
        return overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    protected void loadHandlerFromXML(XML xml) throws IOException {
        field = xml.getString("@field", field);
        format = xml.getString("@format", format);
        String localeStr = xml.getString("@locale", null);
        if (StringUtils.isNotBlank(localeStr)) {
            setLocale(LocaleUtils.toLocale(localeStr));
        }
        overwrite = xml.getBoolean("@overwrite", overwrite);
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("field", field);
        writer.writeAttributeString("format", format);
        writer.writeAttributeBoolean("overwrite", overwrite);
        if (locale != null) {
            writer.writeAttributeString("locale", locale.toString());
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
