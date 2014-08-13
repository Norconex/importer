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

import java.io.Serializable;

import com.norconex.importer.doc.ImporterDocument;

/**
 * @author Pascal Essiembre
 *
 */
public class ImporterResult implements Serializable {

    private static final long serialVersionUID = -103736468554516603L;

    private final ImporterFilterStatus filterStatus;
    private final ImporterDocument doc;
    
    /*default*/ ImporterResult(ImporterFilterStatus filterStatus) {
        this.filterStatus = filterStatus;
        this.doc = null;
    }
    /*default*/ ImporterResult(ImporterDocument doc) {
        this.doc = doc;
        this.filterStatus = new ImporterFilterStatus();
    }

    public ImporterDocument getDocument() {
        return doc;
    }

    public ImporterFilterStatus getFilterStatus() {
        return filterStatus;
    }
    
    public boolean isRejected() {
        return filterStatus != null && filterStatus.isRejected();
    }
    public String getRejectionDescription() {
        if (isRejected()) {
            return filterStatus.getDescription();
        }
        return null;
    }
}
