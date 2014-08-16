/* Copyright 2010-2014 Norconex Inc.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.file.ContentFamily;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.ImporterStatus.Status;
import com.norconex.importer.doc.Content;
import com.norconex.importer.doc.ContentTypeDetector;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.IDocumentFilter;
import com.norconex.importer.handler.filter.IOnMatchFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.splitter.IDocumentSplitter;
import com.norconex.importer.handler.tagger.IDocumentTagger;
import com.norconex.importer.handler.transformer.IDocumentTransformer;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;
import com.norconex.importer.parser.IDocumentParserFactory;

/**
 * Principal class responsible for importing documents.
 * @author Pascal Essiembre
 */
public class Importer {

	private static final Logger LOG = LogManager.getLogger(Importer.class);

	private static final String ARG_INPUTFILE = "inputFile";
    private static final String ARG_OUTPUTFILE = "outputFile";
    private static final String ARG_CONTENTTYPE = "contentType";
    private static final String ARG_REFERENCE = "reference";
    private static final String ARG_CONFIG = "config";

    private static final ImporterStatus PASSING_FILTER_STATUS = 
            new ImporterStatus();
    
	private final ImporterConfig importerConfig;
	private final ContentTypeDetector contentTypeDetector =
	        new ContentTypeDetector();
    
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
        if (!this.importerConfig.getTempDir().exists()) {
            this.importerConfig.getTempDir().mkdirs();
        }
    }

    /**
     * Invokes the importer from the command line.  
     * @param args Invoke it once without any arguments to get a 
     *    list of command-line options.
     */
    public static void main(String[] args) {
        
        CommandLine cmd = parseCommandLineArguments(args);
        File inputFile = new File(cmd.getOptionValue(ARG_INPUTFILE));
        ContentType contentType = 
                ContentType.valueOf(cmd.getOptionValue(ARG_CONTENTTYPE));
        String output = cmd.getOptionValue(ARG_OUTPUTFILE);
        if (StringUtils.isBlank(output)) {
            output = cmd.getOptionValue(ARG_INPUTFILE) + "-imported.txt";
        }
        String reference = cmd.getOptionValue(ARG_REFERENCE);
        Properties metadata = new Properties();
        try {
            ImporterConfig config = null;
            if (cmd.hasOption(ARG_CONFIG)) {
                config = ImporterConfigLoader.loadImporterConfig(
                        new File(cmd.getOptionValue(ARG_CONFIG)), null);
            }
            ImporterResponse response = new Importer(config).importDocument(
                    inputFile, contentType, metadata, reference);
            writeResponse(response, output, 0, 0);
        } catch (Exception e) {
            System.err.println(
                    "A problem occured while importing " + inputFile);
            e.printStackTrace(System.err);
        }
    }
    
    private static void writeResponse(
            ImporterResponse response, String outputPath, int depth, int index) 
                    throws IOException {
        if (!response.isSuccess()) {
            String statusLabel = "REJECTED: ";
            if (response.getImporterStatus().isError()) {
                statusLabel = "   ERROR: ";
            }
            System.out.println(statusLabel + response.getReference() + " (" 
                    + response.getImporterStatus().getDescription() + ")");
        } else {
            ImporterDocument doc = response.getDocument();
            StringBuilder path = new StringBuilder(outputPath);
            if (depth > 0) {
                int pathLength = outputPath.length();
                int extLength = FilenameUtils.getExtension(outputPath).length();
                if (extLength > 0) {
                    extLength++;
                }
                String nameSuffix = "_" + depth + "-" + index;
                path.insert(pathLength - extLength, nameSuffix);
            }
            File docfile = new File(path.toString());
            File metafile = new File(path.toString() + ".meta");

            // Write document file
            FileOutputStream docOutStream = new FileOutputStream(docfile);
            CachedInputStream docInStream = doc.getContent().getInputStream();
            
            try {
                IOUtils.copy(docInStream, docOutStream);
                IOUtils.closeQuietly(docOutStream);
                IOUtils.closeQuietly(docInStream);

                // Write metadata file
                FileOutputStream metaOut = new FileOutputStream(metafile);
                doc.getMetadata().store(metaOut, null);
                IOUtils.closeQuietly(metaOut);
                System.out.println("IMPORTED: " + response.getReference());
            } catch (IOException e) {
                System.err.println(
                        "Could not write: " + doc.getReference());
                e.printStackTrace(System.err);
                System.err.println();
                System.err.flush();
            }
        }

        ImporterResponse[] nextedResponses = response.getNestedResponses();
        for (int i = 0; i < nextedResponses.length; i++) {
            ImporterResponse nextedResponse = nextedResponses[i];
            writeResponse(nextedResponse, outputPath, depth + 1, i + 1);
        }
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
        return importDocument(input, null, metadata, reference);
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
        return importDocument(input, null, metadata, null);
    }
    
    /**
     * Imports a document according to the importer configuration.
     * @param file document input
     * @param contentType document content-type
     * @param metadata the document starting metadata
     * @param reference document reference (e.g. URL, file path, etc)
     * @return importer output
     * @throws ImporterException problem importing document
     */    
    //TODO return ImporterOutput instead, which will hold: isRejected, rejectedCause, ImportDocument
    public ImporterResponse importDocument(
            final File file, ContentType contentType, 
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
            return importDocument(is, contentType, metadata, finalReference);
        } catch (FileNotFoundException e) {
            throw new ImporterException("Could not import file.", e);
        }
    }

    public ImporterResponse importDocument(
            final InputStream input, ContentType contentType, 
            Properties metadata, String reference) {        
        try {
            return doImportDocument(input, contentType, metadata, reference);
        } catch (ImporterException e) {
            LOG.debug("Could not import " + reference, e);
            return new ImporterResponse(reference, new ImporterStatus(e));
        }
    }

    private ImporterResponse doImportDocument(
            final InputStream input, ContentType contentType, 
            Properties metadata, String reference) throws ImporterException {           
        
        //--- Input ---
        BufferedInputStream bufInput = null;
        if (input instanceof BufferedInputStream) {
            bufInput = (BufferedInputStream) input;
        } else {
            bufInput = new BufferedInputStream(input);
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
                        contentTypeDetector.detect(bufInput, reference);
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
        meta.setContentType(safeContentType); 
        ContentFamily contentFamily = 
                ContentFamily.forContentType(safeContentType.toString());
        if (contentFamily != null) {
            meta.setContentFamily(contentFamily);
        }
        
        //--- Document Handling ---
        List<ImporterDocument> nestedDocs = new ArrayList<>();
        ImporterDocument document;
        try {
            Content content = new Content(bufInput);
            document = new ImporterDocument(reference, content, meta);
            document.setContentType(safeContentType);
            
            ImporterStatus filterStatus = importDocument(document, nestedDocs);
            
            ImporterResponse response = null;
            if (filterStatus.isRejected()) {
                response = new ImporterResponse(reference, filterStatus);
            } else {
                response = new ImporterResponse(document);
            }
            for (ImporterDocument childDoc : nestedDocs) {
                ImporterResponse nestedResponse = importDocument(
                                childDoc.getContent().getInputStream(),
                        childDoc.getContentType(), 
                        childDoc.getMetadata(), 
                        childDoc.getReference());
                if (nestedResponse != null) {
                    response.addNestedResponse(nestedResponse);
                }
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
    
    private class IncludeMatchResolver {
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
        return filter instanceof IOnMatchFilter
                && OnMatch.INCLUDE == filter.getOnMatch();
    }
    
    private static CommandLine parseCommandLineArguments(String[] args) {
        Options options = new Options();
        options.addOption("i", "inputFile", true, 
                "Required: File to be imported.");
        options.addOption("o", "outputFile", true, 
                "Optional: File where the imported content will be stored.");
        options.addOption("t", "contentType", true, 
                "Optional: The MIME Content-type of the input file.");
        options.addOption("r", "reference", true, 
                "Optional: Alternate unique qualifier for the input file "
              + "(e.g. URL).");
        options.addOption("c", "config", true, 
                "Optional: Importer XML configuration file.");
   
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse( options, args);
            if(!cmd.hasOption("inputFile")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "importer[.bat|.sh]", options );
                System.exit(-1);
            }
        } catch (ParseException e) {
            System.err.println("A problem occured while parsing arguments.");
            e.printStackTrace(System.err);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "importer[.bat|.sh]", options );
            System.exit(-1);
        }
        return cmd;
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
        
        CachedInputStream  in = doc.getContent().getInputStream();
        InputStream bufInput = new BufferedInputStream(in);

        CachedOutputStream out = createOutputStream();
        OutputStreamWriter output = new OutputStreamWriter(
                out, CharEncoding.UTF_8);

        try {
            List<ImporterDocument> nestedDocs = 
                    parser.parseDocument(doc.getReference(), bufInput, 
                            doc.getContentType(), output, doc.getMetadata());
            if (nestedDocs != null) {
                embeddedDocs.addAll(nestedDocs);
            }
        } catch (DocumentParserException e) {
            IOUtils.closeQuietly(bufInput);
            IOUtils.closeQuietly(out);
            throw e;
        }
        
        if (out.isCacheEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parser \"" + parser.getClass()
                        + "\" did not produce new content for: " 
                        + doc.getReference());
            }
            IOUtils.closeQuietly(out);
            in.dispose();
            doc.setContent(new Content());
        } else {
            in.dispose();
            CachedInputStream newInputStream = null;
            try {
                newInputStream = out.getInputStream();
            } finally {
                IOUtils.closeQuietly(out);
            }
            doc.setContent(new Content(newInputStream));
        }
    }

    private void tagDocument(ImporterDocument doc, IDocumentTagger tagger,
            boolean parsed) throws ImporterHandlerException {
        tagger.tagDocument(doc.getReference(), 
                doc.getContent().getInputStream(), doc.getMetadata(), parsed);
        doc.getContent().getInputStream().rewind();
    }
    
    private boolean acceptDocument(
            ImporterDocument doc, IDocumentFilter filter, boolean parsed)
            throws ImporterHandlerException {
        
        boolean accepted = filter.acceptDocument(
                doc.getContent().getInputStream(), doc.getMetadata(), parsed);
        doc.getContent().getInputStream().rewind();
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
        
        CachedInputStream  in = doc.getContent().getInputStream();
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
        newInputStream.rewind();
        doc.setContent(new Content(newInputStream));
    }
    
    private List<ImporterDocument> splitDocument(
            ImporterDocument doc, IDocumentSplitter h, boolean parsed)
                    throws ImporterHandlerException, IOException {

        CachedInputStream  in = doc.getContent().getInputStream();
        CachedOutputStream out = createOutputStream();
        
        List<ImporterDocument> childDocs = h.splitDocument(
                doc.getReference(), in, out, doc.getMetadata(), parsed);
        try {
            //For splitters, not writing to output stream means to blank the
            //parent content, so we always obtain input from CachedOutputStream.
            doc.setContent(new Content(out.getInputStream()));
        } finally {
            in.dispose();
            IOUtils.closeQuietly(out);
        }
        
        if (childDocs == null) {
            return new ArrayList<ImporterDocument>();
        }
        return childDocs;
    }
    
    private CachedOutputStream createOutputStream() {
        return new CachedOutputStream(
                importerConfig.getFileMemCacheSize(), 
                importerConfig.getTempDir());
    }
}
