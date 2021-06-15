/* Copyright 2010-2021 Norconex Inc.
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

import static com.norconex.importer.ImporterEvent.IMPORTER_PARSER_BEGIN;
import static com.norconex.importer.ImporterEvent.IMPORTER_PARSER_END;
import static com.norconex.importer.ImporterEvent.IMPORTER_PARSER_ERROR;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.event.EventManager;
import com.norconex.commons.lang.file.ContentFamily;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.ImporterEvent.Builder;
import com.norconex.importer.doc.ContentTypeDetector;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.doc.DocInfo;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.HandlerContext;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;
import com.norconex.importer.parser.IDocumentParserFactory;
import com.norconex.importer.parser.ParseState;
import com.norconex.importer.response.IImporterResponseProcessor;
import com.norconex.importer.response.ImporterResponse;
import com.norconex.importer.response.ImporterStatus;
import com.norconex.importer.response.ImporterStatus.Status;
import com.norconex.importer.util.CharsetUtil;

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
    private final EventManager eventManager;
    private static final InheritableThreadLocal<Importer> INSTANCE =
            new InheritableThreadLocal<>();

    /**
     * Creates a new importer with default configuration.
     */
    public Importer() {
        this(null);
    }
    /**
     * Creates a new importer with the given configuration.
     * @param importerConfig Importer configuration
     */
    public Importer(ImporterConfig importerConfig) {
        this(importerConfig, null);
    }
    /**
     * Creates a new importer with the given configuration.
     * @param importerConfig Importer configuration
     * @param eventManager event manager
     */
    public Importer(ImporterConfig importerConfig, EventManager eventManager) {
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
        this.eventManager = new EventManager(eventManager);

        INSTANCE.set(this);
    }

    public static Importer get() {
        return INSTANCE.get();
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
     * Gets the importer configuration.
     * @return importer configuration
     * @since 3.0.0
     */
    public ImporterConfig getImporterConfig() {
        return importerConfig;
    }

    /**
     * Gets the event manager.
     * @return event manager
     * @since 3.0.0
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Imports a document according to the importer configuration.
     * @param req request instructions for importing
     * @return importer response
     * @since 3.0.0
     */
    public ImporterResponse importDocument(ImporterRequest req) {
        try {
            return importDocument(toDocument(req));
        } catch (ImporterException e) {
            LOG.warn("Importer request failed: {}", req, e);
            return new ImporterResponse(req.getReference(),
                    new ImporterStatus(new ImporterException(
                            "Importer request failed: " + req, e)));
        }
    }
    /**
     * Imports a document according to the importer configuration.
     * @param document the document to import
     * @return importer response
     * @since 3.0.0
     */
    public ImporterResponse importDocument(Doc document) {
        // Note: Doc reference, InputStream and metadata are all null-safe.

        prepareDocumentForImporting(document);

        //--- Document Handling ---
        try {
            List<Doc> nestedDocs = new ArrayList<>();
            ImporterStatus filterStatus = doImportDocument(document, nestedDocs);
            ImporterResponse response = null;
            if (filterStatus.isRejected()) {
                response = new ImporterResponse(
                        document.getReference(), filterStatus);
            } else {
                response = new ImporterResponse(document);
            }
            for (Doc childDoc : nestedDocs) {
                ImporterResponse nestedResponse = importDocument(childDoc);
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
        } catch (IOException | ImporterException e) {
            LOG.warn("Could not import document: {}", document, e);
            return new ImporterResponse(document.getReference(),
                    new ImporterStatus(new ImporterException(
                            "Could not import document: " + document, e)));
        }
    }

    private void prepareDocumentForImporting(Doc document) {
        DocInfo docInfo = document.getDocInfo();

        //--- Ensure non-null content Type on Doc ---
        ContentType ct = docInfo.getContentType();
        if (ct == null || StringUtils.isBlank(ct.toString())) {
            try {
                ct = ContentTypeDetector.detect(
                        document.getInputStream(), document.getReference());
            } catch (IOException e) {
                LOG.warn("Could not detect content type. Defaulting to "
                        + "\"application/octet-stream\".", e);
                ct = ContentType.valueOf("application/octet-stream");
            }
            docInfo.setContentType(ct);
        }

        //--- Try to detect content encoding if not already set ---
        String encoding = docInfo.getContentEncoding();
        try {
            encoding = CharsetUtil.detectCharsetIfBlank(
                    encoding, document.getInputStream());
            docInfo.setContentEncoding(encoding);
        } catch (IOException e) {
            LOG.debug("Problem detecting encoding for: {}",
                    docInfo.getReference(), e);
        }

        //--- Add basic metadata for what we know so far ---
        Properties meta = document.getMetadata();
        meta.set(DocMetadata.REFERENCE, document.getReference());
        meta.set(DocMetadata.CONTENT_TYPE, ct.toString());
        ContentFamily contentFamily = ContentFamily.forContentType(ct);
        if (contentFamily != null) {
            meta.set(DocMetadata.CONTENT_FAMILY, contentFamily.toString());
        }
        if (StringUtils.isNotBlank(encoding)) {
            meta.set(DocMetadata.CONTENT_ENCODING, encoding);
        }
    }

    // We deal with stream, but since only one of stream or file can be set,
    // convert file to stream only if set.
    private Doc toDocument(ImporterRequest req) throws ImporterException {
        CachedInputStream is;
        String ref = StringUtils.trimToEmpty(req.getReference());
        if (req.getInputStream() != null) {
            // From input stream
            is = CachedInputStream.cache(req.getInputStream(), streamFactory);
        } else if (req.getFile() != null) {
            // From file
            if (!req.getFile().toFile().isFile()) {
                throw new ImporterException(
                        "File does not exists or is not a file: "
                                + req.getFile().toAbsolutePath());
            }
            try {
                is = streamFactory.newInputStream(
                        new FileInputStream(req.getFile().toFile()));
            } catch (IOException e) {
                throw new ImporterException("Could not import file: "
                        + req.getFile().toAbsolutePath(), e);
            }
            if (StringUtils.isBlank(ref)) {
                ref = req.getFile().toFile().getAbsolutePath();
            }
        } else {
            is = streamFactory.newInputStream();
        }

        DocInfo info = new DocInfo(ref);
        info.setContentEncoding(req.getContentEncoding());
        info.setContentType(req.getContentType());

        return new Doc(info, is, req.getMetadata());
    }

    private ImporterStatus doImportDocument(
            Doc document, List<Doc> nestedDocs)
                    throws ImporterException, IOException {
        ImporterStatus filterStatus = null;

        //--- Pre-handlers ---
        filterStatus = executeHandlers(
                document,
                nestedDocs,
                importerConfig.getPreParseConsumer(),
                ParseState.PRE);
        if (!filterStatus.isSuccess()) {
            return filterStatus;
        }
        //--- Parse ---
        //TODO make parse just another handler in the chain?  Eliminating
        //the need for pre and post handlers?
        parseDocument(document, nestedDocs);
        //--- Post-handlers ---
        filterStatus = executeHandlers(
                document,
                nestedDocs,
                importerConfig.getPostParseConsumer(),
                ParseState.POST);
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
            Doc doc,
            List<Doc> childDocsHolder,
            Consumer<HandlerContext> consumer,
            ParseState parseState) throws ImporterException {

        if (consumer == null) {
            return PASSING_FILTER_STATUS;
        }
        HandlerContext ctx = new HandlerContext(doc, eventManager, parseState);
        try {
            consumer.accept(ctx);
        } catch (UndeclaredThrowableException e) {
            throw (ImporterHandlerException) e.getCause();
        }
        childDocsHolder.addAll(ctx.getChildDocs());

        if (ctx.isRejected()) {
            return new ImporterStatus(ctx.getRejectedBy());
        }
        if (!ctx.getIncludeResolver().passes()) {
            return new ImporterStatus(Status.REJECTED,
                    "None of the filters with onMatch being INCLUDE got "
                  + "matched.");
        }
        return PASSING_FILTER_STATUS;
    }

    private void parseDocument(
            final Doc doc,
            final List<Doc> embeddedDocs)
                    throws IOException, ImporterException {

        IDocumentParserFactory factory = importerConfig.getParserFactory();
        IDocumentParser parser = factory.getParser(
                doc.getReference(), doc.getDocInfo().getContentType());

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

        fire(IMPORTER_PARSER_BEGIN, doc,
                b -> b.subject(parser).parseState(ParseState.PRE));

        try (   CachedOutputStream out = streamFactory.newOuputStream();
                OutputStreamWriter output = new OutputStreamWriter(
                        out, StandardCharsets.UTF_8)) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Parser \"{}\" about to parse \"{}\".",
                        parser.getClass().getCanonicalName(),
                        doc.getReference());
            }
            List<Doc> nestedDocs = parser.parseDocument(doc, output);
            output.flush();
            if (doc.getDocInfo().getContentType() == null) {
                doc.getDocInfo().setContentType(ContentType.valueOf(
                        StringUtils.trimToNull(doc.getMetadata().getString(
                                DocMetadata.CONTENT_TYPE))));
            }
            if (StringUtils.isBlank(doc.getDocInfo().getContentEncoding())) {
                doc.getDocInfo().setContentEncoding(doc.getMetadata().getString(
                        DocMetadata.CONTENT_ENCODING));
            }
            if (nestedDocs != null) {
                for (int i = 0; i < nestedDocs.size() ; i++) {
                    Properties meta = nestedDocs.get(i).getMetadata();
                    meta.add(DocMetadata.EMBEDDED_INDEX, i);
                    meta.add(DocMetadata.EMBEDDED_PARENT_REFERENCES,
                            doc.getReference());
                }
                embeddedDocs.addAll(nestedDocs);
            }
            fire(IMPORTER_PARSER_END, doc,
                    b -> b.subject(parser).parseState(ParseState.POST));

            if (out.isCacheEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Parser \"{}\" did not produce new "
                            + "content for: {}",
                            parser.getClass(), doc.getReference());
                }
                doc.setInputStream(streamFactory.newInputStream());
            } else {
                CachedInputStream newInputStream = out.getInputStream();
                doc.setInputStream(newInputStream);
            }
        } catch (DocumentParserException e) {
            fire(IMPORTER_PARSER_ERROR, doc, b -> b
                    .subject(parser).parseState(ParseState.PRE).exception(e));
            if (importerConfig.getParseErrorsSaveDir() != null) {
                saveParseError(doc, e);
            }
            throw e;
        }
    }

    private void saveParseError(Doc doc, Exception e) {
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
            String ext = FilenameUtils.getExtension(doc.getReference());
            if (StringUtils.isBlank(ext)) {
                ContentType ct = doc.getDocInfo().getContentType();
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

    private void fire(
            String eventName, Doc doc, Consumer<ImporterEvent.Builder> c) {
        Builder b = new ImporterEvent.Builder(eventName, doc);
        if (c != null) {
            c.accept(b);
        }
        eventManager.fire(b.build());
    }

    //--- Deprecated -----------------------------------------------------------

    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param metadata the document starting metadata
     * @param reference document reference (e.g. URL, file path, etc)
     * @return importer output
     * @deprecated Since 3.0.0 use {@link #importDocument(ImporterRequest)}
     */
    @Deprecated
    public ImporterResponse importDocument(
            InputStream input, Properties metadata, String reference) {
        return importDocument(input, null, null, metadata, reference);
    }

    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param metadata the document starting metadata
     * @return importer output
     * @deprecated Since 3.0.0 use {@link #importDocument(ImporterRequest)}
     */
    @Deprecated
    public ImporterResponse importDocument(File input, Properties metadata) {
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
     * @deprecated Since 3.0.0 use {@link #importDocument(ImporterRequest)}
     */
    @Deprecated
    public ImporterResponse importDocument(
            final File file, ContentType contentType, String contentEncoding,
            Properties metadata, String reference) {
        Path path = file != null ? file.toPath() : null;
        return importDocument(new ImporterRequest(path)
            .setContentType(contentType)
            .setContentEncoding(contentEncoding)
            .setMetadata(metadata)
            .setReference(reference));
    }

    /**
     * Imports a document according to the importer configuration.
     * @param input an input stream
     * @param contentType document content-type
     * @param contentEncoding document content encoding
     * @param metadata the document starting metadata
     * @param reference document reference (e.g. URL, file path, etc)
     * @return importer output
     * @deprecated Since 3.0.0 use {@link #importDocument(ImporterRequest)}
     */
    @Deprecated
    public ImporterResponse importDocument(final InputStream input,
            ContentType contentType, String contentEncoding,
            Properties metadata, String reference) {
        return importDocument(new ImporterRequest(input)
                .setContentType(contentType)
                .setContentEncoding(contentEncoding)
                .setMetadata(metadata)
                .setReference(reference));
    }
}
