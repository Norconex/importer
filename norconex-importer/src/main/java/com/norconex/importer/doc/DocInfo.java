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

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.file.ContentType;

/**
 * Important information about a document that has specific meaning and purpose
 * for processing by the Importer and needs to be referenced in a constant way.
 * Those are information needing to be tracked independently from metadata,
 * which can be anything, and can be modified at will by implementors
 * (thus not always constant).
 * In most cases where light caching is involved, implementors can cache this
 * class data as opposed to caching {@link ImporterDocument}.
 * @author Pascal Essiembre
 * @since 3.0.0
 */
public class DocInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    //TODO rename DocumentMemento?  Or DocMemento   Or DocRecord?
//  DocProfile?  DocSpecs?  DocInfo? Specifics? Facts? Profile?
    // DocInfo


    //TODO add parent reference info here???

    //TODO remove most ImporterMetadata method and put them here.

    //TODO track original vs final here (useful for tracking deletions
    // under a modified reference (and have dynamic committer targets).
    //TODO make final?
    private String reference;
    private ContentType contentType;
    private String contentEncoding;

    /**
     * Constructor.
     */
    public DocInfo() {
        super();
    }

    /**
     * Constructor.
     * @param reference document reference
     */
    public DocInfo(String reference) {
        setReference(reference);
    }
    /**
     * Copy constructor.
     * @param docInfo document details to copy
     */
    public DocInfo(DocInfo docInfo) {
        Objects.requireNonNull(docInfo, "'docDetails' must not be null.");
        copyFrom(docInfo);
    }

    public String getReference() {
        return reference;
    }
    public void setReference(String reference) {
        Objects.requireNonNull(reference, "'reference' must not be null.");
        this.reference = reference;
    }

    public ContentType getContentType() {
        return contentType;
    }
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public void copyTo(DocInfo target) {
        BeanUtil.copyProperties(target, this);
    }
    public void copyFrom(DocInfo source) {
        BeanUtil.copyProperties(this, source);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
