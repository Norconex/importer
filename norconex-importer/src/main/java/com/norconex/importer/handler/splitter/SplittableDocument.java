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
package com.norconex.importer.handler.splitter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.CharEncoding;

import com.norconex.importer.ImporterRuntimeException;
import com.norconex.importer.doc.ImporterMetadata;

/**
 * @since 2.0.0
 */
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
