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
package com.norconex.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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

	private static final Logger LOG = LogManager.getLogger(Importer.class);

    private static final ImporterStatus PASSING_FILTER_STATUS = 
            new ImporterStatus();
    
	private final ImporterConfig importerConfig;
	private final ContentTypeDetector contentTypeDetector =
	        new ContentTypeDetector();
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
        File tempDir = this.importerConfig.getTempDir();
        
        if (!tempDir.exists()) {
            try {
                FileUtils.forceMkdir(tempDir);
            } catch (IOException e) {
                throw new ImporterRuntimeException(
                        "Cannot create importer temporary directory: " 
                                + tempDir, e);
            }
        }
        streamFactory = new CachedStreamFactory(
                this.importerConfig.getMaxFilePoolCacheSize(),
                this.importerConfig.getMaxFileCacheSize(),
                this.importerConfig.getTempDir());
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
     * @throws ImporterException problem importing document
     */
    public ImporterResponse importDocument(
            InputStream input, Properties metadata, String reference)
            throws ImporterException {
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
        
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            return importDocument(is, contentType, 
                    contentEncoding, metadata, finalReference);
        } catch (FileNotFoundException e) {
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
            LOG.debug("Could not import " + reference, e);
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
                        contentTypeDetector.detect(content, reference);
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
        meta.setString(ImporterMetadata.DOC_CONTENT_TYPE, 
                safeContentType.toString()); 
        ContentFamily contentFamily = 
                ContentFamily.forContentType(safeContentType.toString());
        if (contentFamily != null) {
            meta.setString(ImporterMetadata.DOC_CONTENT_FAMILY, 
                    contentFamily.toString());
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
                        childDoc.getContent(),
                        childDoc.getContentType(), 
                        childDoc.getContentEncoding(),
                        childDoc.getMetadata(), 
                        childDoc.getReference());
                if (nestedResponse != null) {
                    response.addNestedResponse(nestedResponse);
                }
            }
            
            //--- Response Processor ---
            if (response.getParentResponse() == null && ArrayUtils.isNotEmpty(
                    importerConfig.getResponseProcessors())) {
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
        IImporterResponseProcessor[] procs = 
                importerConfig.getResponseProcessors();
        for (IImporterResponseProcessor proc : procs) {
            proc.processImporterResponse(response);
        }
    }
    
    private ImporterStatus executeHandlers(
            ImporterDocument doc, List<ImporterDocument> childDocsHolder,
            IImporterHandler[] handlers, boolean parsed)
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
                if (isMatchIncludeFilter((IOnMatchFilter) h)) {
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
            if (hasIncludes && !atLeastOneIncludeMatch) {
                return false;
            }
            return true;
        }
    }
    
    
    private boolean isMatchIncludeFilter(IOnMatchFilter filter) {
        return OnMatch.INCLUDE == filter.getOnMatch();
    }
    
    private void parseDocument(
            ImporterDocument doc, List<ImporterDocument> embeddedDocs)
            throws IOException, ImporterException {
        
        IDocumentParserFactory factory = importerConfig.getParserFactory();
        IDocumentParser parser = 
                factory.getParser(doc.getReference(), doc.getContentType());

        // No parser means no parsing, so we simply return
        if (parser == null) {
            return;
        }
        
        CachedOutputStream out = createOutputStream();
        OutputStreamWriter output = new OutputStreamWriter(
                out, CharEncoding.UTF_8);

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parser \"" + parser.getClass().getCanonicalName()
                        + "\" about to parse \"" + doc.getReference() + "\".");
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
            IOUtils.closeQuietly(out);
            if (importerConfig.getParseErrorsSaveDir() != null) {
                saveParseError(doc, e);
            }
            throw e;
        }
        
        if (out.isCacheEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parser \"" + parser.getClass()
                        + "\" did not produce new content for: " 
                        + doc.getReference());
            }
            IOUtils.closeQuietly(out);
            doc.getContent().dispose();
            doc.setContent(streamFactory.newInputStream());
        } else {
            doc.getContent().dispose();
            CachedInputStream newInputStream = null;
            try {
                newInputStream = out.getInputStream();
            } finally {
                IOUtils.closeQuietly(out);
            }
            doc.setContent(newInputStream);
        }
    }

    private void saveParseError(ImporterDocument doc, Exception e) {
        File saveDir = importerConfig.getParseErrorsSaveDir();
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        
        String uuid = UUID.randomUUID().toString();

        // Save exception
        try (PrintWriter exWriter = new PrintWriter(
                new File(saveDir, uuid + "-error.txt"))) {
            e.printStackTrace(exWriter);
            IOUtils.closeQuietly(exWriter);
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
        try (PrintWriter metaWriter = new PrintWriter(
                new File(saveDir, uuid + "-meta.txt"))) {
            doc.getMetadata().store(metaWriter, null);
            IOUtils.closeQuietly(metaWriter);
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
            File contentFile = new File(saveDir, uuid + "-content." + ext);
            FileUtils.copyInputStreamToFile(doc.getContent(), contentFile);
        } catch (IOException e1) {
            LOG.error("Cannot save parse error file content.", e1);
        }
    }
    
    private void tagDocument(ImporterDocument doc, IDocumentTagger tagger,
            boolean parsed) throws ImporterHandlerException {
        tagger.tagDocument(doc.getReference(), 
                doc.getContent(), doc.getMetadata(), parsed);
    }
    
    private boolean acceptDocument(
            ImporterDocument doc, IDocumentFilter filter, boolean parsed)
            throws ImporterHandlerException {
        
        boolean accepted = filter.acceptDocument(
                doc.getReference(),
                doc.getContent(), doc.getMetadata(), parsed);
        //TODO Is the next .rewind() necessary given next call to getContent()
        //will do it?
        doc.getContent().rewind(); 
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
        
        CachedInputStream  in = doc.getContent();
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
            IOUtils.closeQuietly(out);
            newInputStream = in;
        } else {
            in.dispose();
            try {
                newInputStream = out.getInputStream();
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
        doc.setContent(newInputStream);
    }
    
    private List<ImporterDocument> splitDocument(
            ImporterDocument doc, IDocumentSplitter h, boolean parsed)
                    throws ImporterHandlerException, IOException {

        CachedInputStream  in = doc.getContent();
        CachedOutputStream out = createOutputStream();
        
        SplittableDocument sdoc = new SplittableDocument(
                doc.getReference(), in, doc.getMetadata());
        
        List<ImporterDocument> childDocs = h.splitDocument(
                sdoc, out, streamFactory, parsed);
        try {
            // If writing was performed, get new content
            if (!out.isCacheEmpty()) {
                doc.setContent(out.getInputStream());
                in.dispose();
            }
        } finally {
            IOUtils.closeQuietly(out);
        }
        
        if (childDocs == null) {
            return new ArrayList<ImporterDocument>();
        }
        return childDocs;
    }
    
    private CachedOutputStream createOutputStream() {
        return streamFactory.newOuputStream();
    }
}
