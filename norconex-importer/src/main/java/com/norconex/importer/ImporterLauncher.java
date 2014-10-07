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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.response.ImporterResponse;

/**
 * Command line launcher of the Importer application.  Invoked by the 
 * {@link Importer#main(String[])} method.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class ImporterLauncher {

    
    private static final String ARG_INPUTFILE = "inputFile";
    private static final String ARG_OUTPUTFILE = "outputFile";
    private static final String ARG_CONTENTTYPE = "contentType";
    private static final String ARG_CONTENTENCODING = "contentEncoding";
    private static final String ARG_REFERENCE = "reference";
    private static final String ARG_CONFIG = "config";
    public static final String ARG_VARIABLES = "variables";
    
    
    /**
     * Constructor.
     */
    private ImporterLauncher() {
        super();
    }

    public static void launch(String[] args) {
        CommandLine cmd = parseCommandLineArguments(args);
        File inputFile = new File(cmd.getOptionValue(ARG_INPUTFILE));
        File varFile = null;
        if (cmd.hasOption(ARG_VARIABLES)) {
            varFile = new File(cmd.getOptionValue(ARG_VARIABLES));
        }
        ContentType contentType = 
                ContentType.valueOf(cmd.getOptionValue(ARG_CONTENTTYPE));
        String contentEncoding = cmd.getOptionValue(ARG_CONTENTENCODING);
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
                        new File(cmd.getOptionValue(ARG_CONFIG)), varFile);
            }
            ImporterResponse response = new Importer(config).importDocument(
                    inputFile, contentType, contentEncoding, 
                    metadata, reference);
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
            CachedInputStream docInStream = doc.getContent();
            
            FileOutputStream metaOut = null;
            try {
                IOUtils.copy(docInStream, docOutStream);
                IOUtils.closeQuietly(docOutStream);
                IOUtils.closeQuietly(docInStream);

                // Write metadata file
                metaOut = new FileOutputStream(metafile);
                doc.getMetadata().store(metaOut, null);
                System.out.println("IMPORTED: " + response.getReference());
            } catch (IOException e) {
                System.err.println(
                        "Could not write: " + doc.getReference());
                e.printStackTrace(System.err);
                System.err.println();
                System.err.flush();
            } finally {
                IOUtils.closeQuietly(metaOut);
            }
        }

        ImporterResponse[] nextedResponses = response.getNestedResponses();
        for (int i = 0; i < nextedResponses.length; i++) {
            ImporterResponse nextedResponse = nextedResponses[i];
            writeResponse(nextedResponse, outputPath, depth + 1, i + 1);
        }
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
        options.addOption("v", ARG_VARIABLES, true, 
                "Optional: variable file.");
   
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
}
