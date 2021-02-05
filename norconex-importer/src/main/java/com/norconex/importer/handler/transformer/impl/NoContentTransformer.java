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
package com.norconex.importer.handler.transformer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.AbstractDocumentTransformer;

/**
 * <p>Get rid of the content stream and optionally store it as text into a
 * metadata field instead.
 * </p>
 * <h3>Storing content in an existing field</h3>
 * <p>
 * If a <code>toField</code> with the same name already exists for a document,
 * the value will be added to the end of the existing value list.
 * <p>
 * This class can be used both as a pre-parsing or post-parsing handler. To
 * store the content in a field, make sure pre-parsing is of a text
 * content-types.</p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.NoContentTransformer"
 *      toField="(Optionally store content into a field.)"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/transformer&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.NoContentTransformer"/&gt;
 * </pre>
 *
 * <p>
 * The above example removes the content of all documents (leaving you with
 * metadata only).
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.11.0
 */
public class NoContentTransformer extends AbstractDocumentTransformer
       implements IXMLConfigurable {

    private String toField;

    public String getToField() {
        return toField;
    }
    public void setToField(String toField) {
        this.toField = toField;
    }

    @Override
    protected void transformApplicableDocument(String reference,
            InputStream input, OutputStream output, ImporterMetadata metadata,
            boolean parsed) throws ImporterHandlerException {
        try {
            if (StringUtils.isNotBlank(toField)) {
                metadata.addString(toField,
                        IOUtils.toString(input, StandardCharsets.UTF_8));
            }
            output.write(new byte[] {});
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Could not remove content for: " + reference, e);
        }
    }


    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setToField(xml.getString("[@toField]", toField));

    }
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("toField", toField);
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
