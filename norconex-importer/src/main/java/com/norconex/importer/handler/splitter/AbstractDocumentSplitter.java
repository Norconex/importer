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
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * Base class for splitters .
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
public abstract class AbstractDocumentSplitter extends AbstractImporterHandler
            implements IDocumentSplitter {

    private static final long serialVersionUID = -6511725137481907345L;

    public AbstractDocumentSplitter() {
        super("splitter");
    }

    @Override
    public final List<ImporterDocument> splitDocument(
            String reference, InputStream input, OutputStream output, 
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException {
        
        if (!isApplicable(reference, metadata, parsed)) {
            return null;
        }
        return splitApplicableDocument(
                reference, input, output, metadata, parsed);
    }

    protected abstract List<ImporterDocument> splitApplicableDocument(
            String reference, InputStream input, OutputStream output, 
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractDocumentSplitter)) {
            return false;
        }
        return new EqualsBuilder().appendSuper(super.equals(other)).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).toHashCode();
    } 
}