/* Copyright 2015-2020 Norconex Inc.
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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.CommonRestrictions;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.parser.ParseState;
import com.norconex.importer.util.DOMUtil;

/**
 * <p>Uses a Document Object Model (DOM) representation of an HTML, XHTML, or
 * XML document content to perform filtering based on matching an
 * element/attribute or element/attribute value.
 * </p>
 * <p>
 * In order to construct a DOM tree, a document content is loaded entirely
 * into memory. Use this filter with caution if you know you'll need to parse
 * huge files. You can use {@link RegexContentFilter} instead if this is a
 * concern.
 * </p>
 * <p>
 * The <a href="http://jsoup.org/">jsoup</a> parser library is used to load a
 * document content into a DOM tree. Elements are referenced using a
 * <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">
 * CSS or JQuery-like syntax</a>.
 * </p>
 * <p>
 * If an element is referenced without a value to match, its mere presence
 * constitutes a match. If both an element and a regular expression is provided
 * the element value will be retrieved and the regular expression will be
 * applied against it for a match.
 * </p>
 * <p>
 * Refer to {@link AbstractDocumentFilter} for the inclusion/exclusion logic.
 * </p>
 * <p>Should be used as a pre-parse handler.</p>
 * <h3>Content-types</h3>
 * <p>
 * By default, this filter is restricted to (applies only to) documents matching
 * the restrictions returned by
 * {@link CommonRestrictions#domContentTypes(String)}.
 * You can specify your own content types if you know they represent a file
 * with HTML or XML-like markup tags.  For documents that are
 * incompatible, consider using {@link RegexContentFilter}
 * instead.
 * </p>
 *
 * <p>When used as a pre-parse handler,
 * this class attempts to detect the content character
 * encoding unless the character encoding
 * was specified using {@link #setSourceCharset(String)}. Since document
 * parsing converts content to UTF-8, UTF-8 is always assumed when
 * used as a post-parse handler.
 * </p>
 *
 * <p>It is possible to control what gets extracted
 * exactly for matching purposes thanks to the "extract" argument of the
 * new method {@link #setExtract(String)}. Possible values are:</p>
 * <ul>
 *   <li><b>text</b>: Default option when extract is blank. The text of
 *       the element, including combined children.</li>
 *   <li><b>html</b>: Extracts an element inner
 *       HTML (including children).</li>
 *   <li><b>outerHtml</b>: Extracts an element outer
 *       HTML (like "html", but includes the "current" tag).</li>
 *   <li><b>ownText</b>: Extracts the text owned by this element only;
 *       does not get the combined text of all children.</li>
 *   <li><b>data</b>: Extracts the combined data of a data-element (e.g.
 *       &lt;script&gt;).</li>
 *   <li><b>id</b>: Extracts the ID attribute of the element (if any).</li>
 *   <li><b>tagName</b>: Extract the name of the tag of the element.</li>
 *   <li><b>val</b>: Extracts the value of a form element
 *       (input, textarea, etc).</li>
 *   <li><b>className</b>: Extracts the literal value of the element's
 *       "class" attribute, which may include multiple class names,
 *       space separated.</li>
 *   <li><b>cssSelector</b>: Extracts a CSS selector that will uniquely
 *       select (identify) this element.</li>
 *   <li><b>attr(attributeKey)</b>: Extracts the value of the element
 *       attribute matching your replacement for "attributeKey"
 *       (e.g. "attr(title)" will extract the "title" attribute).</li>
 * </ul>
 *
 * <p>You can specify which parser to use when reading
 * documents. The default is "html" and will normalize the content
 * as HTML. This is generally a desired behavior, but this can sometimes
 * have your selector fail. If you encounter this
 * problem, try switching to "xml" parser, which does not attempt normalization
 * on the content. The drawback with "xml" is you may not get all HTML-specific
 * selector options to work.  If you know you are dealing with XML to begin
 * with, specifying "xml" should be a good option.
 * </p>
 *
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *     {@nx.include com.norconex.importer.handler.filter.AbstractDocumentFilter#attributes}
 *     sourceCharset="(character encoding)"
 *     selector="(selector syntax)"
 *     parser="[html|xml]"
 *     extract="[text|html|outerHtml|ownText|data|tagName|val|className|cssSelector|attr(attributeKey)]">
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <valueMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *     (optional expression matching selector extracted value)
 *   </valueMatcher>
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <!-- Exclude an HTML page that has one or more GIF images in it: -->
 * <handler class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          selector="img[src$=.gif]" onMatch="exclude" />
 *
 * <!-- Exclude an HTML page that has a paragraph tag with a class called
 *      "disclaimer" and a value containing "skip me": -->
 * <handler class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          selector="p.disclaimer" onMatch="exclude" >
 *   <regex>\bskip me\b</regex>
 * </handler>
 * }
 *
 * @author Pascal Essiembre
 * @since 2.4.0
 * @deprecated Since 3.0.0, use {@link DOMFilter}.
 */
@Deprecated
@SuppressWarnings("javadoc")
public class DOMContentFilter extends AbstractDocumentFilter {

    private final TextMatcher valueMatcher = new TextMatcher();
    private String selector;
    private String extract;
    private String sourceCharset = null;
    private String parser = DOMUtil.getInstance().PARSER_HTML;

    public DOMContentFilter() {
        setOnMatch(OnMatch.INCLUDE);
        addRestrictions(
                CommonRestrictions.domContentTypes(DocMetadata.CONTENT_TYPE));
    }
    /**
     * Constructor.
     * @param regex regular expression
     * @deprecated Since 3.0.0
     */
    @Deprecated
    public DOMContentFilter(String regex) {
        this(regex, OnMatch.INCLUDE);
    }
    /**
     * Constructor.
     * @param regex regular expression
     * @param onMatch on match instruction
     * @deprecated Since 3.0.0
     */
    @Deprecated
    public DOMContentFilter(String regex, OnMatch onMatch) {
        this(regex, onMatch, false);
    }
    /**
     * Constructor.
     * @param regex regular expression
     * @param onMatch on match instruction
     * @param caseSensitive whether regular expression is case sensitive
     * @deprecated Since 3.0.0
     */
    @Deprecated
    public DOMContentFilter(String regex,
            OnMatch onMatch, boolean caseSensitive) {
        super();
        valueMatcher.setIgnoreCase(!caseSensitive);
        setOnMatch(onMatch);
        setRegex(regex);
        addRestrictions(
                CommonRestrictions.domContentTypes(DocMetadata.CONTENT_TYPE));
    }

    /**
     * Gets the expression matching text extracted by selector.
     * @return expression
     * @deprecated Since 3.0.0, use {@link #getValueMatcher()}
     */
    @Deprecated
    public String getRegex() {
        return valueMatcher.getPattern();
    }
    /**
     * Sets the expression matching text extracted by selector.
     * @param regex expression
     * @deprecated Since 3.0.0, use {@link #getValueMatcher()}
     */
    @Deprecated
    public final void setRegex(String regex) {
        valueMatcher.setPattern(regex);
        valueMatcher.setMethod(Method.REGEX);
    }

    /**
     * Gets whether expression matching text extracted by selector is case
     * sensitive.
     * @return <code>true</code> if case sensitive
     * @deprecated Since 3.0.0, use {@link #getValueMatcher()}
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return !valueMatcher.isIgnoreCase();
    }
    /**
     * Sets whether expression matching text extracted by selector is case
     * sensitive.
     * @param caseSensitive <code>true</code> if case sensitive
     * @deprecated Since 3.0.0, use {@link #getValueMatcher()}
     */
    @Deprecated
    public void setCaseSensitive(boolean caseSensitive) {
        valueMatcher.setIgnoreCase(!caseSensitive);
    }
    public String getSelector() {
        return selector;
    }
    public void setSelector(String selector) {
        this.selector = selector;
    }


    /**
     * Gets this filter text matcher (copy).
     * @return text matcher
     * @since 3.0.0
     */
    public TextMatcher getValueMatcher() {
        return valueMatcher;
    }
    /**
     * Sets this filter text matcher (copy).
     * @param valueMatcher text matcher
     * @since 3.0.0
     */
    public void setValueMatcher(TextMatcher valueMatcher) {
        this.valueMatcher.copyFrom(valueMatcher);
    }
    /**
     * Gets what should be extracted for the value. One of
     * "text" (default), "html", or "outerHtml". <code>null</code> means
     * this class will use the default ("text").
     * @return what should be extracted for the value
     * @since 2.5.0
     */
    public String getExtract() {
        return extract;
    }
    /**
     * Sets what should be extracted for the value. One of
     * "text" (default), "html", or "outerHtml". <code>null</code> means
     * this class will use the default ("text").
     * @param extract what should be extracted for the value
     * @since 2.5.0
     */
    public void setExtract(String extract) {
        this.extract = extract;
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
    protected boolean isDocumentMatched(
            HandlerDoc doc, InputStream input, ParseState parseState)
                    throws ImporterHandlerException {
        String inputCharset = detectCharsetIfBlank(
                doc, input, sourceCharset, parseState);
        try {
            Document jdoc = Jsoup.parse(input, inputCharset,
                    doc.getReference(), DOMUtil.getInstance().toJSoupParser(getParser()));
            Elements elms = jdoc.select(selector);
            // no elements matching
            if (elms.isEmpty()) {
                return false;
            }
            // one or more elements matching
            for (Element elm : elms) {
                String value = DOMUtil.getInstance().getElementValue(elm, getExtract());
                if (valueMatcher.matches(value)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot parse document into a DOM-tree.", e);
        }
    }

    @Override
    protected void loadFilterFromXML(XML xml) {
        xml.checkDeprecated("@caseSensitive", "valueMatcher/ignoreCase", true);
        xml.checkDeprecated("regex", "valueMatcher", true);
        setSelector(xml.getString("@selector", selector));
        setParser(xml.getString("@parser", parser));
        setSourceCharset(xml.getString("@sourceCharset", sourceCharset));
        setSourceCharset(xml.getString("@extract", extract));
        valueMatcher.loadFromXML(xml.getXML("valueMatcher"));
    }
    @Override
    protected void saveFilterToXML(XML xml) {
        xml.setAttribute("selector", selector);
        xml.setAttribute("parser", parser);
        xml.setAttribute("sourceCharset", sourceCharset);
        xml.setAttribute("extract", extract);
        valueMatcher.saveToXML(xml.addElement("valueMatcher"));
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other, "cachedPattern");
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "cachedPattern");
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE)
                .setExcludeFieldNames("cachedPattern")
                .toString();
    }
}
