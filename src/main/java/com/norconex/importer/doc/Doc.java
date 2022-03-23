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
import org.apache.commons.lang3.builder.ToStringExclude;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.builder.ToStringSummary;

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

    //TODO still allow String reference in constructor and create

    //TODO add parent reference info here???

    private final DocInfo docInfo;
    @ToStringSummary
    private final Properties metadata;
    @ToStringExclude
    private CachedInputStream content;

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
    // nullsafe
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

    /**
     * Gets the document reference. Same as
     * invoking {@link DocInfo#getDocumentReference()}.
     * @return reference
     * @see #getDocInfo()
     */
    public String getReference() {
        return docInfo.getDocumentReference();
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
        ReflectionToStringBuilder b = new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
        b.setExcludeNullValues(true);
        return b.toString();

    }
}
