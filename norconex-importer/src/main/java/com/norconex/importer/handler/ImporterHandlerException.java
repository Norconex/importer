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
package com.norconex.importer.handler;

import com.norconex.importer.ImporterException;

/**
 * Exception thrown by several handler classes upon encountering
 * issues.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterHandlerException extends ImporterException {

    private static final long serialVersionUID = 6845549545987836093L;

    public ImporterHandlerException() {
        super();
    }

    public ImporterHandlerException(String message) {
        super(message);
    }

    public ImporterHandlerException(Throwable cause) {
        super(cause);
    }

    public ImporterHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

}
