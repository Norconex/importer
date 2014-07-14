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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tika.io.TikaInputStream;

import com.norconex.commons.lang.file.ContentFamily;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.doc.Content;
import com.norconex.importer.doc.ContentTypeDetector;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.filter.IDocumentFilter;
import com.norconex.importer.filter.IOnMatchFilter;
import com.norconex.importer.filter.OnMatch;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;
import com.norconex.importer.parser.IDocumentParserFactory;
import com.norconex.importer.tagger.IDocumentTagger;
import com.norconex.importer.transformer.IDocumentTransformer;

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

	private final ImporterConfig importerConfig;
	private final ContentTypeDetector contentTypeDetector;
    
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
        contentTypeDetector = new ContentTypeDetector();
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
        File outputFile = new File(output);
        File metadataFile = new File(output + ".meta");
        String reference = cmd.getOptionValue(ARG_REFERENCE);
        Properties metadata = new Properties();
        try {
            ImporterConfig config = null;
            if (cmd.hasOption(ARG_CONFIG)) {
                config = ImporterConfigLoader.loadImporterConfig(
                        new File(cmd.getOptionValue(ARG_CONFIG)), null);
            }
            ImporterDocument doc = new Importer(config).importDocument(
                    inputFile, contentType, metadata, reference);
            
//            new Importer(config).importDocument(
//                    inputFile, contentType, outputFile, metadata, reference);

            // Write document file
            FileOutputStream docOutStream = new FileOutputStream(outputFile);
            CachedInputStream docInStream = doc.getContent().getInputStream();
            IOUtils.copy(docInStream, docOutStream);
            IOUtils.closeQuietly(docOutStream);
            IOUtils.closeQuietly(docInStream);
            
            // Write metadata file
            FileOutputStream metaOut = new FileOutputStream(metadataFile);
            metadata.store(metaOut, null);
            IOUtils.closeQuietly(metaOut);
        } catch (Exception e) {
            System.err.println("A problem occured while importing " + inputFile);
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param output document output
     * @param metadata the document starting metadata
     * @return <code>true</code> if the document has successfully been imported,
     *         <code>false</code> if the document was rejected (i.e. filtered)
     * @throws IOException problem importing document
     */
    @Deprecated
    public boolean importDocument(
            InputStream input, Writer output, Properties metadata)
            throws IOException {
        return importDocument(input, null, output, metadata, null);
    }
    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param contentType document content-type
     * @param output document output
     * @param metadata the document starting metadata
     * @return <code>true</code> if the document has successfully been imported,
     *         <code>false</code> if the document was rejected (i.e. filtered)
     * @param docReference document reference (e.g. URL, file path, etc)
     * @throws IOException problem importing document
     */
    @Deprecated
    public boolean importDocument(
            InputStream input, ContentType contentType, 
            Writer output, Properties metadata, String docReference)
            throws IOException {
        File tmpInput = File.createTempFile("NorconexImporter", "input");
        FileOutputStream out = new FileOutputStream(tmpInput);
        IOUtils.copy(input, out);
        out.close();

        File tmpOutput = File.createTempFile("NorconexImporter", "output");
        
        ContentType safeContentType = contentType;
        if (safeContentType == null 
                || StringUtils.isBlank(safeContentType.toString())) {
            safeContentType = 
                    contentTypeDetector.detect(tmpOutput, docReference);
        }
        boolean accepted = importDocument(
                tmpInput, safeContentType, tmpOutput, metadata, docReference);
        InputStream is = new FileInputStream(tmpOutput);
        IOUtils.copy(is, output);
        is.close();
        return accepted;
    }
    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param output document output
     * @param metadata the document starting metadata
     * @return <code>true</code> if the document has successfully been imported,
     *         <code>false</code> if the document was rejected (i.e. filtered)
     * @throws IOException problem importing document
     */
    @Deprecated
    public boolean importDocument(
            File input, File output, Properties metadata)
            throws IOException {
        return importDocument(input, null, output, metadata, null);
    }
    /**
     * Imports a document according to the importer configuration.
     * @param input document input
     * @param contentType document content-type
     * @param output document output
     * @param metadata the document starting metadata
     * @return <code>true</code> if the document has successfully been imported,
     *         <code>false</code> if the document was rejected (i.e. filtered)
     * @param docReference document reference (e.g. URL, file path, etc)
     * @throws IOException problem importing document
     */    
    @Deprecated
    public boolean importDocument(
            final File input, ContentType contentType, 
            File output, Properties metadata, String docReference)
            throws IOException {

        throw new UnsupportedOperationException("Method now deprecated.");
//        MutableObject<File> workFile = new MutableObject<File>(input);
//        ContentType safeContentType = contentType;
//        if (safeContentType == null 
//                || StringUtils.isBlank(safeContentType.toString())) {
//            safeContentType = contentTypeDetector.detect(input, docReference);
//        }
//        
//        String finalDocRef = docReference;
//        if (StringUtils.isBlank(docReference)) {
//            finalDocRef = input.getAbsolutePath();
//        }
//        
////        metadata.addString(DOC_REFERENCE, finalDocRef); 
////    	metadata.addString(DOC_CONTENT_TYPE, safeContentType.toString()); 
////        ContentFamily contentFamily = 
////                ContentFamily.forContentType(safeContentType.toString());
////        if (contentFamily != null) {
////            metadata.addString(DOC_CONTENT_FAMILY, contentFamily.getId());
////        }
//        
//    	if (!executeHandlers(doc,
//    	        importerConfig.getPreParseHandlers(), false)) {
//    	    return false;
//    	}
//    	
//    	parseDocument(workFile.getValue(), 
//    	        safeContentType, output, metadata, finalDocRef);
//    	workFile.setValue(output);
//
//    	if (!executeHandlers(docReference, input, workFile, metadata, 
//                importerConfig.getPostParseHandlers(), true)) {
//            return false;
//        }
//    	
//    	if (!workFile.getValue().equals(output)) {
//            FileUtil.moveFile(workFile.getValue(), output);
//    	}
//        return true;
    }
    
    
    /**
     * Imports a document according to the importer configuration.
     * @param fileInput document input
     * @param contentType document content-type
     * @param metadata the document starting metadata
     * @param docReference document reference (e.g. URL, file path, etc)
     * @return importer output
     * @throws ImporterException problem importing document
     */    
    //TODO return ImporterOutput instead, which will hold: isRejected, rejectedCause, ImportDocument
    public ImporterDocument importDocument(
            final File fileInput, ContentType contentType, 
            Properties metadata, String docReference) throws ImporterException {

        ImporterMetadata meta = new ImporterMetadata(metadata);
        
        //--- Reference ---
        String finalDocRef = docReference;
        if (StringUtils.isBlank(docReference)) {
            finalDocRef = fileInput.getAbsolutePath();
        }

        //--- Content Type ---
        ContentType safeContentType = contentType;
        if (safeContentType == null 
                || StringUtils.isBlank(safeContentType.toString())) {
            try {
                safeContentType = 
                        contentTypeDetector.detect(fileInput, finalDocRef);
            } catch (IOException e) {
                LOG.error("Could not detect content type. Defaulting to "
                        + "\"application/octet-stream\".", e);
                safeContentType = 
                        ContentType.valueOf("application/octet-stream");
            }
        }
        
        //--- Metadata ---
        meta.setDocumentReference(finalDocRef); 
        meta.setContentType(safeContentType); 
        ContentFamily contentFamily = 
                ContentFamily.forContentType(safeContentType.toString());
        if (contentFamily != null) {
            meta.setContentFamily(contentFamily);
        }
        
        ImporterDocument document;
        try {
            //--- Document ---
            document = new ImporterDocument(
                    finalDocRef, new Content(fileInput), meta);
            document.setContentType(safeContentType);
            
            //--- Pre-handlers ---
            if (!executeHandlers(
                    document, importerConfig.getPreParseHandlers(), false)) {
                return null;// false;
            }
            
            //--- Parse ---
            //TODO make parse just another handler in the chain?  Eliminating
            //the need for pre and post handlers?
            parseDocument(document);

            //--- Post-handlers ---
            if (!executeHandlers(
                    document, importerConfig.getPostParseHandlers(), true)) {
                return null; //false;
            }
        } catch (IOException e) {
            throw new ImporterException(
                    "Could not import document: " + docReference, e);
        }
        
        return document; //true;
    }
    
    private boolean executeHandlers(
            ImporterDocument doc,
            IImporterHandler[] handlers, boolean parsed)
            throws IOException, ImporterHandlerException {
        if (handlers == null) {
            return true;
        }
        
        IncludeMatchResolver includeResolver = new IncludeMatchResolver();
        for (IImporterHandler h : handlers) {
            if (h instanceof IDocumentTagger) {
                tagDocument(doc, (IDocumentTagger) h, parsed);
            } else if (h instanceof IDocumentTransformer) {
                transformDocument(doc, (IDocumentTransformer) h, parsed);
            } else if (h instanceof IDocumentFilter) {
                boolean accepted = acceptDocument(
                        doc, (IDocumentFilter) h, parsed);
                if (isMatchIncludeFilter((IOnMatchFilter) h)) {
                    includeResolver.hasIncludes = true;
                    if (accepted) {
                        includeResolver.atLeastOneIncludeMatch = true;
                    }
                    continue;
                }
                // Deal with exclude and non-OnMatch filters
                if (!accepted){
                    return false;
                }
            } else {
                LOG.error("Unsupported Import Handler: " + h);
            }
        }
        return includeResolver.passes();
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
    
    private void parseDocument(ImporterDocument doc)
            throws IOException, DocumentParserException {
        
        CachedInputStream  in = doc.getContent().getInputStream();
        CachedOutputStream out = new CachedOutputStream(
                importerConfig.getFileMemCacheSize(), 
                importerConfig.getTempDir());
        OutputStreamWriter output = new OutputStreamWriter(
                out, CharEncoding.UTF_8);
        
        InputStream input = TikaInputStream.get(in);
        IDocumentParserFactory factory = importerConfig.getParserFactory();
        IDocumentParser parser = 
                factory.getParser(doc.getReference(), doc.getContentType());
        try {
            parser.parseDocument(
                    input, doc.getContentType(), output, doc.getMetadata());
        } finally {
            IOUtils.closeQuietly(input);
            CachedInputStream newInputStream = out.getInputStream();
            IOUtils.closeQuietly(out);
            doc.setContent(new Content(newInputStream));
        }
    }

    private void tagDocument(ImporterDocument doc, IDocumentTagger tagger,
            boolean parsed) throws ImporterHandlerException {
        tagger.tagDocument(doc.getReference(), 
                doc.getContent().getInputStream(), doc.getMetadata(), parsed);
    }
    
    private boolean acceptDocument(
            ImporterDocument doc, IDocumentFilter filter, boolean parsed)
            throws ImporterHandlerException {
        
        boolean accepted = filter.acceptDocument(
                doc.getContent().getInputStream(), doc.getMetadata(), parsed);
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
        CachedOutputStream out = new CachedOutputStream(
                importerConfig.getFileMemCacheSize(), 
                importerConfig.getTempDir());
        
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
            IOUtils.closeQuietly(in);
            try {
                newInputStream = out.getInputStream();
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
        doc.setContent(new Content(newInputStream));
    }
}
