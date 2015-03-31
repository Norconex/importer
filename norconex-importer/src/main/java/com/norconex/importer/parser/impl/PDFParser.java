/* Copyright 2010-2015 Norconex Inc.
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
package com.norconex.importer.parser.impl;



/**
 * PDF parser based on Apache Tika
 * {@link org.apache.tika.parser.pdf.EnhancedPDFParser}.
 * @author Pascal Essiembre
 * @deprecated since 2.1.0.  Now handled by FallbackParser by default.
 */
@Deprecated
public class PDFParser extends AbstractTikaParser {

    public PDFParser() {
        super(new org.apache.tika.parser.pdf.EnhancedPDFParser());
    }
    
}
