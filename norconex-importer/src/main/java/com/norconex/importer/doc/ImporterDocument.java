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
package com.norconex.importer.doc;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;

/**
 * A document being imported.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterDocument {

    //TODO add parent reference info here???
    
    private String reference;
    private CachedInputStream content;
    private final ImporterMetadata metadata;
    private ContentType contentType;
    private String contentEncoding;
    
    public ImporterDocument(String reference, CachedInputStream content) {
        this(reference, content, null);
    }
    public ImporterDocument(String reference, CachedInputStream content, 
            ImporterMetadata metadata) {
        super();
        validateContent(content);
        validateReference(reference);
        this.reference = reference;
        this.content = content;
        if (metadata == null) {
            this.metadata = new ImporterMetadata();
        } else {
            this.metadata = metadata;
        }
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
        validateReference(reference);
        this.reference = reference;
    }

    public CachedInputStream getContent() {
        content.rewind();
        return content;
    }
    public void setContent(CachedInputStream content) {
        validateContent(content);
        this.content = content;
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
    
    private void validateContent(CachedInputStream content) {
        if (content == null) {
            throw new IllegalArgumentException(
                    "'content' argument cannot be null.");
        }
    }
    private void validateReference(String reference) {
        if (reference == null) {
            throw new IllegalArgumentException(
                    "'reference' argument cannot be null.");
        }
    }
}
