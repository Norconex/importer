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
package com.norconex.importer;

import java.util.List;
import java.util.Map;

import com.norconex.commons.lang.file.ContentFamily;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.map.Properties;

/**
 * @author Pascal Essiembre
 *
 */
public class ImporterMetadata extends Properties {

    //TODO have all fields from importer start with document instead
    // of importer??
    
    private static final long serialVersionUID = -2568349411949228998L;

    public static final String IMPORTER_PREFIX = "document.";

    public static final String DOC_REFERENCE = IMPORTER_PREFIX + "reference";

    public static final String DOC_CONTENT_TYPE = 
            IMPORTER_PREFIX + "contentType";
    public static final String DOC_CONTENT_FAMILY = 
            IMPORTER_PREFIX + "contentFamily";

    public static final String DOC_PARENT_REFERENCE = 
            IMPORTER_PREFIX + "parent.reference";
    public static final String DOC_PARENT_CONTENT_TYPE = 
            IMPORTER_PREFIX + "parent.contentType";
    
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
    
    public ContentType getContentType() {
        return ContentType.valueOf(getString(DOC_CONTENT_TYPE));
    }
    public void setContentType(ContentType contentType) {
        if (contentType == null) {
            setString(DOC_CONTENT_TYPE, (String) null);
        } else {
            setString(DOC_CONTENT_TYPE, contentType.toString());
        }
    }
    public void setContentType(String contentType) {
        setString(DOC_CONTENT_TYPE, contentType);
    }

    public ContentFamily getContentFamily() {
        return ContentFamily.valueOf(getString(DOC_CONTENT_FAMILY));
    }
    public void setContentFamily(ContentFamily contentFamily) {
        if (contentFamily == null) {
            setString(DOC_CONTENT_FAMILY, (String) null);
        } else {
            setString(DOC_CONTENT_FAMILY, contentFamily.toString());
        }
    }
    public void setContentFamily(String contentFamily) {
        setString(DOC_CONTENT_FAMILY, contentFamily);
    }

    public String getDocumentReference() {
        return getString(DOC_REFERENCE);
    }
    public void setDocumentReference(String documentReference) {
        setString(DOC_REFERENCE, documentReference);
    }

    public String getParentReference() {
        return getString(DOC_PARENT_REFERENCE);
    }
    public void setParentReference(String parentReference) {
        setString(DOC_PARENT_REFERENCE, parentReference);
    }
    
    //TODO really have this is we go with suggestion above?
    public String getParentContentType() {
        return getString(DOC_PARENT_CONTENT_TYPE);
    }
    public void setParentContentType(String parentContentType) {
        setString(DOC_PARENT_CONTENT_TYPE, parentContentType);
    }
}
