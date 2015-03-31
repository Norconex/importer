/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.importer.handler.filter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class for filters dealing with the body of text documents only.  
 * Subclasses can safely be used as either pre-parse or post-parse handlers
 * restricted to text documents only (see {@link AbstractImporterHandler}).
 * </p>
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * <pre>
 *  &lt;!-- main tag supports onMatch="[include|exclude]" attribute --&gt;
 *  &lt;restrictTo
 *          caseSensitive="[false|true]"
 *          field="(name of header/metadata field name to match)"&gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public abstract class AbstractCharStreamFilter extends AbstractDocumentFilter {

    @Override
    protected final boolean isDocumentMatched(
            String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        //TODO shall we try to use document char encoding instead??
        try {
            InputStreamReader is = new InputStreamReader(
                    input, CharEncoding.UTF_8);
            return isTextDocumentMatching(reference, is, metadata, parsed);
        } catch (UnsupportedEncodingException e) {
            throw new ImporterHandlerException(e);
        }
    }

    protected abstract boolean isTextDocumentMatching(
            String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractCharStreamFilter)) {
            return false;
        }
        return new EqualsBuilder().appendSuper(super.equals(other)).isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).toHashCode();
    } 

}