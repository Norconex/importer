/* Copyright 2010-2014 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.importer.handler.filter;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class for document filters.  Subclasses can be set an attribute
 * called "onMatch".  The logic whether to include or exclude a document
 * upon matching it is handled by this class.  Subclasses only 
 * need to focus on whether the document gets matched or not by
 * implementing the 
 * {@link #isDocumentMatched(String, InputStream, ImporterMetadata, boolean)}
 * method.</p>
 * 
 * <p>Subclasses inherit this {@link IXMLConfigurable} configuration:</p>
 * <pre>
 *  &lt;!-- main tag supports onMatch="[include|exclude]" attribute --&gt;
 *  &lt;restrictTo caseSensitive="[false|true]"
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

    private final AbstractOnMatchFilter onMatch = new AbstractOnMatchFilter() {
    };
    
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
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString())
                .append("onMatch", onMatch).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractDocumentFilter))
            return false;
        AbstractDocumentFilter castOther = (AbstractDocumentFilter) other;
        return new EqualsBuilder().appendSuper(super.equals(other))
                .append(onMatch, castOther.onMatch).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode())
                .append(onMatch).toHashCode();
    }


    
}