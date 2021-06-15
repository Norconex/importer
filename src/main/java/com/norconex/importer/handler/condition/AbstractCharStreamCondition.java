/* Copyright 2021 Norconex Inc.
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
package com.norconex.importer.handler.condition;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.io.IOUtil;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractCharStreamFilter;
import com.norconex.importer.parser.ParseState;
import com.norconex.importer.util.CharsetUtil;

/**
 * <p>Base class for conditions dealing with the document content as text.
 * Subclasses can safely be used as either pre-parse or post-parse handler
 * conditions restricted to text documents only.
 * </p>
 *
 * <p>When used as a pre-parse handler,
 * this class will use detected or previously set content character
 * encoding unless the character encoding
 * was specified using {@link #setSourceCharset(String)}. Since document
 * parsing converts content to UTF-8, UTF-8 is always assumed when
 * used as a post-parse handler.
 * </p>
 *
 * {@nx.xml.usage #attributes
 *  sourceCharset="(character encoding)"
 * }
 *
 * <p>
 * Subclasses inherit the above {@link IXMLConfigurable} attribute(s).
 * </p>
 *
 * @author Pascal Essiembre
 * @since 3.0.0 (adapted from {@link AbstractCharStreamFilter})
 */
public abstract class AbstractCharStreamCondition
        implements IImporterCondition, IXMLConfigurable {

    private String sourceCharset = null;

    /**
     * Gets the presumed source character encoding.
     * @return character encoding of the source to be transformed
     */
    public String getSourceCharset() {
        return sourceCharset;
    }
    /**
     * Sets the presumed source character encoding.
     * @param sourceCharset character encoding of the source to be transformed
     */
    public void setSourceCharset(String sourceCharset) {
        this.sourceCharset = sourceCharset;
    }

    @Override
    public final boolean testDocument(
            HandlerDoc doc, InputStream input, ParseState parseState)
                    throws ImporterHandlerException {
        String inputCharset = CharsetUtil.firstNonBlankOrUTF8(
                parseState,
                sourceCharset,
                doc.getDocInfo().getContentEncoding());
        try {
            InputStreamReader reader = new InputStreamReader(
                    IOUtil.toNonNullInputStream(input), inputCharset);
            return testDocument(doc, reader, parseState);
        } catch (UnsupportedEncodingException e) {
            throw new ImporterHandlerException(e);
        }
    }

    protected abstract boolean testDocument(
            HandlerDoc doc, Reader input, ParseState parseState)
            throws ImporterHandlerException;

    @Override
    public final void loadFromXML(XML xml) {
        setSourceCharset(xml.getString("@sourceCharset", sourceCharset));
        loadCharStreamConditionFromXML(xml);
    }
    @Override
    public final void saveToXML(XML xml) {
        xml.setAttribute("sourceCharset", sourceCharset);
        saveCharStreamConditionToXML(xml);
    }

    /**
     * Saves configuration settings specific to the implementing class.
     * The parent tag along with the "class" attribute are already written.
     * @param xml the XML
     */
    protected abstract void saveCharStreamConditionToXML(XML xml);
    /**
     * Loads configuration settings specific to the implementing class.
     * @param xml XML configuration
     */
    protected abstract void loadCharStreamConditionFromXML(XML xml);

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
        return new ReflectionToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}