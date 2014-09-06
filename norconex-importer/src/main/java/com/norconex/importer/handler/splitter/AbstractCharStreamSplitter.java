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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * Base class for splitters dealing with text documents only.
 * 
 * <p/>Sub-classes can be used safely as post-parse handlers 
 * (assumed to be text).   
 * Add a restriction to text-documents only when using as a 
 * pre-handler (see {@link AbstractImporterHandler}).
 * 
 * <p />
 * 
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * <pre>
 *  &lt;restrictTo caseSensitive="[false|true]" &gt;
 *          field="(name of header/metadata field name to match)"&gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public abstract class AbstractCharStreamSplitter 
        extends AbstractDocumentSplitter {

    private static final long serialVersionUID = -2595121808885491325L;

    @Override
    protected final List<ImporterDocument> splitApplicableDocument(
            String reference, InputStream input, OutputStream output, 
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException {

        List<ImporterDocument> children = null;
        try {
            InputStreamReader is = 
                    new InputStreamReader(input, CharEncoding.UTF_8);
            OutputStreamWriter os = 
                    new OutputStreamWriter(output, CharEncoding.UTF_8);
            children = splitTextDocument(reference, is, os, metadata, parsed);
            os.flush();
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot split character stream.", e);
        }
        return children;
    }

    protected abstract List<ImporterDocument> splitTextDocument(
            String reference, Reader input, Writer output, 
            ImporterMetadata metadata, boolean parsed)
                    throws IOException;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractCharStreamSplitter)) {
            return false;
        }
        return new EqualsBuilder().appendSuper(super.equals(other)).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).toHashCode();
    } 
}