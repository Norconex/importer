/**
 * 
 */
package com.norconex.importer.doc;

import java.io.Serializable;

import com.norconex.commons.lang.file.ContentType;

/**
 * A document.
 * @author Pascal Essiembre
 */
public class ImporterDocument implements Serializable {

    private static final long serialVersionUID = 7098788330064549944L;

    private final String reference;
    private Content content;
    private final ImporterMetadata metadata;
    private ContentType contentType;
    
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

    public Content getContent() {
        return content;
    }
    public void setContent(Content content) {
        this.content = content;
    }
    
    public ImporterMetadata getMetadata() {
        return metadata;
    }
}
