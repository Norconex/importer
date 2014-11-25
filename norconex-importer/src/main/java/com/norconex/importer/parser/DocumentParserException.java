/* Copyright 2010-2014 Norconex Inc.
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

import com.norconex.importer.ImporterException;

/**
 * Exception thrown upon encountering a non-recoverable issue parsing a
 * document.
 * @author Pascal Essiembre
 */
public class DocumentParserException extends ImporterException {

    private static final long serialVersionUID = -8668185121797858885L;

    public DocumentParserException() {
    }

    public DocumentParserException(String message) {
        super(message);
    }

    public DocumentParserException(Throwable cause) {
        super(cause);
    }

    public DocumentParserException(String message, Throwable cause) {
        super(message, cause);
    }

}
