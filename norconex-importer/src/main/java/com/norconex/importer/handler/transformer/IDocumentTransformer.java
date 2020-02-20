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

import java.io.InputStream;
import java.io.OutputStream;

import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * Transformers allow to manipulate and modify a document metadata or content.
 * 
 * @author Pascal Essiembre
 */
public interface IDocumentTransformer extends IImporterHandler {

    /**
     * Transforms document content and metadata.
     * @param reference document reference (e.g. URL)
     * @param input document to transform
     * @param output transformed document
     * @param metadata document metadata
     * @param parsed whether the document has been parsed already or not (a 
     *        parsed document should normally be text-based)
     * @throws ImporterHandlerException could not transform the document
     */
    void transformDocument(
            String reference, InputStream input, 
            OutputStream output, Properties metadata, boolean parsed)
                    throws ImporterHandlerException;
}
