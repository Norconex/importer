/* Copyright 2015-2018 Norconex Inc.
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.CommonRestrictions;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.splitter.AbstractDocumentSplitter;
import com.norconex.importer.handler.splitter.SplittableDocument;
import com.norconex.importer.util.DOMUtil;

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
 *
 * <p><b>Since 2.5.0</b>, when used as a pre-parse handler,
 * this class attempts to detect the content character
 * encoding unless the character encoding
 * was specified using {@link #setSourceCharset(String)}. Since document
 * parsing converts content to UTF-8, UTF-8 is always assumed when
 * used as a post-parse handler.
 * </p>
 *
 * <p><b>Since 2.8.0</b>, you can specify which parser to use when reading
 * documents. The default is "html" and will normalize the content
 * as HTML. This is generally a desired behavior, but this can sometimes
 * have your selector fail. If you encounter this
 * problem, try switching to "xml" parser, which does not attempt normalization
 * on the content. The drawback with "xml" is you may not get all HTML-specific
 * selector options to work.  If you know you are dealing with XML to begin
 * with, specifying "xml" should be a good option.
 * </p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.splitter.impl.DOMSplitter"
 *          selector="(selector syntax)"
 *          parser="[html|xml]"
 *          sourceCharset="(character encoding)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following split contacts found in an HTML document, each one being
 * stored within a div with a class named "contact".
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.splitter.impl.DOMSplitter"
 *          selector="div.contact" /&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public class DOMSplitter extends AbstractDocumentSplitter
        implements IXMLConfigurable {

    private String selector;
    private String sourceCharset = null;
    private String parser = DOMUtil.PARSER_HTML;

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
    /**
     * Gets the assumed source character encoding.
     * @return character encoding of the source to be transformed
     * @since 2.5.0
     */
    public String getSourceCharset() {
        return sourceCharset;
    }
    /**
     * Sets the assumed source character encoding.
     * @param sourceCharset character encoding of the source to be transformed
     * @since 2.5.0
     */
    public void setSourceCharset(String sourceCharset) {
        this.sourceCharset = sourceCharset;
    }

    /**
     * Gets the parser to use when creating the DOM-tree.
     * @return <code>html</code> (default) or <code>xml</code>.
     * @since 2.8.0
     */
    public String getParser() {
        return parser;
    }
    /**
     * Sets the parser to use when creating the DOM-tree.
     * @param parser <code>html</code> or <code>xml</code>.
     * @since 2.8.0
     */
    public void setParser(String parser) {
        this.parser = parser;
    }

    @Override
    protected List<ImporterDocument> splitApplicableDocument(
            SplittableDocument doc, OutputStream output,
            CachedStreamFactory streamFactory, boolean parsed)
            throws ImporterHandlerException {

        String inputCharset = detectCharsetIfBlank(
                sourceCharset, doc.getReference(),
                doc.getInput(), doc.getMetadata(), parsed);

        List<ImporterDocument> docs = new ArrayList<>();
        try {
            Document soupDoc = Jsoup.parse(doc.getInput(), inputCharset,
                    doc.getReference(), DOMUtil.toJSoupParser(getParser()));
            Elements elms = soupDoc.select(selector);

            // if there only 1 element matched, make sure it is not the same as
            // the parent document to avoid infinite loops (the parent
            // matching itself recursively).
            if (elms.size() == 1) {
                Element matchedElement = elms.get(0);
                Element parentElement = getBodyElement(soupDoc);
                if (matchedElement.equals(parentElement)) {
                    return docs;
                }
            }

            // process "legit" child elements
            for (Element elm : elms) {
                ImporterMetadata childMeta = new ImporterMetadata();
                childMeta.loadFromMap(doc.getMetadata());
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

    private Element getBodyElement(Document soupDoc) {
        Element body = soupDoc.body();
        if (body.childNodeSize() == 1) {
            return body.child(0);
        }
        return null;
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        setSelector(xml.getString("@selector", selector));
        setSourceCharset(xml.getString("@sourceCharset", sourceCharset));
        setParser(xml.getString("@parser", parser));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("selector", selector);
        xml.setAttribute("sourceCharset", sourceCharset);
        xml.setAttribute("parser", parser);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
