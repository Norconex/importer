/* Copyright 2015 Norconex Inc.
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
package com.norconex.importer.handler.splitter.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.CommonRestrictions;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.splitter.AbstractDocumentSplitter;
import com.norconex.importer.handler.splitter.SplittableDocument;

/**
 * <p>Splits HTML, XHTML, or XML document on a specific element.
 * </p>
 * <p>
 * This class constructs a DOM tree from the document content. That DOM tree
 * is loaded entirely into memory. Use this splitter with caution if you know
 * you'll need to parse huge files. It may be preferable to use a stream-based
 * approach if this is a concern.
 * </p>
 * <p>
 * The <a href="http://jsoup.org/">jsoup</a> parser library is used to load a 
 * document content into a DOM tree. Elements are referenced using a 
 * <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">
 * CSS or JQuery-like syntax</a>.
 * </p>
 * <p>Should be used as a pre-parse handler.</p>
 * 
 * <h3>Content-types</h3>
 * <p>
 * By default, this filter is restricted to (applies only to) documents matching
 * the restrictions returned by 
 * {@link CommonRestrictions#domContentTypes()}. 
 * You can specify your own content types if you know they represent a file
 * with HTML or XML-like markup tags.
 * </p>
 * <h3>
 * XML configuration usage:
 * </h3>
 * <pre>
 *  &lt;splitter class="com.norconex.importer.handler.splitter.impl.DOMSplitter"
 *          selector="(selector syntax)" &gt;
 *      &lt;restrictTo
 *              caseSensitive="[false|true]"
 *              field="(name of metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/splitter&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public class DOMSplitter extends AbstractDocumentSplitter
        implements IXMLConfigurable {

    private String selector;
    
    public DOMSplitter() {
        super();
        addRestrictions(CommonRestrictions.domContentTypes());
    }

    public String getSelector() {
        return selector;
    }
    public void setSelector(String selector) {
        this.selector = selector;
    }

    @Override
    protected List<ImporterDocument> splitApplicableDocument(
            SplittableDocument doc, OutputStream output,
            CachedStreamFactory streamFactory, boolean parsed)
            throws ImporterHandlerException {
        
        List<ImporterDocument> docs = new ArrayList<>();
        try {
            Document soupDoc = Jsoup.parse(
                    doc.getInput(), CharEncoding.UTF_8, doc.getReference());
            Elements elms = soupDoc.select(selector);
            for (Element elm : elms) {
                ImporterMetadata childMeta = new ImporterMetadata();
                childMeta.load(doc.getMetadata());
                String childContent = elm.outerHtml();
                String childEmbedRef = elm.cssSelector();
                String childRef = doc.getReference() + "!" + childEmbedRef;
                CachedInputStream content = null;
                if (childContent.length() > 0) {
                    content = streamFactory.newInputStream(childContent);
                } else {
                    content = streamFactory.newInputStream();
                }
                ImporterDocument childDoc = 
                        new ImporterDocument(childRef, content, childMeta); 
                childMeta.setReference(childRef);
                childMeta.setEmbeddedReference(childEmbedRef);
                childMeta.setEmbeddedParentReference(doc.getReference());
                childMeta.setEmbeddedParentRootReference(doc.getReference());
                docs.add(childDoc);
            }
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot parse document into a DOM-tree.", e);
        }
        return docs;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) {
        setSelector(xml.getString("[@selector]", getSelector()));
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("selector", getSelector());
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DOMSplitter)) {
            return false;
        }
        DOMSplitter castOther = (DOMSplitter) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(selector, castOther.selector)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(selector)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("selector", selector)
                .toString();
    }
}
