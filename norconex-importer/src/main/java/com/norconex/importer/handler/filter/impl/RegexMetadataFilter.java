/* Copyright 2010-2018 Norconex Inc.
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
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
/**
 * <p>Accepts or rejects a document based on its field values using
 * regular expression.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.RegexMetadataFilter"
 *          onMatch="[include|exclude]"
 *          caseSensitive="[false|true]"
 *          field="(name of metadata name to match)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;regex&gt;(regular expression of value to match)&lt;/regex&gt;
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * This example will accept only documents containing word "potato"
 * in the title.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.RegexMetadataFilter"
 *          onMatch="include" field="title" &gt;
 *      &lt;regex&gt;.*potato.*&lt;/regex&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 */
public class RegexMetadataFilter extends AbstractDocumentFilter {

    private boolean caseSensitive;
    private String field;
    private String regex;
    private Pattern cachedPattern;

    public RegexMetadataFilter() {
        this(null, null, OnMatch.INCLUDE);
    }
    public RegexMetadataFilter(String field, String regex) {
        this(field, regex, OnMatch.INCLUDE);
    }
    public RegexMetadataFilter(String field, String regex, OnMatch onMatch) {
        this(field, regex, onMatch, false);
    }
    public RegexMetadataFilter(
            String property, String regex,
            OnMatch onMatch, boolean caseSensitive) {
        super();
        this.caseSensitive = caseSensitive;
        this.field = property;
        setOnMatch(onMatch);
        setRegex(regex);
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
    public String getField() {
        return field;
    }
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        cachedPattern = null;
    }
    public void setField(String property) {
        this.field = property;
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (StringUtils.isBlank(regex)) {
            return true;
        }
        Collection<String> values =  metadata.getStrings(field);
        for (Object value : values) {
            String strVal = Objects.toString(value, StringUtils.EMPTY);
            if (getCachedPattern().matcher(strVal).matches()) {
                return true;
            }
        }
        return false;
    }

    private synchronized Pattern getCachedPattern() {
        if (cachedPattern != null) {
            return cachedPattern;
        }
        Pattern p;
        if (regex == null) {
            p = Pattern.compile(".*");
        } else {
            int flags = Pattern.DOTALL;
            if (!caseSensitive) {
                flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }
            p = Pattern.compile(regex, flags);
        }
        cachedPattern = p;
        return p;
    }

    @Override
    protected void loadFilterFromXML(XML xml) throws IOException {
        setField(xml.getString("@field"));
        setCaseSensitive(xml.getBoolean("@caseSensitive", false));
        setRegex(xml.getString("regex", getRegex()));
    }

    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("field", field);
        writer.writeAttributeBoolean("caseSensitive", caseSensitive);
        writer.writeElementString("regex", regex);
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

