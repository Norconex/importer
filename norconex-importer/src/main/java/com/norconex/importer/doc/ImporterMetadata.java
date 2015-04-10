/* Copyright 2014 Norconex Inc.
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

import java.util.List;
import java.util.Map;

import com.norconex.commons.lang.map.Properties;

/**
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterMetadata extends Properties {

    private static final long serialVersionUID = -2568349411949228998L;

    private static final String DOC_META_PREFIX = "document.";

    public static final String DOC_REFERENCE = DOC_META_PREFIX + "reference";
    public static final String DOC_CONTENT_TYPE = 
            DOC_META_PREFIX + "contentType";
    public static final String DOC_CONTENT_ENCODING = 
            DOC_META_PREFIX + "contentEncoding";
    
    public static final String DOC_CONTENT_FAMILY = 
            DOC_META_PREFIX + "contentFamily";
    public static final String DOC_LANGUAGE = 
            DOC_META_PREFIX + "language";
    public static final String DOC_TRANSLATED_FROM = 
            DOC_META_PREFIX + "translatedFrom";
    public static final String DOC_GENERATED_TITLE = 
            DOC_META_PREFIX + "generatedTitle";
    public static final String DOC_IMPORTED_DATE = 
            DOC_META_PREFIX + "importedDate";

    private static final String DOC_EMBEDDED_META_PREFIX = 
            DOC_META_PREFIX + "embedded.";
    public static final String DOC_EMBEDDED_PARENT_REFERENCE =
            DOC_EMBEDDED_META_PREFIX + "parent.reference";
    public static final String DOC_EMBEDDED_PARENT_ROOT_REFERENCE =
            DOC_EMBEDDED_META_PREFIX + "parent.root.reference";
    public static final String DOC_EMBEDDED_REFERENCE =
            DOC_EMBEDDED_META_PREFIX + "reference";
    public static final String DOC_EMBEDDED_TYPE =
            DOC_EMBEDDED_META_PREFIX + "type";
    
    public ImporterMetadata() {
        super();
    }
    public ImporterMetadata(boolean caseSensitiveKeys) {
        super(caseSensitiveKeys);
    }
    public ImporterMetadata(Map<String, List<String>> map,
            boolean caseSensitiveKeys) {
        super(map, caseSensitiveKeys);
    }
    public ImporterMetadata(Map<String, List<String>> map) {
        super(map);
    }
    
    public String getLanguage() {
        return getString(DOC_LANGUAGE);
    }
    public void setLanguage(String language) {
        setString(DOC_LANGUAGE, language);
    }

    public String getReference() {
        return getString(DOC_REFERENCE);
    }
    public void setReference(String documentReference) {
        setString(DOC_REFERENCE, documentReference);
    }

    public String getEmbeddedParentReference() {
        return getString(DOC_EMBEDDED_PARENT_REFERENCE);
    }
    public void setEmbeddedParentReference(String parentReference) {
        setString(DOC_EMBEDDED_PARENT_REFERENCE, parentReference);
    }

    public String getEmbeddedParentRootReference() {
        return getString(DOC_EMBEDDED_PARENT_ROOT_REFERENCE);
    }
    public void setEmbeddedParentRootReference(String parentRootReference) {
        setString(DOC_EMBEDDED_PARENT_ROOT_REFERENCE, parentRootReference);
    }
    
    public String getEmbeddedReference() {
        return getString(DOC_EMBEDDED_REFERENCE);
    }
    public void setEmbeddedReference(String reference) {
        setString(DOC_EMBEDDED_REFERENCE, reference);
    }

    public String getEmbeddedType() {
        return getString(DOC_EMBEDDED_TYPE);
    }
    public void setEmbeddedType(String type) {
        setString(DOC_EMBEDDED_TYPE, type);
    }
}
