/* Copyright 2014-2018 Norconex Inc.
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

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.importer.ImporterRuntimeException;

/**
 * A document being imported.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterDocument {

    //TODO add parent reference info here???

    private String reference;
    private CachedInputStream content;
    private ImporterMetadata metadata;
    private ContentType contentType;
    private String contentEncoding;

    /**
     *
     * @param reference
     * @param streamFactory
     * @since 3.0.0
     */
    public ImporterDocument(
            String reference, CachedStreamFactory streamFactory) {
        Objects.requireNonNull(
                streamFactory, "'streamFactory' must not be null.");
        init(reference, streamFactory.newInputStream(), null);
    }

    /**
     *
     * @param reference
     * @param streamFactory
     * @param metadata importer metadata
     * @since 3.0.0
     */
    public ImporterDocument(String reference,
            CachedStreamFactory streamFactory, ImporterMetadata metadata) {
        Objects.requireNonNull(
                streamFactory, "'streamFactory' must not be null.");
        init(reference, streamFactory.newInputStream(), metadata);
    }

    public ImporterDocument(String reference, CachedInputStream content) {
        init(reference, content, null);
    }
    public ImporterDocument(String reference, CachedInputStream content,
            ImporterMetadata metadata) {
        init(reference, content, metadata);
    }

    private void init(String reference,
            CachedInputStream content, ImporterMetadata metadata) {
        Objects.requireNonNull(content, "'content' must not be null.");
        Objects.requireNonNull(reference, "'reference' must not be null.");
        this.reference = reference;
        this.content = content;
        if (metadata == null) {
            this.metadata = new ImporterMetadata();
        } else {
            this.metadata = metadata;
        }
    }

    /**
     * Disposes of any resources associated with this document (like
     * disk or memory cache).
     * @throws IOException
     * @since 3.0.0
     */
    //TODO implement "closeable" instead?
    public synchronized void dispose() throws IOException {
        content.dispose();
    }

    public ContentType getContentType() {
        return contentType;
    }
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getReference() {
        return reference;
    }
    public void setReference(String reference) {
        Objects.requireNonNull(reference, "'reference' must not be null.");
        this.reference = reference;
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

    public String getContentEncoding() {
        return contentEncoding;
    }
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }
    public ImporterMetadata getMetadata() {
        return metadata;
    }
}
