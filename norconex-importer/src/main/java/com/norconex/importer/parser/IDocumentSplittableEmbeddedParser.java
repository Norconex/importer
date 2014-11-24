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
package com.norconex.importer.parser;


/**
 * Implementations are responsible for parsing a document (InputStream) to 
 * extract its text and metadata.  This parser also supports splitting
 * embedded documents into individual documents.
 * @author Pascal Essiembre
 * @see IDocumentParserFactory
 * @since 2.0.0
 */
@SuppressWarnings("nls")
public interface IDocumentSplittableEmbeddedParser extends IDocumentParser {

    /**
     * Sets whether to split embedded documents whenever possible.
     * @param splitEmbedded <code>true</code> to split embedded documents
     */
    void setSplitEmbedded(boolean splitEmbedded);
}
