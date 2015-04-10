/* Copyright 2015 Norconex Inc.
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

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
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
 * <p>XML configuration usage:</p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.CurrentDateTagger"
 *      field="(target field)"
 *      format="(date format)"
 *      overwrite="[false|true]" &gt;
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
 * @since 2.2.0
 */
public class CurrentDateTagger extends AbstractDocumentTagger {

    public static final String DEFAULT_FIELD = 
            ImporterMetadata.DOC_IMPORTED_DATE;
    
    private String field = DEFAULT_FIELD;
    private String format;
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
        return new SimpleDateFormat(format).format(new Date(time));
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
    
    public boolean isOverwrite() {
        return overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        field = xml.getString("[@field]", field);
        format = xml.getString("[@format]", format);
        overwrite = xml.getBoolean("[@overwrite]", overwrite);
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("field", field);
        writer.writeAttributeString("format", format);
        writer.writeAttributeBoolean("overwrite", overwrite);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("field", field);
        builder.append("format", format);
        builder.append("overwrite", overwrite);
        return builder.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CurrentDateTagger))
            return false;
        CurrentDateTagger castOther = (CurrentDateTagger) other;
        return new EqualsBuilder().appendSuper(super.equals(other))
                .append(field, castOther.field)
                .append(format, castOther.format)
                .append(overwrite, castOther.overwrite)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode())
                .append(field).append(format).append(overwrite)
                .toHashCode();
    }
}
