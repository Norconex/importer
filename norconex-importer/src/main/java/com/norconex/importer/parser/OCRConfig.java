/* Copyright 2015 Norconex Inc.
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

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * OCR configuration details.
 * @author Pascal Essiembre
 * @since 2.1.0
 */
public class OCRConfig {

    private String path;
    private String languages;
    private String contentTypes;
    
    /**
     * Constructor.
     */
    public OCRConfig() {
        super();
    }

    /**
     * Gets the installation path of OCR engine (i.e. Tesseract).
     * @return path
     */
    public String getPath() {
        return path;
    }
    /**
     * Sets the installation path of OCR engine (i.e. Tesseract).
     * @param path installation path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets languages to use by OCR.
     * @return languages
     */
    public String getLanguages() {
        return languages;
    }
    /**
     * Sets languages to use by OCR.
     * @param languages languages to use by OCR.
     */
    public void setLanguages(String languages) {
        this.languages = languages;
    }

    /**
     * Gets content types to restrict OCR to.
     * @return content types
     */
    public String getContentTypes() {
        return contentTypes;
    }
    /**
     * Sets content types to restrict OCR to.
     * @param contentTypes content types
     */
    public void setContentTypes(String contentTypes) {
        this.contentTypes = contentTypes;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((contentTypes == null) ? 0 : contentTypes.hashCode());
        result = prime * result
                + ((languages == null) ? 0 : languages.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OCRConfig)) {
            return false;
        }
        OCRConfig other = (OCRConfig) obj;
        if (contentTypes == null) {
            if (other.contentTypes != null) {
                return false;
            }
        } else if (!contentTypes.equals(other.contentTypes)) {
            return false;
        }
        if (languages == null) {
            if (other.languages != null) {
                return false;
            }
        } else if (!languages.equals(other.languages)) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("path", path);
        builder.append("languages", languages);
        builder.append("contentTypes", contentTypes);
        return builder.toString();
    }
    
    
}
