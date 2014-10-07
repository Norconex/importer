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
package com.norconex.importer.response;

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
}
