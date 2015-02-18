/* Copyright 2010-2014 Norconex Inc.
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

import org.apache.commons.configuration.XMLConfiguration;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.ConfigurationUtil;

/**
 * Importer configuration loader.  Configuration options are defined
 * as part of general product documentation.
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public final class ImporterConfigLoader {

    private ImporterConfigLoader() {
        super();
    }

    /**
     * Loads importer configuration.
     * @param configFile configuration file
     * @param configVariables configuration variables 
     *        (.variables or .properties)
     * @return importer configuration
     * @throws ConfigurationException problem loading configuration
     */
    public static ImporterConfig loadImporterConfig(
            File configFile, File configVariables) {
        try {
            ConfigurationLoader configLoader = new ConfigurationLoader();
            XMLConfiguration xml = configLoader.loadXML(
                    configFile, configVariables);
            return loadImporterConfig(xml);
        } catch (Exception e) {
            throw configurationException(
                    "Could not load configuration file: " + configFile, e);
        }
    }    

    /**
     * Loads importer configuration.
     * @param config reader for the configuration file
     * @return importer configuration
     * @throws ConfigurationException problem loading configuration
     */    
    public static ImporterConfig loadImporterConfig(Reader config)  {
        try {
            XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(config);
            return loadImporterConfig(xml);
        } catch (Exception e) {
            throw configurationException(
                    "Could not load configuration file from Reader.", e);
        }
    }
    
    /**
     * Loads importer configuration.
     * @param xml XMLConfiguration instance
     * @return importer configuration
     * @throws ConfigurationException problem loading configuration
     */
    public static ImporterConfig loadImporterConfig(
            XMLConfiguration xml) {
        if (xml == null) {
            return null;
        }
        ImporterConfig config = new ImporterConfig();
        try {
            config.loadFromXML(ConfigurationUtil.newReader(xml));
        } catch (Exception e) {
            throw configurationException("Could not load configuration "
                    + "from XMLConfiguration instance.", e);
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
