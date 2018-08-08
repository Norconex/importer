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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
/**
 * <p>Accepts or rejects a document based on whether any of the specified
 * metadata fields are empty or not.  Any control characters (char &lt;= 32)
 * are removed before evaluating if a property is empty or not.</p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.EmptyMetadataFilter"
 *          onMatch="[include|exclude]"
 *          fields="(coma separated list of fields to match)" &gt;
 *
 *    &lt;restrictTo caseSensitive="[false|true]"
 *            field="(name of header/metadata field name to match)"&gt;
 *        (regular expression of value to match)
 *    &lt;/restrictTo&gt;
 *    &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * To exclude documents without titles:
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.EmptyMetadataFilter"
 *          onMatch="exclude" fields="title,dc:title" /&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 1.2
 */
public class EmptyMetadataFilter extends AbstractDocumentFilter {

    private final List<String> fields = new ArrayList<>();


    public EmptyMetadataFilter() {
        this(OnMatch.INCLUDE, (String) null);
    }
    public EmptyMetadataFilter(
            OnMatch onMatch, String... fields) {
        super();
        this.fields.addAll(Arrays.asList(fields));
        setOnMatch(onMatch);
    }

    public List<String> getFields() {
        return Collections.unmodifiableList(fields);
    }
    public void setFields(String... fields) {
        setFields(Arrays.asList(fields));
    }
    public void setFields(List<String> fields) {
        CollectionUtil.setAll(this.fields, fields);
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (fields.isEmpty()) {
            return true;
        }
        for (String prop : fields) {
            Collection<String> values =  metadata.getStrings(prop);

            boolean isPropEmpty = true;
            for (String value : values) {
                if (!StringUtils.isBlank(StringUtils.trim(value))) {
                    isPropEmpty = false;
                    break;
                }
            }
            if (isPropEmpty) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void loadFilterFromXML(XML xml) {
        setFields(xml.getDelimitedStringList("@fields", fields));
    }

    @Override
    protected void saveFilterToXML(XML xml) {
        xml.setDelimitedAttributeList("fields", fields);
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

