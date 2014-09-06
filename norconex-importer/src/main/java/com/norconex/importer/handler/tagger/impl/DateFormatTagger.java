/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
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

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.IDocumentTagger;

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
 * <p/>
 * Can be used both as a pre-parse or post-parse handler.
 * <p/>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DateFormatTagger"
 *      fromField="(from field)" toField="(to field)" 
 *      fromFormat="(date format)" toFormat="(date format)"
 *      keepBadDates="(false|true)" overwrite="[false|true]" /&gt
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class DateFormatTagger implements IDocumentTagger, IXMLConfigurable {

    private static final long serialVersionUID = -3380117073554862363L;

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
    public void tagDocument(String reference, InputStream document,
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
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
        fromField = xml.getString("[@fromField]", fromField);
        toField = xml.getString("[@toField]", toField);
        fromFormat = xml.getString("[@fromFormat]", fromFormat);
        toFormat = xml.getString("[@toFormat]", toFormat);
        overwrite = xml.getBoolean("[@overwrite]", overwrite);
        keepBadDates = xml.getBoolean("[@keepBadDates]", keepBadDates);
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            writer.writeStartElement("tagger");
            writer.writeAttribute("class", getClass().getCanonicalName());

            writer.writeAttributeString("fromField", fromField);
            writer.writeAttributeString("toField", toField);
            writer.writeAttributeString("fromFormat", fromFormat);
            writer.writeAttributeString("toFormat", toFormat);
            writer.writeAttributeBoolean("overwrite", overwrite);
            writer.writeAttributeBoolean("keepBadDates", keepBadDates);
            
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((fromField == null) ? 0 : fromField.hashCode());
        result = prime * result
                + ((fromFormat == null) ? 0 : fromFormat.hashCode());
        result = prime * result + (keepBadDates ? 1231 : 1237);
        result = prime * result + (overwrite ? 1231 : 1237);
        result = prime * result + ((toField == null) ? 0 : toField.hashCode());
        result = prime * result
                + ((toFormat == null) ? 0 : toFormat.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DateFormatTagger)) {
            return false;
        }
        DateFormatTagger other = (DateFormatTagger) obj;
        if (fromField == null) {
            if (other.fromField != null) {
                return false;
            }
        } else if (!fromField.equals(other.fromField)) {
            return false;
        }
        if (fromFormat == null) {
            if (other.fromFormat != null) {
                return false;
            }
        } else if (!fromFormat.equals(other.fromFormat)) {
            return false;
        }
        if (keepBadDates != other.keepBadDates) {
            return false;
        }
        if (overwrite != other.overwrite) {
            return false;
        }
        if (toField == null) {
            if (other.toField != null) {
                return false;
            }
        } else if (!toField.equals(other.toField)) {
            return false;
        }
        if (toFormat == null) {
            if (other.toFormat != null) {
                return false;
            }
        } else if (!toFormat.equals(other.toFormat)) {
            return false;
        }
        return true;
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
    
    
}
