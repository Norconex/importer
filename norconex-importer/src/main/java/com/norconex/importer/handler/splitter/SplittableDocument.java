package com.norconex.importer.handler.splitter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.CharEncoding;

import com.norconex.importer.ImporterRuntimeException;
import com.norconex.importer.doc.ImporterMetadata;

public class SplittableDocument {

    private final String reference;
    private final InputStream input;
    private final ImporterMetadata metadata;
    
    public SplittableDocument(
            String reference, InputStream input, ImporterMetadata metadata) {
        super();
        this.reference = reference;
        this.input = input;
        this.metadata = metadata;
    }

    public String getReference() {
        return reference;
    }
    public InputStream getInput() {
        return input;
    }
    public Reader getReader() {
        try {
            return new InputStreamReader(input, CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new ImporterRuntimeException("UTF8 must be supported.", e);
        }
    }
    public ImporterMetadata getMetadata() {
        return metadata;
    }
}
