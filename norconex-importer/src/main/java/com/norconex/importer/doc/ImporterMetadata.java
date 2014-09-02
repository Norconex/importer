/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.doc;

import java.util.List;
import java.util.Map;

import com.norconex.commons.lang.map.Properties;

/**
 * @author Pascal Essiembre
 *
 */
public class ImporterMetadata extends Properties {

    //TODO have all fields from importer start with document instead
    // of importer??
    
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
    
    //TODO remove content type and content family method to avoid
    // conflicts with HTTP Collector?
//    public ContentType getContentType() {
//        return ContentType.valueOf(getString(DOC_CONTENT_TYPE));
//    }
//    public void setContentType(ContentType contentType) {
//        if (contentType == null) {
//            setString(DOC_CONTENT_TYPE, (String) null);
//        } else {
//            setString(DOC_CONTENT_TYPE, contentType.toString());
//        }
//    }
//    public void setContentType(String contentType) {
//        setString(DOC_CONTENT_TYPE, contentType);
//    }

//    public ContentFamily getContentFamily() {
//        return ContentFamily.valueOf(getString(DOC_CONTENT_FAMILY));
//    }
//    public void setContentFamily(ContentFamily contentFamily) {
//        if (contentFamily == null) {
//            setString(DOC_CONTENT_FAMILY, (String) null);
//        } else {
//            setString(DOC_CONTENT_FAMILY, contentFamily.toString());
//        }
//    }
//    public void setContentFamily(String contentFamily) {
//        setString(DOC_CONTENT_FAMILY, contentFamily);
//    }
    
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
