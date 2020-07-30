/* Copyright 2018-2020 Norconex Inc.
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
package com.norconex.importer.handler.splitter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.doc.DocInfo;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.splitter.AbstractDocumentSplitter;
import com.norconex.importer.parser.ParseState;

/**
 * <p>
 * Split PDFs pages so each pages are treated as individual documents. May not
 * work on all PDFs (e.g., encrypted PDFs).
 * </p>
 *
 * <p>
 * The original PDF is kept intact. If you want to eliminate it to keep only
 * the split pages, make sure to filter it.  You can do so by filtering
 * out PDFs without one of these two fields added to each pages:
 * <code>document.pdf.pageNumber</code> or
 * <code>document.pdf.numberOfPages</code>.  A filtering example:
 * </p>
 *
 * {@nx.xml.example
 * <filter class="com.norconex.importer.handler.filter.impl.EmptyFilter"
 *         onMatch="exclude">
 *   <fieldMatcher matchWhole="true">document.pdf.pageNumber</fieldMatcher>
 * </filter>
 * }
 *
 * <p>
 * By default this splitter restricts its use to
 * <code>document.contentType</code> matching <code>application/pdf</code>.
 * </p>
 *
 * <p>Should be used as a pre-parse handler.</p>
 *
 * {@nx.xml.usage
 *  <handler class="com.norconex.importer.handler.splitter.impl.PDFPageSplitter">
 *    {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}

 *    <referencePagePrefix>
 *      (String to put before the page number is appended to the document
 *      reference. Default is "#".)
 *    </referencePagePrefix>
 *
 *  </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.splitter.impl.PDFPageSplitter">
 *   <referencePagePrefix>#page</referencePagePrefix>
 * </handler>
 * }
 * <p>The above example will split PDFs and will append the page number
 * to the original PDF reference as "#page1", "#page2", etc.
 * </p>
 * @author Pascal Essiembre
 * @since 2.9.0
 */
@SuppressWarnings("javadoc")
public class PDFPageSplitter extends AbstractDocumentSplitter
        implements IXMLConfigurable {

    public static final String DOC_PDF_PAGE_NO = "document.pdf.pageNumber";
    public static final String DOC_PDF_TOTAL_PAGES =
            "document.pdf.numberOfPages";

    public static final String DEFAULT_REFERENCE_PAGE_PREFIX = "#";

    private String referencePagePrefix = DEFAULT_REFERENCE_PAGE_PREFIX;

    public PDFPageSplitter() {
        super();
        addRestriction(new PropertyMatcher(
                TextMatcher.basic(DocMetadata.CONTENT_TYPE),
                TextMatcher.basic("application/pdf")));
    }

    public String getReferencePagePrefix() {
        return referencePagePrefix;
    }
    public void setReferencePagePrefix(String referencePagePrefix) {
        this.referencePagePrefix = referencePagePrefix;
    }

    @Override
    protected List<Doc> splitApplicableDocument(
            HandlerDoc doc, InputStream input, OutputStream output,
            ParseState parseState) throws ImporterHandlerException {

        List<Doc> pageDocs = new ArrayList<>();

        // Make sure we are not splitting a page that was already split
        if (doc.getMetadata().getInteger(DOC_PDF_PAGE_NO, 0) > 0) {
            return pageDocs;
        }

        try (PDDocument document = PDDocument.load(input)) {

            // Make sure we are not splitting single pages.
            if (document.getNumberOfPages() <= 1) {
                doc.getMetadata().set(DOC_PDF_PAGE_NO, 1);
                doc.getMetadata().set(DOC_PDF_TOTAL_PAGES, 1);
                return pageDocs;
            }

            Splitter splitter = new Splitter();
            List<PDDocument> splittedDocuments = splitter.split(document);
            int pageNo = 0;
            for (PDDocument page : splittedDocuments) {
                pageNo++;

                String pageRef =
                        doc.getReference() + referencePagePrefix + pageNo;

                // metadata
                Properties pageMeta = new Properties();
                pageMeta.loadFromMap(doc.getMetadata());

                DocInfo pageInfo = new DocInfo(pageRef);

//                pageInfo.setEmbeddedReference(Integer.toString(pageNo));
                pageMeta.set(DocMetadata.EMBEDDED_REFERENCE,
                        Integer.toString(pageNo));


                pageInfo.addEmbeddedParentReference(doc.getReference());

//                pageMeta.setReference(pageRef);
//                pageMeta.setEmbeddedReference(Integer.toString(pageNo));
//                pageMeta.setEmbeddedParentReference(doc.getReference());
//                pageMeta.setEmbeddedParentRootReference(doc.getReference());

                pageMeta.set(DOC_PDF_PAGE_NO, pageNo);
                pageMeta.set(DOC_PDF_TOTAL_PAGES, document.getNumberOfPages());

                // a single page should not be too big to store in memory
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    page.save(os);
                } finally {
                    page.close();
                }
                Doc pageDoc = new Doc(
                        pageInfo,
                        doc.getStreamFactory().newInputStream(
                                os.toInputStream()),
                        pageMeta);
                pageDocs.add(pageDoc);
            }
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Could not split PDF: " + doc.getReference(), e);
        }
        return pageDocs;
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        setReferencePagePrefix(
                xml.getString("referencePagePrefix", referencePagePrefix));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.addElement("referencePagePrefix", referencePagePrefix);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PDFPageSplitter)) {
            return false;
        }
        PDFPageSplitter castOther = (PDFPageSplitter) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(referencePagePrefix, castOther.referencePagePrefix)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(referencePagePrefix)
                .toHashCode();
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("referencePagePrefix", referencePagePrefix)
                .toString();
    }
}
