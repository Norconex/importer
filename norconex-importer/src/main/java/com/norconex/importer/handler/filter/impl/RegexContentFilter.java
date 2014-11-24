/* Copyright 2014 Norconex Inc.
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
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractCharStreamFilter;
import com.norconex.importer.handler.filter.AbstractStringFilter;
import com.norconex.importer.handler.filter.OnMatch;

/**
 * Filters a document based on a pattern matching in its content.  Based
 * on document site, it is possible the pattern matching will be done
 * in chunks, sometimes not achieving expected results.  Consider
 * using {@link AbstractCharStreamFilter} if this is a concern.
 * 
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.RegexContentFilter"
 *          onMatch="[include|exclude]" 
 *          caseSensitive="[false|true]" &gt;
 *      (regular expression of value to match)
 *  &lt;/filter&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class RegexContentFilter extends AbstractStringFilter {

    private boolean caseSensitive;
    private String regex;
    private Pattern pattern;

    
    public RegexContentFilter() {
        this(null, OnMatch.INCLUDE);
    }
    public RegexContentFilter(String regex) {
        this(regex, OnMatch.INCLUDE);
    }
    public RegexContentFilter(String regex, OnMatch onMatch) {
        this(regex, onMatch, false);
    }
    public RegexContentFilter(String regex, 
            OnMatch onMatch, boolean caseSensitive) {
        super();
        this.caseSensitive = caseSensitive;
        setOnMatch(onMatch);
        setRegex(regex);
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
        if (regex != null) {
            if (caseSensitive) {
                this.pattern = Pattern.compile(regex);
            } else {
                this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            }
        } else {
            this.pattern = Pattern.compile(".*");
        }
    }
    
    @Override
    protected boolean isStringContentMatching(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            boolean partialContent) throws ImporterHandlerException {

        if (StringUtils.isBlank(regex)) {
            return true;
        }
        if (pattern.matcher(content).matches()) {
            return true;
        }
        return false;
    }
    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute("caseSensitive", 
                Boolean.toString(caseSensitive));
        writer.writeCharacters(regex == null ? "" : regex);
    }
    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        setRegex(xml.getString(""));
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("caseSensitive", caseSensitive)
                .append("regex", regex).append("pattern", pattern).toString();
    }
    
}
