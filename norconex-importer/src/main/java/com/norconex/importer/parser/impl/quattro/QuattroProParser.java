/* Copyright 2015-2016 Norconex Inc.
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
package com.norconex.importer.parser.impl.quattro;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;

/**
 * <p>Parser for Corel QuattroPro documents.</p>
 * @author Pascal Essiembre 
 * @since 2.1.0
 */
public class QuattroProParser implements IDocumentParser {

    @Override
    public List<ImporterDocument> parseDocument(
            ImporterDocument doc, Writer output) 
                    throws DocumentParserException {
        
        QPWTextExtractor ex = new QPWTextExtractor();
        try {
            ex.extract(doc.getContent(), output, doc.getMetadata());
        } catch (IOException e) {
            throw new DocumentParserException(
                    "Coult not parse QuattroPro document: "
                            + doc.getReference(), e);
        }
        return null;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof QuattroProParser)) {
            return false;
        }
        return new EqualsBuilder()
                .append(getClass(), other.getClass())
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getClass().getCanonicalName().hashCode())
                .toHashCode();
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .toString();
    }    
}
