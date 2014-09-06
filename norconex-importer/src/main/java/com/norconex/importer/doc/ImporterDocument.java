/**
 * 
 */
package com.norconex.importer.doc;

import java.io.Serializable;

import com.norconex.commons.lang.file.ContentType;

/**
 * A document being imported.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterDocument implements Serializable {

    private static final long serialVersionUID = 7098788330064549944L;

    
    //TODO add parent reference info here???
    
    private String reference;
    private Content content;
    private final ImporterMetadata metadata;
    private ContentType contentType;
    private String contentEncoding;
    
    public ImporterDocument(String reference) {
        this(reference, (Content) null);
    }
    public ImporterDocument(String reference, Content content) {
        this(reference, content, null);
    }
    public ImporterDocument(String reference, ImporterMetadata metadata) {
        this(reference, null, metadata);
    }
    public ImporterDocument(
            String reference, Content content, ImporterMetadata metadata) {
        super();
        if (reference == null) {
            throw new IllegalArgumentException(
                    "'reference' argument cannot be null.");
        }
        this.reference = reference;
        if (content == null) {
            content = new Content();
        }
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
        this.reference = reference;
    }

    public Content getContent() {
        return content;
    }
    public void setContent(Content content) {
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
}
