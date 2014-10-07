/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

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
