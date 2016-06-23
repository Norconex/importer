/* Copyright 2016 Norconex Inc.
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
 * Configuration settings affecting how embedded documents are handled
 * by parsers.
 * @author Pascal Essiembre
 * @since 2.6.0
 */
public class EmbeddedConfig {
    
    private String splitContentTypes;
    private String noExtractEmbeddedContentTypes;
    private String noExtractContainerContentTypes;

    public String getSplitContentTypes() {
        return splitContentTypes;
    }
    public void setSplitContentTypes(String splitContentTypes) {
        this.splitContentTypes = splitContentTypes;
    }
    public String getNoExtractEmbeddedContentTypes() {
        return noExtractEmbeddedContentTypes;
    }
    public void setNoExtractEmbeddedContentTypes(
            String noExtractEmbeddedContentTypes) {
        this.noExtractEmbeddedContentTypes = noExtractEmbeddedContentTypes;
    }
    public String getNoExtractContainerContentTypes() {
        return noExtractContainerContentTypes;
    }
    public void setNoExtractContainerContentTypes(
            String noExtractContainerContentTypes) {
        this.noExtractContainerContentTypes = noExtractContainerContentTypes;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(splitContentTypes)
                && StringUtils.isBlank(noExtractContainerContentTypes)
                && StringUtils.isBlank(noExtractEmbeddedContentTypes);
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof EmbeddedConfig)) {
            return false;
        }
        EmbeddedConfig castOther = (EmbeddedConfig) other;
        return new EqualsBuilder()
                .append(splitContentTypes, castOther.splitContentTypes)
                .append(noExtractEmbeddedContentTypes, 
                        castOther.noExtractEmbeddedContentTypes)
                .append(noExtractContainerContentTypes, 
                        castOther.noExtractContainerContentTypes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(splitContentTypes)
                .append(noExtractEmbeddedContentTypes)
                .append(noExtractContainerContentTypes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("splitEmbedded", splitContentTypes)
                .append("noExtractEmbeddedContentTypes", 
                        noExtractEmbeddedContentTypes)
                .append("noExtractContainerContentTypes", 
                        noExtractContainerContentTypes)
                .toString();
    }
}
