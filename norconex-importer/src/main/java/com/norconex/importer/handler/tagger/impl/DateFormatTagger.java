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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * Formats a date from any given format to a format of choice, as per the 
 * formatting options found on {@link SimpleDateFormat}.  The default format
 * for <code>fromFormat</code> or <code>toFormat</code> when not specified
 * is EPOCH.  
 * <p />
 * When omitting the <code>toField</code>, the value will replace the one
 * in the same field.
 * <p />
 * If the <code>toField</code> already
 * exists, the newly formatted date will be <i>added</i> to the list of 
 * existing values, unless "overwrite" is set to <code>true</code>. 
 * <br><br>
 * Can be used both as a pre-parse or post-parse handler.
 * <br><br>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DateFormatTagger"
 *      fromField="(from field)" toField="(to field)" 
 *      fromFormat="(date format)" toFormat="(date format)"
 *      keepBadDates="(false|true)" overwrite="[false|true]" &gt
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class DateFormatTagger extends AbstractDocumentTagger {

    private static final Logger LOG = 
            LogManager.getLogger(CharacterCaseTagger.class);
    
    private String fromField;
    private String toField;
    private String fromFormat;
    private String toFormat;
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
        if (StringUtils.isBlank(fromDate)) {
            return null;
        }
        
        //--- Parse from date ---
        Date date = null;
        if (StringUtils.isBlank(fromFormat)) {
            // From date format is EPOCH
            long millis = NumberUtils.toLong(fromDate, -1);
            if (millis == -1) {
                LOG.warn("Invalid date format found in " + fromField
                        + ". When no \"fromFormat\" is specified, the date "
                        + "value is expected to be of EPOCH format.");
                return null;
            }
            date = new Date(millis);
        } else {
            // From date is custom format
            try {
                date = new SimpleDateFormat(fromFormat).parse(fromDate);
            } catch (ParseException e) {
                LOG.warn("Invalid date format found in " + fromField + ".", e);
                return null;
            }
        }

        //--- Format to date ---
        String toDate = null;
        if (StringUtils.isBlank(toFormat)) {
            // To date foramt is EPOCH
            toDate = Long.toString(date.getTime());
        } else {
            toDate = new SimpleDateFormat(toFormat).format(date);
        }
        return toDate;
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

    public String getFromFormat() {
        return fromFormat;
    }
    public void setFromFormat(String fromFormat) {
        this.fromFormat = fromFormat;
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
        fromFormat = xml.getString("[@fromFormat]", fromFormat);
        toFormat = xml.getString("[@toFormat]", toFormat);
        overwrite = xml.getBoolean("[@overwrite]", overwrite);
        keepBadDates = xml.getBoolean("[@keepBadDates]", keepBadDates);
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("fromField", fromField);
        writer.writeAttributeString("toField", toField);
        writer.writeAttributeString("fromFormat", fromFormat);
        writer.writeAttributeString("toFormat", toFormat);
        writer.writeAttributeBoolean("overwrite", overwrite);
        writer.writeAttributeBoolean("keepBadDates", keepBadDates);
    }

    

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("fromField", fromField);
        builder.append("toField", toField);
        builder.append("fromFormat", fromFormat);
        builder.append("toFormat", toFormat);
        builder.append("overwrite", overwrite);
        builder.append("keepBadDates", keepBadDates);
        return builder.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DateFormatTagger))
            return false;
        DateFormatTagger castOther = (DateFormatTagger) other;
        return new EqualsBuilder().appendSuper(super.equals(other))
                .append(fromField, castOther.fromField)
                .append(toField, castOther.toField)
                .append(fromFormat, castOther.fromFormat)
                .append(toFormat, castOther.toFormat)
                .append(overwrite, castOther.overwrite)
                .append(keepBadDates, castOther.keepBadDates).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode())
                .append(fromField).append(toField).append(fromFormat)
                .append(toFormat).append(overwrite).append(keepBadDates)
                .toHashCode();
    }
    
    
}
