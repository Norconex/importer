/* Copyright 2015-2016 Norconex Inc.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
     * Gets the regular expression matching content types to restrict OCR to.
     * @return content types
     */
    public String getContentTypes() {
        return contentTypes;
    }
    /**
     * Sets the regular expression matching content types to restrict OCR to.
     * @param contentTypes content types
     */
    public void setContentTypes(String contentTypes) {
        this.contentTypes = contentTypes;
    }
    
    public boolean isEmpty() {
        return  StringUtils.isBlank(path)
                && StringUtils.isBlank(languages)
                && StringUtils.isBlank(contentTypes);
    }

    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof OCRConfig)) {
            return false;
        }
        OCRConfig castOther = (OCRConfig) other;
        return new EqualsBuilder()
                .append(path, castOther.path)
                .append(languages, castOther.languages)
                .append(contentTypes, castOther.contentTypes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(path)
                .append(languages)
                .append(contentTypes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", path)
                .append("languages", languages)
                .append("contentTypes", contentTypes)
                .toString();
    }
}
