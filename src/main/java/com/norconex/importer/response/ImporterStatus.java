/* Copyright 2014-2018 Norconex Inc.
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
package com.norconex.importer.response;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.importer.ImporterException;
import com.norconex.importer.handler.filter.IDocumentFilter;

/**
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterStatus {

    public enum Status { SUCCESS, REJECTED, ERROR };

    private final Status status;
    private final IDocumentFilter filter;
    private final ImporterException exception;
    private final String description;


    public ImporterStatus() {
        this(Status.SUCCESS, null, null, null);
    }
    public ImporterStatus(Status status, String description) {
        this(status, null, null, description);
    }
    public ImporterStatus(ImporterException e) {
        this(Status.ERROR, null, e, e.getLocalizedMessage());
    }
    public ImporterStatus(ImporterException e, String description) {
        this(Status.ERROR, null, e, description);
    }
    public ImporterStatus(IDocumentFilter filter) {
        this(filter, filter.toString());
    }
    public ImporterStatus(IDocumentFilter filter, String description) {
        this(Status.REJECTED, filter, null, description);
    }
    private ImporterStatus(
            Status s, IDocumentFilter f, ImporterException e, String d) {
        super();
        this.status = s;
        this.filter = f;
        this.exception = e;
        this.description = d;
    }

    public String getDescription() {
        return description;
    }
    public IDocumentFilter getRejectionFilter() {
        return filter;
    }
    public ImporterException getException() {
        return exception;
    }
    public Status getStatus() {
        return status;
    }
    public boolean isRejected() {
        return status == Status.REJECTED;
    }
    public boolean isError() {
        return status == Status.ERROR;
    }
    public boolean isSuccess() {
        return status == Status.SUCCESS;
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
        ReflectionToStringBuilder b = new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
        b.setExcludeNullValues(true);
        return b.toString();
    }
}
