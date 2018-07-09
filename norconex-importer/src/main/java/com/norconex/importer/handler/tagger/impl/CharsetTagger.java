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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.tika.utils.CharsetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.util.CharsetUtil;

/**
 * <p>
 * Converts one or more field values (if needed) from a source character
 * encoding (charset) to a target one. Both the source and target character
 * encodings are optional. If no source character encoding is explicitly
 * provided, it first tries to detect the encoding of the field values
 * before converting them to the target encoding. If the source
 * character encoding cannot be established, the content encoding will remain
 * unchanged. When no target character encoding is specified, UTF-8 is assumed.
 * </p>
 *
 * <h3>Should I use this tagger?</h3>
 * <p>
 * Before using this tagger, you need to know the parsing of documents
 * by the importer using default document parser factory will try to convert
 * and return fields as UTF-8 (for most, if not all content-types).
 * If UTF-8 is your desired target, it only make sense to use this tagger
 * as a pre-parsing handler (for text content-types only) when it is important
 * to work with a specific character encoding before parsing.
 * If on the other hand you wish to convert to a character encoding to a
 * target different than UTF-8, you can use this tagger as a post-parsing
 * handler to do so.
 * </p>
 *
 * <h3>Conversion is not flawless</h3>
 * <p>
 * Because character encoding detection is not always accurate and because
 * documents sometime mix different encoding, there is no guarantee this
 * class will handle ALL character encoding conversions properly.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CharsetTagger"
 *          sourceCharset="(character encoding)"
 *          targetCharset="(character encoding)"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;fieldsRegex&gt;(regex matching fields to detect encoding)&lt;/fieldsRegex&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * Converts the characters of a "description" field from "ISO-8859-1"
 * to "UTF-8".
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.CharsetTagger"
 *          sourceCharset="ISO-8859-1" targetCharset="UTF-8"&gt;
 *      &lt;fieldsRegex&gt;description&lt;/fieldsRegex&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.5.0
 */
public class CharsetTagger extends AbstractDocumentTagger
        implements IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(CharsetTagger.class);

    public static final String DEFAULT_TARGET_CHARSET =
            StandardCharsets.UTF_8.toString();

    private String targetCharset = DEFAULT_TARGET_CHARSET;
    private String sourceCharset = null;
    private String fieldsRegex;

    @Override
    protected void tagApplicableDocument(String reference,
            InputStream document, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (StringUtils.isBlank(fieldsRegex)) {
            throw new ImporterHandlerException(
                    "\"fieldsRegex\" cannot be blank on CharsetTagger.");
        }

        String[] metaFields = metadata.keySet().toArray(
                ArrayUtils.EMPTY_STRING_ARRAY);
        Pattern pattern = Pattern.compile(fieldsRegex, Pattern.DOTALL
                | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        for (String metaField : metaFields) {
            if (pattern.matcher(metaField).matches()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Field to convert charset: " + metaField);
                }
                convertCharset(reference, metadata, metaField);
            }
        }
    }


    public String getFieldsRegex() {
        return fieldsRegex;
    }
    public void setFieldsRegex(String fieldsRegex) {
        this.fieldsRegex = fieldsRegex;
    }

    private void convertCharset(
            String reference, ImporterMetadata metadata, String metaField) {
        List<String> values = metadata.get(metaField);
        if (values == null) {
            return;
        }
        String declaredEncoding =
                metadata.getString(ImporterMetadata.DOC_CONTENT_ENCODING);
        List<String> newValues = new ArrayList<>();
        for (String value : values) {
            String newValue = value;
            try {
                newValue = convertCharset(reference, value, declaredEncoding);
            } catch (IOException e) {
                LOG.warn("Cannot detect source encoding for value \""
                        + StringUtils.abbreviate(value, 0, 200)
                        + "\". Encoding will remain unchanged. Reference: "
                        + reference, e);
            }
            newValues.add(newValue);
        }
        metadata.setString(metaField,
                newValues.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
    }

    private String convertCharset(
            String reference, String value, String declaredEncoding)
            throws IOException {

        //--- Get source charset ---
        String inputCharset = sourceCharset;
        if (StringUtils.isBlank(inputCharset)) {
            inputCharset = CharsetUtil.detectCharset(value, declaredEncoding);
        }
        // Do not attempt conversion of no source charset is found
        if (StringUtils.isBlank(inputCharset)) {
            return value;
        }
        inputCharset = CharsetUtils.clean(inputCharset);

        //--- Get target charset ---
        String outputCharset = targetCharset;
        if (StringUtils.isBlank(outputCharset)) {
            outputCharset = StandardCharsets.UTF_8.toString();
        }
        outputCharset = CharsetUtils.clean(outputCharset);

        // Do not proceed if encoding is already what we want
        if (inputCharset.equals(outputCharset)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Source and target encodings are the same for "
                        + reference);
            }
            return value;
        }

        //--- Convert ---
        try {
            value = CharsetUtil.convertCharset(
                    value, inputCharset, outputCharset);
        } catch (IOException e) {
            LOG.warn("Cannot convert character encoding from " + inputCharset
                    + " to " + outputCharset
                    + ". Encoding will remain unchanged. "
                    + "Reference: " + reference, e);
        }
        return value;
    }

    public String getTargetCharset() {
        return targetCharset;
    }
    public void setTargetCharset(String targetCharset) {
        this.targetCharset = targetCharset;
    }

    public String getSourceCharset() {
        return sourceCharset;
    }
    public void setSourceCharset(String sourceCharset) {
        this.sourceCharset = sourceCharset;
    }

    @Override
    protected void loadHandlerFromXML(XML xml) throws IOException {
        setSourceCharset(xml.getString("@sourceCharset", getSourceCharset()));
        setTargetCharset(xml.getString("@targetCharset", getTargetCharset()));
        setFieldsRegex(xml.getString("fieldsRegex", fieldsRegex));
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("sourceCharset", getSourceCharset());
        writer.writeAttributeString("targetCharset", getTargetCharset());
        writer.writeElementString("fieldsRegex", fieldsRegex);
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
