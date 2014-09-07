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
package com.norconex.importer.handler.tagger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class for taggers dealing with the body of text documents only.  
 * Subclasses can safely be used as either pre-parse or post-parse handlers
 * restricted to text documents only (see {@link AbstractImporterHandler}).
 * </p>
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * <pre>
 *  &lt;restrictTo
 *          caseSensitive="[false|true]" &gt;
 *          field="(name of header/metadata field name to match)"&gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public abstract class AbstractCharStreamTagger extends AbstractDocumentTagger {

    private static final long serialVersionUID = 7733519110785336458L;

    @Override
    protected final void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException {
        
        String contentType = metadata.getString("Content-Type", "");
        contentType = contentType.replaceAll(".*charset=", "");
        if (StringUtils.isBlank(contentType)) {
            contentType = CharEncoding.UTF_8;
        }
        try {
            InputStreamReader is = new InputStreamReader(document, contentType);
            tagTextDocument(reference, is, metadata, parsed);
        } catch (UnsupportedEncodingException e) {
            throw new ImporterHandlerException(e);
        }
    }

    protected abstract void tagTextDocument(
            String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractCharStreamTagger)) {
            return false;
        }
        return new EqualsBuilder().appendSuper(super.equals(other)).isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).toHashCode();
    } 

}