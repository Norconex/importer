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
