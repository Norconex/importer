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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
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
 * </p>
 * <p>
 * This class constructs a DOM tree from the document content. That DOM tree
 * is loaded entirely into memory. Use this tagger with caution if you know
 * you'll need to parse huge files. It may be preferable to use 
 * {@link TextPatternTagger} if this is a concern.
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
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DOMTagger"
 *          selector="(selector syntax)"
 *          toField="(target field)"
 *          overwrite="[false|true]" &gt;
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

    private String selector;
    private String toField;
    private boolean overwrite;

    
    /**
     * Constructor.
     */
    public DOMTagger() {
        super();
        addRestrictions(CommonRestrictions.domContentTypes());
    }
    
    @Override
    protected void tagApplicableDocument(String reference,
            InputStream document, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        if (StringUtils.isBlank(selector)) {
            throw new IllegalArgumentException(
                    "'selector' argument cannot be blank.");
        }
        if (StringUtils.isBlank(toField)) {
            throw new IllegalArgumentException(
                    "'toField' argument cannot be blank.");
        }
        
        try {
            Document doc = Jsoup.parse(document, CharEncoding.UTF_8, reference);
            Elements elms = doc.select(selector);
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
            if (overwrite) {
                metadata.setString(toField, vals);
            } else {
                metadata.addString(toField, vals);
            }
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot parse document into a DOM-tree.", e);
        }
    }

    public String getSelector() {
        return selector;
    }
    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getToField() {
        return toField;
    }
    public void setToField(String toField) {
        this.toField = toField;
    }

    public boolean isOverwrite() {
        return overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setSelector(xml.getString("[@selector]", getSelector()));
        setToField(xml.getString("[@toField]", getToField()));
        setOverwrite(xml.getBoolean("[@overwrite]", isOverwrite()));
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("selector", getSelector());
        writer.writeAttributeString("toField", getToField());
        writer.writeAttributeBoolean("overwrite", isOverwrite());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (overwrite ? 1231 : 1237);
        result = prime * result
                + ((selector == null) ? 0 : selector.hashCode());
        result = prime * result + ((toField == null) ? 0 : toField.hashCode());
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
        if (!(obj instanceof DOMTagger)) {
            return false;
        }
        DOMTagger other = (DOMTagger) obj;
        if (overwrite != other.overwrite) {
            return false;
        }
        if (selector == null) {
            if (other.selector != null) {
                return false;
            }
        } else if (!selector.equals(other.selector)) {
            return false;
        }
        if (toField == null) {
            if (other.toField != null) {
                return false;
            }
        } else if (!toField.equals(other.toField)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
        builder.append("selector", getSelector());
        builder.append("toField", getToField());
        builder.append("overwrite", isOverwrite());
        return builder.toString();
    }
}
