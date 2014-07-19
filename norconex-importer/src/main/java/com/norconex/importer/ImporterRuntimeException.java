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

/**
 * RuntimeException thrown when a an issue prevented the proper importation of a 
 * file.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ImporterRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 6785842126379940233L;

    public ImporterRuntimeException() {
        super();
    }

    public ImporterRuntimeException(String message) {
        super(message);
    }

    public ImporterRuntimeException(Throwable cause) {
        super(cause);
    }

    public ImporterRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
