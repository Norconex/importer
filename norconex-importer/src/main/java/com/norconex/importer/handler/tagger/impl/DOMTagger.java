/* Copyright 2015-2016 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.CommonRestrictions;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Extract the value of one or more elements or attributes into 
 * a target field, from and HTML, XHTML, or XML document. If a target field 
 * already exists, extracted values will be added to existing values, 
 * unless "overwrite" is set to <code>true</code>.</p>
 * <p>
 * This class constructs a DOM tree from the document content. That DOM tree
 * is loaded entirely into memory. Use this tagger with caution if you know
 * you'll need to parse huge files. It may be preferable to use 
 * {@link TextPatternTagger} if this is a concern. Also, to help performance
 * and avoid re-creating DOM tree before every DOM extraction you want to 
 * perform, try to combine multiple extractions in a single instance
 * of this Tagger.
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
 * <h3>
 * XML configuration usage:
 * </h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DOMTagger"
 *          sourceCharset="(character encoding)"&gt;
 *      &lt;dom selector="(selector syntax)" toField="(target field)"
 *              overwrite="[false|true]" /&gt;
 *      &lt;!-- multiple "dom" tags allowed --&gt;
 *          
 *      &lt;restrictTo
 *              caseSensitive="[false|true]"
 *              field="(name of metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public class DOMTagger extends AbstractDocumentTagger {

    private final List<DOMExtractDetails> list = new ArrayList<>();
    private String sourceCharset = null;
    
    /**
     * Constructor.
     */
    public DOMTagger() {
        super();
        addRestrictions(CommonRestrictions.domContentTypes());
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
    
    @Override
    protected void tagApplicableDocument(String reference,
            InputStream document, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        String inputCharset = detectCharsetIfBlank(
                sourceCharset, reference, document, metadata, parsed);
        
        try {
            Document doc = Jsoup.parse(document, inputCharset, reference);
            for (DOMExtractDetails details : list) {
                Elements elms = doc.select(details.selector);
                // no elements matching
                if (elms.isEmpty()) {
                    return;
                }
                
                // one or more elements matching
                List<String> values = new ArrayList<String>();
                for (Element elm : elms) {
                    String value = elm.html();
                    if (StringUtils.isNotBlank(value)) {
                        values.add(value);
                    }
                }
                if (values.isEmpty()) {
                    return;
                }
                String[] vals = values.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
                if (details.overwrite) {
                    metadata.setString(details.toField, vals);
                } else {
                    metadata.addString(details.toField, vals);
                }
            }
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot extract DOM element(s) from DOM-tree.", e);
        }
    }

    /**
     * Adds DOM element value extraction instructions.
     * @param selector selector
     * @param toField target field name
     * @param overwrite whether toField overwrite target field if it exists
     */
    public void addDOMExtractDetails(
            String selector, String toField, boolean overwrite) {
        if (StringUtils.isBlank(selector)) {
            throw new IllegalArgumentException(
                    "'selector' argument cannot be blank.");
        }
        if (StringUtils.isBlank(toField)) {
            throw new IllegalArgumentException(
                    "'toField' argument cannot be blank.");
        }
        list.add(new DOMExtractDetails(selector, toField, overwrite));
    }
    
    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setSourceCharset(xml.getString("[@sourceCharset]", getSourceCharset()));
        List<HierarchicalConfiguration> nodes = xml.configurationsAt("dom");
        if (!nodes.isEmpty()) {
            list.clear();
        }
        for (HierarchicalConfiguration node : nodes) {
            addDOMExtractDetails(node.getString("[@selector]", null),
                      node.getString("[@toField]", null),
                      node.getBoolean("[@overwrite]", false));
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("sourceCharset", getSourceCharset());
        for (DOMExtractDetails details : list) {
            writer.writeStartElement("dom");
            writer.writeAttribute("selector", details.selector);
            writer.writeAttribute("toField", details.toField);
            writer.writeAttribute("overwrite", 
                    Boolean.toString(details.overwrite));
            writer.writeEndElement();
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DOMTagger)) {
            return false;
        }
        DOMTagger castOther = (DOMTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(list, castOther.list)
                .append(sourceCharset, castOther.sourceCharset)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(list)
                .append(sourceCharset)                
                .toHashCode();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
        builder.appendSuper(super.toString());
        builder.append("list", list);
        builder.append("sourceCharset", sourceCharset);
        return builder.toString();
    }
    
    private static class DOMExtractDetails {
        private String selector;
        private String toField;
        private boolean overwrite;
        
        DOMExtractDetails(String selector, String to, boolean overwrite) {
            this.selector = selector;
            this.toField = to;
            this.overwrite = overwrite;
        }
        
        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(
                    this, ToStringStyle.SHORT_PREFIX_STYLE);
            builder.append("selector", selector);
            builder.append("toField", toField);
            builder.append("overwrite", overwrite);
            return builder.toString();
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof DOMExtractDetails)) {
                return false;
            }
            DOMExtractDetails castOther = (DOMExtractDetails) other;
            return new EqualsBuilder().append(selector, castOther.selector)
                    .append(toField, castOther.toField)
                    .append(overwrite, castOther.overwrite).isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(selector).append(toField)
                    .append(overwrite).toHashCode();
        }
    }    
}
