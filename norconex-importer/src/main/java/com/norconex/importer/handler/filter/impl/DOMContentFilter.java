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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.CommonRestrictions;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;

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
 * <h3>Content-types</h3>
 * <p>
 * By default, this filter is restricted to (applies only to) documents matching
 * the restrictions returned by 
 * {@link CommonRestrictions#domContentTypes()}. 
 * You can specify your own content types if you know they represent a file
 * with HTML or XML-like markup tags.  For documents that are
 * incompatible, consider using {@link RegexContentFilter}
 * instead.
 * It usually only make sense to use this filter as a pre-parse handler.
 * </p>
 * <h3>
 * XML configuration usage:
 * </h3>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          onMatch="[include|exclude]" 
 *          caseSensitive="[false|true]"
 *          selector="(selector syntax)" &gt;
 *    &lt;regex&gt;(optional regular expression of value to match)&lt;/regex&gt;
 *    &lt;restrictTo caseSensitive="[false|true]"
 *            field="(name of header/metadata field name to match)"&gt;
 *        (regular expression of value to match)
 *    &lt;/restrictTo&gt;
 *    &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/filter&gt;
 * </pre>
 * <h3>
 * Configuration samples:
 * </h3>
 * <p>To exclude an HTML page that has one or more GIF images in it:</p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          selector="img[src$=.gif]" onMatch="exclude" /&gt;
 * </pre>
 * <p>To exclude an HTML page that has a paragraph tag with a class called
 * "disclaimer" and a value containing "skip me":</p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.DOMContentFilter"
 *          selector="p.disclaimer" onMatch="exclude" &gt;
 *    &lt;regex&gt;\bskip me\b&lt;/regex&gt;
 *  &lt;/filter&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.4.0
 * @see Pattern
 */
public class DOMContentFilter extends AbstractDocumentFilter {
    
    private boolean caseSensitive;
    private String regex;
    private Pattern pattern;
    private String selector;

    
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
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    public final void setRegex(String regex) {
        this.regex = regex;
        int baseFlags = Pattern.DOTALL;
        if (regex != null) {
            if (caseSensitive) {
                this.pattern = Pattern.compile(regex, baseFlags);
            } else {
                this.pattern = Pattern.compile(regex, baseFlags 
                        | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            }
        } else {
            this.pattern = Pattern.compile(".*");
        }
    }
    public String getSelector() {
        return selector;
    }
    public void setSelector(String selector) {
        this.selector = selector;
    }
    
    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        try {
            Document doc = Jsoup.parse(input, CharEncoding.UTF_8, reference);
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
                if (pattern.matcher(elm.text()).find()) {
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
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeBoolean("caseSensitive", caseSensitive);
        writer.writeAttributeString("selector", selector);
        writer.writeElementString("regex", regex);
    }
    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", isCaseSensitive()));
        setSelector(xml.getString("[@selector]", getSelector()));
        setRegex(xml.getString("regex"));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("caseSensitive", caseSensitive)
                .append("selector", selector)
                .append("regex", regex).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (caseSensitive ? 1231 : 1237);
        result = prime * result + ((selector == null) ? 0 
                : selector.hashCode());
        result = prime * result + ((regex == null) ? 0 : regex.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DOMContentFilter)) {
            return false;
        }
        DOMContentFilter other = (DOMContentFilter) obj;
        if (caseSensitive != other.caseSensitive) {
            return false;
        }
        if (regex == null) {
            if (other.regex != null) {
                return false;
            }
        } else if (!regex.equals(other.regex)) {
            return false;
        }
        if (selector == null) {
            if (other.selector != null) {
                return false;
            }
        } else if (!selector.equals(other.selector)) {
            return false;
        }
        return true;
    }

}
