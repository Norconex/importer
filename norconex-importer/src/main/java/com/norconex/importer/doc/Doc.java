/* Copyright 2014-2020 Norconex Inc.
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
package com.norconex.importer.doc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.ImporterRuntimeException;

/**
 * A document being imported.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class Doc {


    //TODO Move these to a new "DocMetadata" constant class?
    //TODO DELETE these if they can be referenced from DocInfo?
    //    (still have them as metadata, just no longer need constants).
    private static final String DOC_META_PREFIX = "document.";
    public static final String DOC_REFERENCE = DOC_META_PREFIX + "reference";
    public static final String DOC_CONTENT_TYPE =
            DOC_META_PREFIX + "contentType";
    public static final String DOC_CONTENT_ENCODING =
            DOC_META_PREFIX + "contentEncoding";
    public static final String DOC_CONTENT_FAMILY =
            DOC_META_PREFIX + "contentFamily";
    public static final String DOC_LANGUAGE =
            DOC_META_PREFIX + "language";
    public static final String DOC_TRANSLATED_FROM =
            DOC_META_PREFIX + "translatedFrom";
    public static final String DOC_GENERATED_TITLE =
            DOC_META_PREFIX + "generatedTitle";
    public static final String DOC_IMPORTED_DATE =
            DOC_META_PREFIX + "importedDate";
    static final String DOC_EMBEDDED_META_PREFIX =
            DOC_META_PREFIX + "embedded.";
    public static final String DOC_EMBEDDED_PARENT_REFERENCE =
            DOC_EMBEDDED_META_PREFIX + "parent.reference";
    public static final String DOC_EMBEDDED_PARENT_ROOT_REFERENCE =
            DOC_EMBEDDED_META_PREFIX + "parent.root.reference";
    public static final String DOC_EMBEDDED_REFERENCE =
            DOC_EMBEDDED_META_PREFIX + "reference";
    public static final String DOC_EMBEDDED_TYPE =
            DOC_EMBEDDED_META_PREFIX + "type";


    //TODO still allow String reference in constructor and create
    // new DocInfo?

    //TODO add parent reference info here???

    // Rename Doc and rename Properties to DocMetadata?

    private final DocInfo docInfo;
    private CachedInputStream content;
    private Properties metadata;

    public Doc(String reference, CachedInputStream content) {
        this(reference, content, null);
    }
    public Doc(String reference, CachedInputStream content,
            Properties metadata) {
        this(new DocInfo(Objects.requireNonNull(reference,
                "'reference' must not be null.")), content, metadata);
    }

    /**
     * Creates a blank importer document using the supplied input stream
     * to handle content.
     * The document reference automatically gets added to the metadata.
     * @param docInfo document details
     * @param content content input stream
     * @since 3.0.0
     */
    public Doc(DocInfo docInfo, CachedInputStream content) {
        this(docInfo, content, null);
    }
    /**
     * Creates a blank importer document using the supplied input stream
     * to handle content.
     * The document reference automatically gets added to the metadata.
     * @param docInfo document details
     * @param content content input stream
     * @param metadata importer document metadata
     * @since 3.0.0
     */
    public Doc(DocInfo docInfo, CachedInputStream content,
            Properties metadata) {
        Objects.requireNonNull(docInfo, "'docInfo' must not be null.");
        Objects.requireNonNull(content, "'content' must not be null.");
        this.docInfo = docInfo;
        this.content = content;
        if (metadata == null) {
            this.metadata = new Properties();
        } else {
            this.metadata = metadata;
        }
        this.metadata.set(Doc.DOC_REFERENCE, docInfo.getReference());
    }

    /**
     * Disposes of any resources associated with this document (like
     * disk or memory cache).
     * @throws IOException could not dispose of document resources
     * @since 3.0.0
     */
    //TODO implement "closeable" instead?
    public synchronized void dispose() throws IOException {
        content.dispose();
    }

    /**
     * Gets the document content.
     * @return input stream
     * @deprecated Since 3.0.0, use {@link #getInputStream()}
     */
    @Deprecated
    public CachedInputStream getContent() {
        return getInputStream();
    }
    /**
     * Sets the document content.
     * @param content cached input stream
     * @deprecated Since 3.0.0, use {@link #setInputStream(InputStream)}
     */
    @Deprecated
    public void setContent(CachedInputStream content) {
        setInputStream(content);
    }

    //TODO Since 3.0.0
    public CachedInputStream getInputStream() {
        content.rewind();
        return content;
    }
    //TODO Since 3.0.0
    public void setInputStream(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "'inputStream' must not be null.");
        if (this.content == inputStream) {
            return;
        }
        try {
            this.content.dispose();
            if (inputStream instanceof CachedInputStream) {
                this.content = (CachedInputStream) inputStream;
            } else {
                CachedOutputStream os =
                        this.content.getStreamFactory().newOuputStream();
                IOUtils.copy(inputStream, os);
                this.content = os.getInputStream();
            }
        } catch (IOException e) {
            throw new ImporterRuntimeException(
                    "Could set content input stream.", e);
        }
    }
    //TODO Since 3.0.0
    public CachedStreamFactory getStreamFactory() {
        return content.getStreamFactory();
    }

    public DocInfo getDocInfo() {
        return docInfo;
    }

    public Properties getMetadata() {
        return metadata;
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

    //TODO Deprecate all below?

    /**
     * Gets the content encoding.
     * @return content encoding
     */
    public String getContentEncoding() {
        return docInfo.getContentEncoding();
    }
    /**
     * Sets the content encoding.
     * @param contentEncoding content encoding
     * @deprecated Since 3.0.0, use {@link #getDocInfo()}
     */
    @Deprecated
    public void setContentEncoding(String contentEncoding) {
        this.docInfo.setContentEncoding(contentEncoding);
    }
    /**
     * Gets the content type.
     * @return content type
     */
    public ContentType getContentType() {
        return docInfo.getContentType();
    }
    /**
     * Sets the content type.
     * @param contentType content type
     * @deprecated Since 3.0.0, use {@link #getDocInfo()}
     */
    @Deprecated
    public void setContentType(ContentType contentType) {
        this.docInfo.setContentType(contentType);
    }
    /**
     * Gets the document reference.
     * @return reference
     */
    public String getReference() {
        return docInfo.getReference();
    }
    /**
     * Sets the document reference.
     * @param reference reference
     * @deprecated Since 3.0.0, use {@link #getDocInfo()}
     */
    @Deprecated
    public void setReference(String reference) {
        this.docInfo.setReference(reference);
    }
}
