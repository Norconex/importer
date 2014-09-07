/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.importer.handler.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * Base class for transformers dealing with text documents only.
 * Subclasses can safely be used as either pre-parse or post-parse handlers
 * restricted to text documents only (see {@link AbstractImporterHandler}).
 * <p/>
 * Sub-classes can restrict to which document to apply this transformation
 * based on document metadata (see {@link AbstractImporterHandler}).
 * <p/>
 * Subclasses implementing {@link IXMLConfigurable} should allow this inner 
 * configuration:
 * <pre>
 *      &lt;restrictTo caseSensitive="[false|true]" &gt;
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public abstract class AbstractCharStreamTransformer 
            extends AbstractDocumentTransformer {

    private static final long serialVersionUID = -7465364282740091371L;
    
    @Override
    protected final void transformApplicableDocument(
            String reference, InputStream input,
            OutputStream output, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        try {
            InputStreamReader is = new InputStreamReader(input, CharEncoding.UTF_8);
            OutputStreamWriter os = 
                    new OutputStreamWriter(output, CharEncoding.UTF_8);
            transformTextDocument(reference, is, os, metadata, parsed);
            os.flush();
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot transform character stream.", e);
        }
    }

    protected abstract void transformTextDocument(
            String reference, Reader input,
            Writer output, ImporterMetadata metadata, boolean parsed)
            throws IOException;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractCharStreamTransformer)) {
            return false;
        }
        return new EqualsBuilder().appendSuper(super.equals(other)).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).toHashCode();
    } 
}