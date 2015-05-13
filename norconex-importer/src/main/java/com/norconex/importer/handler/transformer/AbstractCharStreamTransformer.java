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
package com.norconex.importer.handler.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * Base class for transformers dealing with text documents only.
 * Subclasses can safely be used as either pre-parse or post-parse handlers
 * restricted to text documents only (see {@link AbstractImporterHandler}).
 * <br><br>
 * Sub-classes can restrict to which document to apply this transformation
 * based on document metadata (see {@link AbstractImporterHandler}).
 * <br><br>
 * Subclasses implementing {@link IXMLConfigurable} should allow this inner 
 * configuration:
 * <pre>
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public abstract class AbstractCharStreamTransformer 
            extends AbstractDocumentTransformer {

    @Override
    protected final void transformApplicableDocument(
            String reference, InputStream input,
            OutputStream output, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        try {
            InputStreamReader is = new InputStreamReader(input, CharEncoding.UTF_8);
            OutputStreamWriter os = 
                    new OutputStreamWriter(output, CharEncoding.UTF_8);
            transformTextDocument(reference, is, os, metadata, parsed);
            os.flush();
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot transform character stream.", e);
        }
    }

    protected abstract void transformTextDocument(
            String reference, Reader input,
            Writer output, ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractCharStreamTransformer)) {
            return false;
        }
        return new EqualsBuilder().appendSuper(super.equals(other)).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).toHashCode();
    } 
}