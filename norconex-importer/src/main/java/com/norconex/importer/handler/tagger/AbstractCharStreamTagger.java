/* Copyright 2010-2015 Norconex Inc.
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
package com.norconex.importer.handler.tagger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class for taggers dealing with the body of text documents only.  
 * Subclasses can safely be used as either pre-parse or post-parse handlers
 * restricted to text documents only (see {@link AbstractImporterHandler}).
 * </p>
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * <pre>
 *  &lt;restrictTo
 *          caseSensitive="[false|true]"
 *          field="(name of header/metadata field name to match)"&gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public abstract class AbstractCharStreamTagger extends AbstractDocumentTagger {

    @Override
    protected final void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException {
        
        String contentType = metadata.getString("Content-Type", "");
        contentType = StringUtils.substringBefore(contentType, ";");
        
        String charset = metadata.getString("Content-Encoding", null);
        if (charset == null) {
            charset = metadata.getString("charset", null);
        }
        if (charset == null) {
            for (String type : metadata.getStrings("Content-Type")) {
                if (type.contains("charset")) {
                    charset = StringUtils.trimToNull(StringUtils.substringAfter(
                            type, "charset="));
                    break;
                }
            }
        }
        if (StringUtils.isBlank(charset) 
                || !CharEncoding.isSupported(charset)) {
            charset = CharEncoding.UTF_8;
        }
        try {
            InputStreamReader is = new InputStreamReader(document, charset);
            tagTextDocument(reference, is, metadata, parsed);
        } catch (UnsupportedEncodingException e) {
            throw new ImporterHandlerException(e);
        }
    }

    protected abstract void tagTextDocument(
            String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException;
}