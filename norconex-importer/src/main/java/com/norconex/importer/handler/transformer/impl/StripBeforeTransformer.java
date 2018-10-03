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
package com.norconex.importer.handler.transformer.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.regex.Regex;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Strips any content found before first match found for given pattern.</p>
 *
 * <p>This class can be used as a pre-parsing (text content-types only)
 * or post-parsing handlers.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.StripBeforeTransformer"
 *          inclusive="[false|true]"
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
 *      &lt;stripBeforeRegex&gt;(regex)&lt;/stripBeforeRegex&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following will strip all text up to and including this HTML comment:
 * <code>&lt;!-- HEADER_END --&gt;</code>.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.StripBeforeTransformer"
 *          inclusive="true"&gt;
 *      &lt;stripBeforeRegex&gt;&lt;![CDATA[&lt;!-- HEADER_END --&gt;]]&gt;&lt;/stripBeforeRegex&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class StripBeforeTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(StripBeforeTransformer.class);

    private boolean inclusive;
    private boolean caseSensitive;
    private String stripBeforeRegex;

    @Override
    protected void transformStringContent(final String reference,
            final StringBuilder content, final ImporterMetadata metadata, final boolean parsed,
            final int sectionIndex) {
        if (stripBeforeRegex == null) {
            LOG.error("No regular expression provided.");
            return;
        }
//        int flags = Pattern.DOTALL;
//        if (!caseSensitive) {
//            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
//        }
//        Pattern pattern = Pattern.compile(stripBeforeRegex, flags);
        Pattern pattern = Regex.compileDotAll(stripBeforeRegex, !caseSensitive);
        Matcher match = pattern.matcher(content);
        if (match.find()) {
            if (inclusive) {
                content.delete(0, match.end());
            } else {
                content.delete(0, match.start());
            }
        }
    }

    public boolean isInclusive() {
        return inclusive;
    }
    /**
     * Sets whether the match itself should be stripped or not.
     * @param inclusive <code>true</code> to strip start and end text
     */
    public void setInclusive(final boolean inclusive) {
        this.inclusive = inclusive;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * Sets whether to ignore case when matching text.
     * @param caseSensitive <code>true</code> to consider character case
     */
    public void setCaseSensitive(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getStripBeforeRegex() {
        return stripBeforeRegex;
    }
    public void setStripBeforeRegex(final String regex) {
        this.stripBeforeRegex = regex;
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        setCaseSensitive(xml.getBoolean("@caseSensitive", caseSensitive));
        setInclusive(xml.getBoolean("@inclusive", inclusive));
        setStripBeforeRegex(
                xml.getString("stripBeforeRegex", stripBeforeRegex));
    }

    @Override
    protected void saveStringTransformerToXML(final XML xml) {
        xml.setAttribute("caseSensitive", caseSensitive);
        xml.setAttribute("inclusive", inclusive);
        xml.addElement("stripBeforeRegex", stripBeforeRegex);
    }

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
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
