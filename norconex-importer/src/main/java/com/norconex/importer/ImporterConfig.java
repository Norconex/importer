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
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.parser.GenericDocumentParserFactory;
import com.norconex.importer.parser.IDocumentParserFactory;
import com.norconex.importer.response.IImporterResponseProcessor;

/**
 * Importer configuration.
 * @author Pascal Essiembre
 */
public class ImporterConfig implements IXMLConfigurable {

    public static final String DEFAULT_TEMP_DIR_PATH = 
            FileUtils.getTempDirectoryPath();
    public static final int DEFAULT_MAX_FILE_CACHE_SIZE = 
            (int) DataUnit.MB.toBytes(100);
    public static final int DEFAULT_MAX_FILE_POOL_CACHE_SIZE = 
            (int) DataUnit.GB.toBytes(1);
    
    private IDocumentParserFactory documentParserFactory = 
            new GenericDocumentParserFactory();

    private final List<IImporterHandler> preParseHandlers = new ArrayList<>();
    private final List<IImporterHandler> postParseHandlers = new ArrayList<>();
    private final List<IImporterResponseProcessor> responseProcessors = 
            new ArrayList<>();

    private File tempDir = new File(DEFAULT_TEMP_DIR_PATH);
    private int maxFileCacheSize = DEFAULT_MAX_FILE_CACHE_SIZE;
    private int maxFilePoolCacheSize = DEFAULT_MAX_FILE_POOL_CACHE_SIZE;
    private File parseErrorsSaveDir;
    
    public IDocumentParserFactory getParserFactory() {
        return documentParserFactory;
    }
    public void setParserFactory(IDocumentParserFactory parserFactory) {
        this.documentParserFactory = parserFactory;
    }
    
    public File getTempDir() {
        return tempDir;
    }
    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }
    
    /**
     * Gets the directory where file generating parsing errors will be saved.
     * Default is <code>null</code> (not storing errors).
     * @return directory where to save error files
     */
    public File getParseErrorsSaveDir() {
        return parseErrorsSaveDir;
    }
    /**
     * Sets the directory where file generating parsing errors will be saved.
     * @param parseErrorsSaveDir directory where to save error files
     */
    public void setParseErrorsSaveDir(File parseErrorsSaveDir) {
        this.parseErrorsSaveDir = parseErrorsSaveDir;
    }

    public List<IImporterHandler> getPreParseHandlers() {
        return Collections.unmodifiableList(preParseHandlers);
    }
    public void setPreParseHandlers(List<IImporterHandler> preParseHandlers) {
        this.preParseHandlers.clear();
        if (preParseHandlers != null) {
            this.preParseHandlers.addAll(preParseHandlers);
        }
    }

    public List<IImporterHandler> getPostParseHandlers() {
        return Collections.unmodifiableList(postParseHandlers);
    }
    public void setPostParseHandlers(List<IImporterHandler> postParseHandlers) {
        this.postParseHandlers.clear();
        if (postParseHandlers != null) {
            this.postParseHandlers.addAll(postParseHandlers);
        }
    }
    
    public List<IImporterResponseProcessor> getResponseProcessors() {
        return Collections.unmodifiableList(responseProcessors);
    }
    public void setResponseProcessors(
            List<IImporterResponseProcessor> responseProcessors) {
        this.responseProcessors.clear();
        if (responseProcessors != null) {
            this.responseProcessors.addAll(responseProcessors);
        }
    }

    public int getMaxFileCacheSize() {
        return maxFileCacheSize;
    }
    public void setMaxFileCacheSize(int maxFileCacheSize) {
        this.maxFileCacheSize = maxFileCacheSize;
    }
    
    public int getMaxFilePoolCacheSize() {
        return maxFilePoolCacheSize;
    }
    public void setMaxFilePoolCacheSize(int maxFilePoolCacheSize) {
        this.maxFilePoolCacheSize = maxFilePoolCacheSize;
    }
    @Override
    public void loadFromXML(Reader in) throws IOException {
        if (in == null) {
            return;
        }
        XML xml = new XML(in);

        //--- Temp directory -----------------------------------------------
        setTempDir(new File(xml.getString(
                "tempDir", ImporterConfig.DEFAULT_TEMP_DIR_PATH)));

        //--- Parse errors save dir ----------------------------------------
        String saveDir = xml.getString("parseErrorsSaveDir", null);
        setParseErrorsSaveDir(saveDir != null ? new File(saveDir) : null);
        
        //--- File Mem Cache Size ------------------------------------------
        setMaxFileCacheSize(xml.getInteger("maxFileCacheSize", 
                DEFAULT_MAX_FILE_CACHE_SIZE));
        //--- File Pool Mem Cache Size -------------------------------------
        setMaxFilePoolCacheSize(xml.getInteger("maxFilePoolCacheSize", 
                DEFAULT_MAX_FILE_POOL_CACHE_SIZE));
        
        //--- Pre-Import Handlers ------------------------------------------
        setPreParseHandlers(xml.getObjectList("preParseHandlers/*",
                getPreParseHandlers()));

        //--- Document Parser Factory --------------------------------------
        setParserFactory(
                xml.getObject("documentParserFactory", getParserFactory()));

        //--- Post-Import Handlers -----------------------------------------
        setPostParseHandlers(xml.getObjectList("postParseHandlers/*", 
                getPostParseHandlers()));
                    
        //--- Response Processors ------------------------------------------
        setResponseProcessors(xml.getObjectList(
                "responseProcessors/responseProcessor", 
                        getResponseProcessors()));
    }
    
    @Override
    public void saveToXML(Writer out,String elementName) throws IOException {
        EnhancedXMLStreamWriter w = new EnhancedXMLStreamWriter(out);
        w.writeStartElement(elementName);
        //writer.writeStartElement("importer");
        w.writeElementFile("tempDir", getTempDir());
        w.writeElementFile("parseErrorsSaveDir", getParseErrorsSaveDir());
        w.writeElementInteger("maxFileCacheSize", getMaxFileCacheSize());
        w.writeElementInteger(
                "maxFilePoolCacheSize", getMaxFilePoolCacheSize());
        w.writeObjectList(
                "preParseHandlers", "handler", getPreParseHandlers());
        w.writeObject("documentParserFactory", getParserFactory());
        w.writeObjectList(
                "postParseHandlers", "handler", getPostParseHandlers());
        
        
        w.writeObjectList("responseProcessors", "responseProcessor", 
                getResponseProcessors());
        
        w.writeEndElement();
        w.flush();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ImporterConfig)) {
            return false;
        }
        ImporterConfig castOther = (ImporterConfig) other;
        return new EqualsBuilder()
                .append(documentParserFactory, castOther.documentParserFactory)
                .append(preParseHandlers, castOther.preParseHandlers)
                .append(postParseHandlers, castOther.postParseHandlers)
                .append(responseProcessors, castOther.responseProcessors)
                .append(tempDir, castOther.tempDir)
                .append(maxFileCacheSize, castOther.maxFileCacheSize)
                .append(maxFilePoolCacheSize, castOther.maxFilePoolCacheSize)
                .append(parseErrorsSaveDir, castOther.parseErrorsSaveDir)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(documentParserFactory)
                .append(preParseHandlers)
                .append(postParseHandlers)
                .append(responseProcessors)
                .append(tempDir)
                .append(maxFileCacheSize)
                .append(maxFilePoolCacheSize)
                .append(parseErrorsSaveDir)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("documentParserFactory", documentParserFactory)
                .append("preParseHandlers", preParseHandlers)
                .append("postParseHandlers", postParseHandlers)
                .append("responseProcessors", responseProcessors)
                .append("tempDir", tempDir)
                .append("maxFileCacheSize", maxFileCacheSize)
                .append("maxFilePoolCacheSize", maxFilePoolCacheSize)
                .append("parseErrorsSaveDir", parseErrorsSaveDir)
                .toString();
    }    
}
