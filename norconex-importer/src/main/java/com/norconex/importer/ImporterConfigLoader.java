/* Copyright 2010-2018 Norconex Inc.
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
import java.io.Reader;
import java.util.List;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.XMLValidationError;
import com.norconex.commons.lang.xml.XMLValidationException;

/**
 * Importer configuration loader.  Configuration options are defined
 * as part of general product documentation.
 * @author Pascal Essiembre
 */
public final class ImporterConfigLoader {

    private ImporterConfigLoader() {
        super();
    }

    /**
     * Loads importer configuration.
     * Throws {@link XMLValidationException} if the configuration
     * has validation errors unless errors are ignored.
     * @param configFile configuration file
     * @param configVariables configuration variables 
     *        (.variables or .properties)
     * @param ignoreErrors 
     *     <code>true</code> to ignore configuration validation errors
     * @return importer configuration
     * @throws ConfigurationException problem loading configuration
     */
    public static ImporterConfig loadImporterConfig(
            File configFile, File configVariables, boolean ignoreErrors) {
        try {
            return loadImporterConfig(new ConfigurationLoader().loadXML(
                    configFile, configVariables), ignoreErrors);
        } catch (Exception e) {
            throw configurationException(
                    "Could not load configuration file: " + configFile, e);
        }
    }    

    /**
     * Loads importer configuration.
     * Throws {@link XMLValidationException} if the configuration
     * has validation errors unless errors are ignored.
     * @param reader reader for the configuration file
     * @param ignoreErrors 
     *     <code>true</code> to ignore configuration validation errors
     * @return importer configuration
     * @throws ConfigurationException problem loading configuration
     */    
    public static ImporterConfig loadImporterConfig(
            Reader reader, boolean ignoreErrors)  {
        return loadImporterConfig(new XML(reader), ignoreErrors);
    }
    
    /**
     * Loads importer configuration.
     * Throws {@link XMLValidationException} if the configuration
     * has validation errors unless errors are ignored. 
     * @param xml XMLConfiguration instance
     * @param ignoreErrors 
     *     <code>true</code> to ignore configuration validation errors 
     * @return importer configuration
     * @throws ConfigurationException problem loading configuration
     */
    public static ImporterConfig loadImporterConfig(
            XML xml, boolean ignoreErrors) {
        ImporterConfig config = new ImporterConfig();
        List<XMLValidationError> errors = xml.configure(config);
        if (!ignoreErrors && !errors.isEmpty()) {
            throw new XMLValidationException(errors);
        }
        return config;
    }
    
    private static ConfigurationException configurationException(
            String msg, Exception e) {
        if (e instanceof ConfigurationException) {
            return (ConfigurationException) e;
        }
        return new ConfigurationException(msg, e);
    }
}
