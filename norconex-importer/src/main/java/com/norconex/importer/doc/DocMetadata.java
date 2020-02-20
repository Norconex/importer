/* Copyright 2020 Norconex Inc.
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
package com.norconex.importer.doc;

/**
 * <p>
 * Constants for common metadata field names typically associated
 * with a document and often set on {@link Doc#getMetadata()}.
 * </p>
 * <p>
 * While those may originally be set by the importer, it is possible for
 * implementors to overwrite, rename, or even delete them from a document
 * metadata.
 * </p>
 * @author Pascal Essiembre
 * @since 3.0.0
 */
public final class DocMetadata {

    //TODO DELETE these if they can be referenced from DocInfo?
    //    (still have them as metadata, just no longer need constants).
    private static final String PREFIX = "document.";
    public static final String REFERENCE = PREFIX + "reference";
    public static final String CONTENT_TYPE = PREFIX + "contentType";
    public static final String CONTENT_ENCODING = PREFIX + "contentEncoding";
    public static final String CONTENT_FAMILY = PREFIX + "contentFamily";
    public static final String LANGUAGE = PREFIX + "language";
    public static final String TRANSLATED_FROM = PREFIX + "translatedFrom";
    public static final String GENERATED_TITLE = PREFIX + "generatedTitle";
    public static final String IMPORTED_DATE = PREFIX + "importedDate";
    static final String EMBEDDED_PREFIX = PREFIX + "embedded.";
    public static final String EMBEDDED_PARENT_REFERENCES =
            EMBEDDED_PREFIX + "parent.reference";
    public static final String EMBEDDED_REFERENCE =
            EMBEDDED_PREFIX + "reference";
    public static final String EMBEDDED_TYPE =
            EMBEDDED_PREFIX + "type";

    private DocMetadata() {
        super();
    }
}
