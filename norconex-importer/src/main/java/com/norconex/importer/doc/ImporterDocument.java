/**
 * 
 */
package com.norconex.importer.doc;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.importer.ImporterMetadata;

/**
 * A document.
 * @author Pascal Essiembre
 */
public class ImporterDocument implements Serializable {

    private static final long serialVersionUID = 7098788330064549944L;

    private static final ImporterDocument[] EMPTY_DOCS = 
            new ImporterDocument[] {};
    
    private final String reference;
    private Content content;
    private final ImporterMetadata metadata;
    private ContentType contentType;
    private final List<ImporterDocument> childDocuments = 
            new ArrayList<ImporterDocument>();

    private ImporterDocument parentDocument;
    
    public ImporterDocument(String reference) {
        this(reference, null);
    }
    public ImporterDocument(String reference, Content content) {
        this(reference, content, null);
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
            content = new Content((InputStream) null);
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

    public ImporterDocument getParentDocument() {
        return parentDocument;
    }
    
    public boolean isRootDocument() {
        return parentDocument == null;
    }
    
    public void addChildDocument(ImporterDocument doc) {
        doc.setParent(this);
        childDocuments.add(doc);
    }
    public void removeChildDocument(String reference) {
        ImporterDocument doc = null;
        for (ImporterDocument childDoc : childDocuments) {
            if (childDoc.getReference().equals(reference)) {
                doc = childDoc;
            }
        }
        if (doc == null) {
            return;
        }
        doc.setParent(null);
        childDocuments.remove(doc);
    }
    
    public ImporterDocument[] getChildDocuments() {
        return childDocuments.toArray(EMPTY_DOCS);
    }
    
    private void setParent(ImporterDocument parentDocument) {
        this.parentDocument = parentDocument;
    }
}
