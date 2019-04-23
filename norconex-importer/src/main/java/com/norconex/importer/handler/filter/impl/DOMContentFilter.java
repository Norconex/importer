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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.norconex.commons.lang.text.Regex;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.CommonRestrictions;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
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
 * {@link CommonRestrictions#domContentTypes()}.
 * You can specify your own content types if you know they represent a file
 * with HTML or XML-like markup tags.  For documents that are
 * incompatible, consider using {@link RegexContentFilter}
 * instead.
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
 * <p><b>Since 2.5.0</b>, it is possible to control what gets extracted
 * exactly for matching purposes thanks to the "extract" argument of the
 * new method {@link #setExtract(String)}.  Version 2.6.0
 * introduced several more extract options. Possible values are:</p>
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
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          onMatch="[include|exclude]"
 *          caseSensitive="[false|true]"
 *          sourceCharset="(character encoding)"
 *          selector="(selector syntax)"
 *          parser="[html|xml]"
 *          extract="[text|html|outerHtml|ownText|data|tagName|val|className|cssSelector|attr(attributeKey)]" &gt;
 *
 *    &lt;restrictTo caseSensitive="[false|true]"
 *            field="(name of header/metadata field name to match)"&gt;
 *        (regular expression of value to match)
 *    &lt;/restrictTo&gt;
 *    &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *    &lt;regex&gt;(optional regular expression matching selector extracted value)&lt;/regex&gt;
 *  &lt;/handler&gt;
 * </pre>
 * <h3>Examples:</h3>
 * <p>To exclude an HTML page that has one or more GIF images in it:</p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          selector="img[src$=.gif]" onMatch="exclude" /&gt;
 * </pre>
 * <p>To exclude an HTML page that has a paragraph tag with a class called
 * "disclaimer" and a value containing "skip me":</p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          selector="p.disclaimer" onMatch="exclude" &gt;
 *    &lt;regex&gt;\bskip me\b&lt;/regex&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public class DOMContentFilter extends AbstractDocumentFilter {

    private boolean caseSensitive;
    private String regex;
    private Pattern cachedPattern;
    private String selector;
    private String extract;
    private String sourceCharset = null;
    private String parser = DOMUtil.PARSER_HTML;

    public DOMContentFilter() {
        this(null, OnMatch.INCLUDE);
    }
    public DOMContentFilter(String regex) {
        this(regex, OnMatch.INCLUDE);
    }
    public DOMContentFilter(String regex, OnMatch onMatch) {
        this(regex, onMatch, false);
    }
    public DOMContentFilter(String regex,
            OnMatch onMatch, boolean caseSensitive) {
        super();
        this.caseSensitive = caseSensitive;
        setOnMatch(onMatch);
        setRegex(regex);
        addRestrictions(CommonRestrictions.domContentTypes());
    }

    public String getRegex() {
        return regex;
    }
    public final void setRegex(String regex) {
        this.regex = regex;
        cachedPattern = null;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        cachedPattern = null;
    }
    public String getSelector() {
        return selector;
    }
    public void setSelector(String selector) {
        this.selector = selector;
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
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        String inputCharset = detectCharsetIfBlank(
                sourceCharset, reference, input, metadata, parsed);

        try {
            Document doc = Jsoup.parse(input, inputCharset,
                    reference, DOMUtil.toJSoupParser(getParser()));
            Elements elms = doc.select(selector);
            // no elements matching
            if (elms.isEmpty()) {
                return false;
            }
            // one or more elements matching
            if (StringUtils.isBlank(regex)) {
                return true;
            }
            for (Element elm : elms) {
                String value = DOMUtil.getElementValue(elm, getExtract());
                if (getCachedPattern().matcher(value).find()) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot parse document into a DOM-tree.", e);
        }
    }

    private synchronized Pattern getCachedPattern() {
        if (cachedPattern != null) {
            return cachedPattern;
        }
        Pattern p;
        if (regex == null) {
            p = Pattern.compile(".*");
        } else {
            p = Regex.compileDotAll(regex, !caseSensitive);
        }
        cachedPattern = p;
        return p;
    }

    @Override
    protected void saveFilterToXML(XML xml) {
        xml.setAttribute("caseSensitive", caseSensitive);
        xml.setAttribute("selector", selector);
        xml.setAttribute("parser", parser);
        xml.setAttribute("sourceCharset", sourceCharset);
        xml.setAttribute("extract", extract);
        xml.addElement("regex", regex);
    }
    @Override
    protected void loadFilterFromXML(XML xml) {
        setCaseSensitive(xml.getBoolean("@caseSensitive", caseSensitive));
        setSelector(xml.getString("@selector", selector));
        setParser(xml.getString("@parser", parser));
        setSourceCharset(xml.getString("@sourceCharset", sourceCharset));
        setSourceCharset(xml.getString("@extract", extract));
        setRegex(xml.getString("regex", regex));
        cachedPattern = null;
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
