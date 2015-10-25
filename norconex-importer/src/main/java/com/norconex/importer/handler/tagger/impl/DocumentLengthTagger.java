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

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Adds the document length (i.e., number of bytes) to 
 * the specified <code>field</code>. The length is the document 
 * content length as it is in its current processing stage. If for 
 * instance you set this tagger after a transformer that modifies the content,
 * the obtained length will be for the modified content, and not the
 * original length. To obtain a document's length before any modification
 * was made to it, use this tagger as one of the first
 * handler in your pre-parse handlers.</p>
 * 
 * <p>If <code>field</code> already has one or more values, 
 * the length will be <i>added</i> to the list of 
 * existing values, unless "overwrite" is set to <code>true</code>.</p>
 * 
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * 
 * <p>XML configuration usage:</p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DocumentLengthTagger"
 *      field="(mandatory target field)"
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
public class DocumentLengthTagger extends AbstractDocumentTagger {

    private String field;
    private boolean overwrite;
    
    @Override
    protected void tagApplicableDocument(String reference,
            InputStream document, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        if (StringUtils.isBlank(field)) {
            throw new IllegalArgumentException("\"field\" cannot be empty.");
        }
        
        int length = -1;
        if (document instanceof CachedInputStream) {
            length = ((CachedInputStream) document).length();
        } else {
            CountingInputStream is = new CountingInputStream(document);
            try {
                IOUtils.copy(is, new NullOutputStream());
            } catch (IOException e) {
                throw new ImporterHandlerException(e);
            }
            length = is.getCount();
        }
        if (overwrite) {
            metadata.setInt(field, length);
        } else {
            metadata.addInt(field, length);
        }
    }

    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
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
        overwrite = xml.getBoolean("[@overwrite]", overwrite);
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("field", field);
        writer.writeAttributeBoolean("overwrite", overwrite);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = 
                new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        builder.append("field", field);
        builder.append("overwrite", overwrite);
        return builder.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DocumentLengthTagger)) {
            return false;
        }
        DocumentLengthTagger castOther = (DocumentLengthTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(other))
                .append(field, castOther.field)
                .append(overwrite, castOther.overwrite)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(field)
                .append(overwrite)
                .toHashCode();
    }
}
