/* Copyright 2014-2018 Norconex Inc.
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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.text.Regex;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractCharStreamFilter;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.AbstractStringFilter;
import com.norconex.importer.handler.filter.OnMatch;

/**
 * <p>Filters a document based on a pattern matching in its content.  Based
 * on document size, it is possible the pattern matching will be done
 * in chunks, sometimes not achieving expected results.  Consider
 * using {@link AbstractCharStreamFilter} if this is a concern.
 * Refer to {@link AbstractDocumentFilter} for the inclusion/exclusion logic.
 * </p>
 * <p>
 * <b>Since 2.2.0</b>, the following regular expression flags are always
 * active: {@link Pattern#MULTILINE} and {@link Pattern#DOTALL}.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.RegexContentFilter"
 *          onMatch="[include|exclude]"
 *          caseSensitive="[false|true]"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" &gt;
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
 * This example will accept only documents containing word "apple".
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.RegexContentFilter"
 *          onMatch="include" &gt;
 *      &lt;regex&gt;.*apple.*&lt;/regex&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class RegexContentFilter extends AbstractStringFilter {

    private boolean caseSensitive;
    private String regex;
    private Pattern cachedPattern;

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

    @Override
    protected boolean isStringContentMatching(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) throws ImporterHandlerException {

        if (StringUtils.isBlank(regex)) {
            return true;
        }
        return getCachedPattern().matcher(content).matches();
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
    protected void saveStringFilterToXML(XML xml) {
        xml.setAttribute("caseSensitive", caseSensitive);
        xml.addElement("regex", regex);
    }
    @Override
    protected void loadStringFilterFromXML(XML xml) {
        setRegex(xml.getString("regex", regex));
        setCaseSensitive(xml.getBoolean("@caseSensitive", caseSensitive));
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
