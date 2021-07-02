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
package com.norconex.importer.handler.filter.impl;

import java.io.InputStream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.IDocumentFilter;
import com.norconex.importer.parser.ParseState;
/**
 * <p>
 * Rejects a document. A rejected document is not processed further and
 * does not generate any output by the Importer.
 * </p>
 * <p>
 * Typically used within Importer XML flow conditions in your XML configuration.
 * </p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.filter.impl.RejectFilter"/>
 * }
 *
 * {@nx.xml.example
 *  <handler class="RejectFilter"/>
 *
 *  <!-- Alternatively, when used in XML flow: -->
 *  <reject/>
 * }
 *
 * @author Pascal Essiembre
 * @since 3.0.0
 */
public final class RejectFilter implements IDocumentFilter {

    public static final RejectFilter INSTANCE = new RejectFilter();

    @Override
    public boolean acceptDocument(
            HandlerDoc doc, InputStream input, ParseState parseState)
                    throws ImporterHandlerException {
        return false;
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

