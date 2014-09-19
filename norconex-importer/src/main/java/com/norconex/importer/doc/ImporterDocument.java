/**
 * 
 */
package com.norconex.importer.doc;

import java.io.Serializable;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;

/**
 * A document being imported.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterDocument implements Serializable {

    private static final long serialVersionUID = 7098788330064549944L;

    
    //TODO add parent reference info here???
    
    private String reference;
    private CachedInputStream content;
    private final ImporterMetadata metadata;
    private ContentType contentType;
    private String contentEncoding;
    
//    public ImporterDocument(String reference) {
//        this(reference, (CachedInputStream) null);
//    }
    public ImporterDocument(String reference, CachedInputStream content) {
        this(reference, content, null);
    }
//    public ImporterDocument(String reference, ImporterMetadata metadata) {
//        this(reference, null, metadata);
//    }
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
