package com.norconex.importer;

import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.xml.XMLValidationException;
import org.apache.commons.cli.CommandLine;

import java.nio.file.Path;

public class ValidateImporter {
    static ImporterConfig loadCommandLineConfig(
            CommandLine cmd, Path configFile, Path varFile) {
        if (configFile == null) {
            return null;
        }

        ImporterConfig config = new ImporterConfig();
        try {
            new ConfigurationLoader()
                    .setVariablesFile(varFile)
                    .loadFromXML(configFile, config);
        } catch (Exception e) {
            System.err.println("A problem occured loading configuration.");
            e.printStackTrace(System.err);
            System.exit(-1);
        }
        return config;
    }

    static void checkConfig(Path configFile, Path varFile) {
        try {
            new ConfigurationLoader()
                    .setVariablesFile(varFile)
                    .loadFromXML(configFile, ImporterConfig.class);
            System.out.println("No XML configuration errors.");
        } catch (XMLValidationException e) {
            System.err.println("There were " + e.getErrors().size()
                    + " XML configuration error(s).");
            System.exit(-1);
        }
    }
}