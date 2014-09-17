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
package com.norconex.importer.handler.filter;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Base class for document filters.  Subclasses can be set an attribute
 * called "onMatch".  The logic whether to include or exclude a document
 * upon matching it is handled by this class.  Subclasses only 
 * need to focus on whether the document gets matched or not by
 * implementing the 
 * {@link #isDocumentMatched(String, InputStream, ImporterMetadata, boolean)}
 * method.
 * <p />
 * 
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * <pre>
 *  &lt;!-- main tag supports onMatch="[include|exclude]" attribute --&gt;
 *  &lt;restrictTo caseSensitive="[false|true]" &gt;
 *          field="(name of header/metadata field name to match)"&gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see AbstractOnMatchFilter
 */
public abstract class AbstractDocumentFilter extends AbstractImporterHandler
            implements IDocumentFilter, IOnMatchFilter {

    private static final long serialVersionUID = -8588335655642900736L;

    private final AbstractOnMatchFilter onMatch = new AbstractOnMatchFilter() {
        private static final long serialVersionUID = 1075825339565726500L;
    };
    
    private transient String toString;

    public AbstractDocumentFilter() {
        super("filter");
    }

    @Override
    public OnMatch getOnMatch() {
        return onMatch.getOnMatch();
    }

    public final void setOnMatch(OnMatch onMatch) {
        this.onMatch.setOnMatch(onMatch);
    }

    @Override
    public boolean acceptDocument(String reference, 
            InputStream input, ImporterMetadata metadata,
            boolean parsed) throws ImporterHandlerException {
        
        if (!isApplicable(reference, metadata, parsed)) {
            return true;
        }
        
        if (!isDocumentMatched(reference, input, metadata, parsed)) {
            return true;
        }
        return getOnMatch() == OnMatch.INCLUDE;
    }

    protected abstract boolean isDocumentMatched(
            String reference, InputStream input, 
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException;

    @Override
    protected final void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        onMatch.saveToXML(writer);
        saveFilterToXML(writer);
    }
    protected abstract void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException;

    @Override
    protected final void loadHandlerFromXML(
            XMLConfiguration xml) throws IOException {
        onMatch.loadFromXML(xml);
        loadFilterFromXML(xml);
    }
    protected abstract void loadFilterFromXML(
            XMLConfiguration xml) throws IOException;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractDocumentFilter))
            return false;
        AbstractDocumentFilter castOther = (AbstractDocumentFilter) other;
        return new EqualsBuilder().append(onMatch, castOther.onMatch)
                .isEquals();
    }
    private transient int hashCode;

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(onMatch).toHashCode();
        }
        return hashCode;
    }

    
    @Override
    public String toString() {
        if (toString == null) {
            toString = new ToStringBuilder(this).append("onMatch", onMatch)
                    .toString();
        }
        return toString;
    }

    
}