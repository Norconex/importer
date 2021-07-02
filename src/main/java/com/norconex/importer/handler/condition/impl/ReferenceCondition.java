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
package com.norconex.importer.handler.condition.impl;

import java.io.InputStream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.condition.IImporterCondition;
import com.norconex.importer.parser.ParseState;

/**
 * <p
 * A condition based on a text pattern matching a document reference (e.g. URL).
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * {@nx.xml.usage
 * <condition class="com.norconex.importer.handler.condition.impl.ReferenceCondition">
 *   <valueMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *     (expression of reference value to match)
 *   </valueMatcher>
 * </condition>
 * }
 *
 * {@nx.xml.example
 * <condition class="ReferenceCondition">
 *   <valueMatcher method="regex">.*&#47;login/.*</valueMatcher>
 * </condition>
 * }
 * <p>
 * The above example reject documents having "/login/" in their reference.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 3.0.0
 */
@SuppressWarnings("javadoc")
public class ReferenceCondition
        implements IImporterCondition, IXMLConfigurable {

    private final TextMatcher valueMatcher = new TextMatcher();

    public ReferenceCondition() {
        super();
    }
    public ReferenceCondition(TextMatcher valueMatcher) {
        setValueMatcher(valueMatcher);
    }

    /**
     * Gets the text matcher for field values.
     * @return text matcher
     */
    public TextMatcher getValueMatcher() {
        return valueMatcher;
    }
    /**
     * Sets the text matcher for field values. Copies it.
     * @param valueMatcher text matcher
     */
    public void setValueMatcher(TextMatcher valueMatcher) {
        this.valueMatcher.copyFrom(valueMatcher);
    }

    @Override
    public boolean testDocument(HandlerDoc doc, InputStream input,
            ParseState parseState) throws ImporterHandlerException {
        return valueMatcher.matches(doc.getReference());
    }
    @Override
    public void loadFromXML(XML xml) {
        valueMatcher.loadFromXML(xml.getXML("valueMatcher"));
    }
    @Override
    public void saveToXML(XML xml) {
        valueMatcher.saveToXML(xml.addElement("valueMatcher"));
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
                this, ToStringStyle.SHORT_PREFIX_STYLE)
                .toString();
    }
}
