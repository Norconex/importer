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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.norconex.importer.doc.ImporterDocument;

/**
 * @author Pascal Essiembre
 *
 */
public class ImporterResponse implements Serializable {

    private static final long serialVersionUID = -103736468554516603L;

    public static final ImporterResponse[] EMPTY_RESPONSES = 
            new ImporterResponse[] {};
    
    private final String reference;
    private ImporterStatus status;
    private final ImporterDocument doc;
    private final List<ImporterResponse> nestedResponses = new ArrayList<>();
    private ImporterResponse parentResponse;
    
    public ImporterResponse(String reference, ImporterStatus status) {
        this.reference = reference;
        this.status = status;
        this.doc = null;
    }
    public ImporterResponse(ImporterDocument doc) {
        this.reference = doc.getReference();
        this.doc = doc;
        this.status = new ImporterStatus();
    }

    public ImporterDocument getDocument() {
        return doc;
    }

    public ImporterStatus getImporterStatus() {
        return status;
    }
    public void setImporterStatus(ImporterStatus status) {
        this.status = status;
    }
    
    public boolean isSuccess() {
        return status != null && status.isSuccess();
    }

    public String getReference() {
        return reference;
    }
  
    public ImporterResponse getParentResponse() {
        return parentResponse;
    }
    
    
    public void addNestedResponse(ImporterResponse response) {
        response.setParentResponse(this);
        nestedResponses.add(response);
    }
    public void removeNestedResponse(String reference) {
        ImporterResponse response = null;
        for (ImporterResponse nestedResponse : nestedResponses) {
            if (nestedResponse.getReference().equals(reference)) {
                response = nestedResponse;
            }
        }
        if (response == null) {
            return;
        }
        nestedResponses.remove(response);
        response.setParentResponse(null);
    }
    
    public ImporterResponse[] getNestedResponses() {
        return nestedResponses.toArray(EMPTY_RESPONSES);
    }
    
    private void setParentResponse(ImporterResponse parentResponse) {
        this.parentResponse = parentResponse;
    }
}
