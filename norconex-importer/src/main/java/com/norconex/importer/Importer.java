/* Copyright 2010-2019 Norconex Inc.
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
package com.norconex.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.file.ContentFamily;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.doc.ContentTypeDetector;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.IDocumentFilter;
import com.norconex.importer.handler.filter.IOnMatchFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.splitter.IDocumentSplitter;
import com.norconex.importer.handler.splitter.SplittableDocument;
import com.norconex.importer.handler.tagger.IDocumentTagger;
import com.norconex.importer.handler.transformer.IDocumentTransformer;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;
import com.norconex.importer.parser.IDocumentParserFactory;
import com.norconex.importer.response.IImporterResponseProcessor;
import com.norconex.importer.response.ImporterResponse;
import com.norconex.importer.response.ImporterStatus;
import com.norconex.importer.response.ImporterStatus.Status;

/**
 * Principal class responsible for importing documents.
 * @author Pascal Essiembre
 */
public class Importer {

	private static final Logger LOG = LoggerFactory.getLogger(Importer.class);

    private static final ImporterStatus PASSING_FILTER_STATUS =
            new ImporterStatus();

	private final ImporterConfig importerConfig;
	private final CachedStreamFactory streamFactory;

    /**
     * Creates a new importer with default configuration.
     */
    public Importer() {
        this(new ImporterConfig());
    }
    /**
     * Creates a new importer with the given configuration.
     * @param importerConfig Importer configuration
     */
    public Importer(ImporterConfig importerConfig) {
        super();
        if (importerConfig != null) {
            this.importerConfig = importerConfig;
        } else {
            this.importerConfig = new ImporterConfig();
        }
        Path tempDir = this.importerConfig.getTempDir();

        if (!tempDir.toFile().exists()) {
            try {
                Files.createDirectories(tempDir);
            } catch (IOException e) {
                throw new ImporterRuntimeException(
                        "Cannot create importer temporary directory: "
                                + tempDir, e);
            }
        }
        streamFactory = new CachedStreamFactory(
                this.importerConfig.getMaxFilePoolCacheSize(),
                this.importerConfig.getMaxFileCacheSize(),
                this.importerConfig.getTempDir()); // use workdir + /tmp?
    }

    /**
     * Invokes the importer from the command line.
     * @param args Invoke it once without any arguments to get a
     *    list of command-line options.
     */
    public static void main(String[] args) {
        ImporterLauncher.launch(args);
    }

    public CachedStreamFactory getStreamFactory() {
        return streamFactory;
    }

    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param metadata the document starting metadata
     * @param reference document reference (e.g. URL, file path, etc)
     * @return importer output
     */
    public ImporterResponse importDocument(
            InputStream input, Properties metadata, String reference) {
        return importDocument(input, null, null, metadata, reference);
    }

    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param metadata the document starting metadata
     * @return importer output
     * @throws ImporterException problem importing document
     */
    public ImporterResponse importDocument(
            File input, Properties metadata) throws ImporterException {
        return importDocument(input, null, null, metadata, null);
    }

    /**
     * Imports a document according to the importer configuration.
     * @param file document input
     * @param contentType document content-type
     * @param contentEncoding document content encoding
     * @param metadata the document starting metadata
     * @param reference document reference (e.g. URL, file path, etc)
     * @return importer output
     * @throws ImporterException problem importing document
     */
    public ImporterResponse importDocument(
            final File file, ContentType contentType, String contentEncoding,
            Properties metadata, String reference) throws ImporterException {

        //--- Validate File ---
        if (file == null) {
            throw new ImporterException("File cannot be null.");
        } else if (!file.isFile()) {
            throw new ImporterException(
                    "File does not exists or is not a file.");
        }

        //--- Reference ---
        String finalReference = reference;
        if (StringUtils.isBlank(reference)) {
            finalReference = file.getAbsolutePath();
        }

        try (InputStream is =
                streamFactory.newInputStream(new FileInputStream(file))) {
            return importDocument(is, contentType,
                    contentEncoding, metadata, finalReference);
        } catch (IOException e) {
            throw new ImporterException("Could not import file.", e);
        }
    }

    public ImporterResponse importDocument(final InputStream input,
            ContentType contentType, String charEncoding,
            Properties metadata, String reference) {
        try {
            return doImportDocument(input, contentType,
                    charEncoding, metadata, reference);
        } catch (ImporterException e) {
            LOG.warn("Could not import " + reference, e);
            return new ImporterResponse(reference, new ImporterStatus(e));
        }
    }

    //TODO pass ImportDocument instead?   Accept one as argument?
    private ImporterResponse doImportDocument(final InputStream input,
            ContentType contentType, String contentEncoding,
            Properties metadata, String reference) throws ImporterException {


        if (input == null && LOG.isDebugEnabled()) {
            LOG.debug("Content is null for " + reference
                    + ". Won't import much. Was it intentional?");
        }

        //--- Input ---
        CachedInputStream content = null;
        if (input instanceof CachedInputStream) {
            content = (CachedInputStream) input;
        } else {
            content = streamFactory.newInputStream(input);
        }

        //--- Reference ---
        if (StringUtils.isBlank(reference)) {
            throw new ImporterException("The document reference was not set.");
        }

        //--- Content Type ---
        ContentType safeContentType = contentType;
        if (safeContentType == null
                || StringUtils.isBlank(safeContentType.toString())) {
            try {
                safeContentType =
                        ContentTypeDetector.detect(content, reference);
            } catch (IOException e) {
                LOG.error("Could not detect content type. Defaulting to "
                        + "\"application/octet-stream\".", e);
                safeContentType =
                        ContentType.valueOf("application/octet-stream");
            }
        }

        //--- Metadata ---
        ImporterMetadata meta = null;
        if (metadata instanceof ImporterMetadata) {
            meta = (ImporterMetadata) metadata;
        } else {
            meta = new ImporterMetadata(metadata);
        }
        meta.setReference(reference);
        meta.set(ImporterMetadata.DOC_CONTENT_TYPE,
                safeContentType.toString());
        ContentFamily contentFamily =
                ContentFamily.forContentType(safeContentType.toString());
        if (contentFamily != null) {
            meta.set(ImporterMetadata.DOC_CONTENT_FAMILY,
                    contentFamily.toString());
        }
        if (StringUtils.isNotBlank(contentEncoding)) {
            meta.set(ImporterMetadata.DOC_CONTENT_ENCODING, contentEncoding);
        }

        //--- Document Handling ---
        try {
            List<ImporterDocument> nestedDocs = new ArrayList<>();
            ImporterDocument document =
                    new ImporterDocument(reference, content, meta);
            document.setContentType(safeContentType);
            document.setContentEncoding(contentEncoding);

            ImporterStatus filterStatus = importDocument(document, nestedDocs);

            ImporterResponse response = null;
            if (filterStatus.isRejected()) {
                response = new ImporterResponse(reference, filterStatus);
            } else {
                response = new ImporterResponse(document);
            }
            for (ImporterDocument childDoc : nestedDocs) {
                ImporterResponse nestedResponse = importDocument(
                        childDoc.getInputStream(),
                        childDoc.getContentType(),
                        childDoc.getContentEncoding(),
                        childDoc.getMetadata(),
                        childDoc.getReference());
                if (nestedResponse != null) {
                    response.addNestedResponse(nestedResponse);
                }
            }

            //--- Response Processor ---
            if (response.getParentResponse() == null
                    && !importerConfig.getResponseProcessors().isEmpty()) {
                processResponse(response);
            }
            return response;
        } catch (IOException e) {
            throw new ImporterException(
                    "Could not import document: " + reference, e);
        }
    }

    private ImporterStatus importDocument(
            ImporterDocument document, List<ImporterDocument> nestedDocs)
                    throws IOException, ImporterException {
        ImporterStatus filterStatus = null;

        //--- Pre-handlers ---
        filterStatus = executeHandlers(document, nestedDocs,
                importerConfig.getPreParseHandlers(), false);
        if (!filterStatus.isSuccess()) {
            return filterStatus;
        }

        //--- Parse ---
        //TODO make parse just another handler in the chain?  Eliminating
        //the need for pre and post handlers?
        parseDocument(document, nestedDocs);

        //--- Post-handlers ---
        filterStatus = executeHandlers(document, nestedDocs,
                importerConfig.getPostParseHandlers(), true);
        if (!filterStatus.isSuccess()) {
            return filterStatus;
        }

        return PASSING_FILTER_STATUS;
    }


    private void processResponse(ImporterResponse response) {
        for (IImporterResponseProcessor proc
                : importerConfig.getResponseProcessors()) {
            proc.processImporterResponse(response);
        }
    }

    private ImporterStatus executeHandlers(
            ImporterDocument doc, List<ImporterDocument> childDocsHolder,
            List<IImporterHandler> handlers, boolean parsed)
            throws IOException, ImporterException {
        if (handlers == null) {
            return PASSING_FILTER_STATUS;
        }

        IncludeMatchResolver includeResolver = new IncludeMatchResolver();
        for (IImporterHandler h : handlers) {
            if (h instanceof IDocumentTagger) {
                tagDocument(doc, (IDocumentTagger) h, parsed);
            } else if (h instanceof IDocumentTransformer) {
                transformDocument(doc, (IDocumentTransformer) h, parsed);
            } else if (h instanceof IDocumentSplitter) {
                childDocsHolder.addAll(
                        splitDocument(doc, (IDocumentSplitter) h, parsed));
            } else if (h instanceof IDocumentFilter) {
                IDocumentFilter filter = (IDocumentFilter) h;
                boolean accepted = acceptDocument(doc, filter, parsed);
                if (isMatchIncludeFilter(filter)) {
                    includeResolver.hasIncludes = true;
                    if (accepted) {
                        includeResolver.atLeastOneIncludeMatch = true;
                    }
                    continue;
                }
                // Deal with exclude and non-OnMatch filters
                if (!accepted){
                    return new ImporterStatus(filter);
                }
            } else {
                LOG.error("Unsupported Import Handler: " + h);
            }
        }

        if (!includeResolver.passes()) {
            return new ImporterStatus(Status.REJECTED,
                    "None of the filters with onMatch being INCLUDE got "
                  + "matched.");
        }
        return PASSING_FILTER_STATUS;
    }

    private static class IncludeMatchResolver {
        private boolean hasIncludes = false;
        private boolean atLeastOneIncludeMatch = false;
        public boolean passes() {
            return !(hasIncludes && !atLeastOneIncludeMatch);
        }
    }


    private boolean isMatchIncludeFilter(IDocumentFilter filter) {
        return filter instanceof IOnMatchFilter
                && OnMatch.INCLUDE == ((IOnMatchFilter) filter).getOnMatch();
    }

    private void parseDocument(
            ImporterDocument doc, List<ImporterDocument> embeddedDocs)
            throws IOException, ImporterException {

        IDocumentParserFactory factory = importerConfig.getParserFactory();
        IDocumentParser parser =
                factory.getParser(doc.getReference(), doc.getContentType());

        // Do not attempt to parse zero-length content
        if (doc.getInputStream().isEmpty()) {
            LOG.debug("No content for \"{}\".", doc.getReference());
            return;
        }

        // No parser means no parsing, so we simply return
        if (parser == null) {
            LOG.debug("No parser for \"{}\"", doc.getReference());
            return;
        }

        CachedOutputStream out = createOutputStream();
        OutputStreamWriter output = new OutputStreamWriter(
                out, StandardCharsets.UTF_8);

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parser \"{}\" about to parse \"{}\".",
                        parser.getClass().getCanonicalName(),
                        doc.getReference());
            }
            List<ImporterDocument> nestedDocs =
                    parser.parseDocument(doc, output);
            output.flush();
            if (doc.getContentType() == null) {
                String ct = doc.getMetadata().getString(
                                ImporterMetadata.DOC_CONTENT_TYPE);
                if (StringUtils.isNotBlank(ct)) {
                    doc.setContentType(ContentType.valueOf(ct));
                }
            }
            if (StringUtils.isBlank(doc.getContentEncoding())) {
                doc.setContentEncoding(doc.getMetadata().getString(
                        ImporterMetadata.DOC_CONTENT_ENCODING));
            }
            if (nestedDocs != null) {
                embeddedDocs.addAll(nestedDocs);
            }
        } catch (DocumentParserException e) {
            try { out.close(); } catch (IOException ie) { /*NOOP*/ }
            if (importerConfig.getParseErrorsSaveDir() != null) {
                saveParseError(doc, e);
            }
            throw e;
        }

        if (out.isCacheEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parser \"{}\" did not produce new content for: {}",
                        parser.getClass(), doc.getReference());
            }
            try { out.close(); } catch (IOException ie) { /*NOOP*/ }
            doc.getInputStream().dispose();
            doc.setInputStream(streamFactory.newInputStream());
        } else {
            doc.getInputStream().dispose();
            CachedInputStream newInputStream = null;
            try {
                newInputStream = out.getInputStream();
            } finally {
                try { out.close(); } catch (IOException ie) { /*NOOP*/ }
            }
            doc.setInputStream(newInputStream);
        }
    }

    private void saveParseError(ImporterDocument doc, Exception e) {
        Path saveDir = importerConfig.getParseErrorsSaveDir();
        if (!saveDir.toFile().exists()) {
            try {
                Files.createDirectories(saveDir);
            } catch (IOException ex) {
                LOG.error("Cannot create importer temporary directory: "
                        + saveDir, ex);
            }
        }

        String uuid = UUID.randomUUID().toString();

        // Save exception
        try (PrintWriter exWriter = new PrintWriter(Files.newBufferedWriter(
                saveDir.resolve(uuid + "-error.txt")))) {
            e.printStackTrace(exWriter);
        } catch (IOException e1) {
            LOG.error("Cannot save parse exception.", e1);
        }

        if (doc == null) {
            LOG.error("The importer document that cause a parse error is "
                    + "null. It is not possible to save it.  Only the "
                    + "exception will be saved.");
            return;
        }

        // Save metadata
        try (PrintWriter metaWriter = new PrintWriter(Files.newBufferedWriter(
                saveDir.resolve(uuid + "-meta.txt")))) {
            doc.getMetadata().storeToProperties(metaWriter);
        } catch (IOException e1) {
            LOG.error("Cannot save parse error file metadata.", e1);
        }

        // Save content
        try {
            String ext = FilenameUtils.getExtension(
                    doc.getMetadata().getReference());
            if (StringUtils.isBlank(ext)) {
                ContentType ct = doc.getContentType();
                if (ct != null) {
                    ext = ct.getExtension();
                }
            }
            if (StringUtils.isBlank(ext)) {
                ext = "unknown";
            }

            Files.copy(doc.getInputStream(),
                    saveDir.resolve(uuid + "-content." + ext),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e1) {
            LOG.error("Cannot save parse error file content.", e1);
        }
    }

    private void tagDocument(ImporterDocument doc, IDocumentTagger tagger,
            boolean parsed) throws ImporterHandlerException {
        tagger.tagDocument(doc.getReference(),
                doc.getInputStream(), doc.getMetadata(), parsed);
    }

    private boolean acceptDocument(
            ImporterDocument doc, IDocumentFilter filter, boolean parsed)
            throws ImporterHandlerException {

        boolean accepted = filter.acceptDocument(
                doc.getReference(),
                doc.getInputStream(), doc.getMetadata(), parsed);
        //TODO Is the next .rewind() necessary given next call to getContent()
        //will do it?
        doc.getInputStream().rewind();
        if (!accepted) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Document import rejected. Filter=" + filter);
            }
            return false;
        }
        return true;
    }

    private void transformDocument(ImporterDocument doc,
            IDocumentTransformer transformer, boolean parsed)
                    throws ImporterHandlerException, IOException {

        CachedInputStream  in = doc.getInputStream();
        CachedOutputStream out = createOutputStream();

        transformer.transformDocument(
                doc.getReference(), in, out, doc.getMetadata(), parsed);

        CachedInputStream newInputStream = null;
        if (out.isCacheEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Transformer \"" + transformer.getClass()
                        + "\" did not return any content for: "
                        + doc.getReference());
            }
            try { out.close(); } catch (IOException ie) { /*NOOP*/ }
            newInputStream = in;
        } else {
            in.dispose();
            try {
                newInputStream = out.getInputStream();
            } finally {
                try { out.close(); } catch (IOException ie) { /*NOOP*/ }
            }
        }
        doc.setInputStream(newInputStream);
    }

    private List<ImporterDocument> splitDocument(
            ImporterDocument doc, IDocumentSplitter h, boolean parsed)
                    throws ImporterHandlerException, IOException {

        CachedInputStream  in = doc.getInputStream();
        CachedOutputStream out = createOutputStream();

        SplittableDocument sdoc = new SplittableDocument(
                doc.getReference(), in, doc.getMetadata());

        List<ImporterDocument> childDocs = h.splitDocument(
                sdoc, out, streamFactory, parsed);
        try {
            // If writing was performed, get new content
            if (!out.isCacheEmpty()) {
                doc.setInputStream(out.getInputStream());
                in.dispose();
            }
        } finally {
            try { out.close(); } catch (IOException ie) { /*NOOP*/ }
        }

        if (childDocs == null) {
            return new ArrayList<>();
        }
        return childDocs;
    }

    private CachedOutputStream createOutputStream() {
        return streamFactory.newOuputStream();
    }
}
