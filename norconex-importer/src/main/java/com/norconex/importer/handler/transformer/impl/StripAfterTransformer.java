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
 * <p>Strips any content found after first match found for given pattern.</p>
 *
 * <p>This class can be used as a pre-parsing (text content-types only)
 * or post-parsing handlers.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.StripAfterTransformer"
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
 *      &lt;stripAfterRegex&gt;(regex)&lt;/stripAfterRegex&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following will strip all text starting with this HTML comment and
 * everything after it:
 * <code>&lt;!-- FOOTER --&gt;</code>.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.StripAfterTransformer"
 *          inclusive="true"&gt;
 *      &lt;stripAfterRegex&gt;&lt;![CDATA[&lt;!-- FOOTER --&gt;]]&gt;&lt;/stripAfterRegex&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class StripAfterTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(StripAfterTransformer.class);

    private boolean inclusive;
    private boolean caseSensitive;
    private String stripAfterRegex;

    @Override
    protected void transformStringContent(final String reference,
            final StringBuilder content, final ImporterMetadata metadata, final boolean parsed,
            final int sectionIndex) {
        if (stripAfterRegex == null) {
            LOG.error("No regular expression provided.");
            return;
        }

        Pattern pattern = Regex.compileDotAll(stripAfterRegex, !caseSensitive);
//        int flags = Pattern.DOTALL;
//        if (!caseSensitive) {
//            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
//        }
//        Pattern pattern = Pattern.compile(stripAfterRegex, flags);
        Matcher match = pattern.matcher(content);
        if (match.find()) {
            if (inclusive) {
                content.delete(match.start(), content.length());
            } else {
                content.delete(match.end(), content.length());
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

    public String getStripAfterRegex() {
        return stripAfterRegex;
    }
    public void setStripAfterRegex(final String regex) {
        this.stripAfterRegex = regex;
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        setCaseSensitive(xml.getBoolean("@caseSensitive", caseSensitive));
        setInclusive(xml.getBoolean("@inclusive", inclusive));
        setStripAfterRegex(xml.getString("stripAfterRegex", stripAfterRegex));
    }

    @Override
    protected void saveStringTransformerToXML(final XML xml) {
        xml.setAttribute("caseSensitive", caseSensitive);
        xml.setAttribute("inclusive", inclusive);
        xml.addElement("stripAfterRegex", stripAfterRegex);
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
