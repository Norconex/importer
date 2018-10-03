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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.xml.IXMLConfigurable;
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
    public static final int DEFAULT_MAX_MEM_INSTANCE =
            (int) DataUnit.MB.toBytes(100);
    public static final int DEFAULT_MAX_MEM_POOL =
            (int) DataUnit.GB.toBytes(1);

    private IDocumentParserFactory documentParserFactory =
            new GenericDocumentParserFactory();

    private final List<IImporterHandler> preParseHandlers = new ArrayList<>();
    private final List<IImporterHandler> postParseHandlers = new ArrayList<>();
    private final List<IImporterResponseProcessor> responseProcessors =
            new ArrayList<>();

    private Path tempDir = Paths.get(DEFAULT_TEMP_DIR_PATH);
    private int maxFileCacheSize = DEFAULT_MAX_MEM_INSTANCE;
    private int maxFilePoolCacheSize = DEFAULT_MAX_MEM_POOL;
    private Path parseErrorsSaveDir;

//    private int maxMemoryPool;
//    private int maxMemoryInstance;

    public IDocumentParserFactory getParserFactory() {
        return documentParserFactory;
    }
    public void setParserFactory(IDocumentParserFactory parserFactory) {
        this.documentParserFactory = parserFactory;
    }

    public Path getTempDir() {
        return tempDir;
    }
    public void setTempDir(Path tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * Gets the directory where file generating parsing errors will be saved.
     * Default is <code>null</code> (not storing errors).
     * @return directory where to save error files
     */
    public Path getParseErrorsSaveDir() {
        return parseErrorsSaveDir;
    }
    /**
     * Sets the directory where file generating parsing errors will be saved.
     * @param parseErrorsSaveDir directory where to save error files
     */
    public void setParseErrorsSaveDir(Path parseErrorsSaveDir) {
        this.parseErrorsSaveDir = parseErrorsSaveDir;
    }

    public List<IImporterHandler> getPreParseHandlers() {
        return Collections.unmodifiableList(preParseHandlers);
    }
    public void setPreParseHandlers(List<IImporterHandler> preParseHandlers) {
        CollectionUtil.setAll(this.preParseHandlers, preParseHandlers);
    }

    public List<IImporterHandler> getPostParseHandlers() {
        return Collections.unmodifiableList(postParseHandlers);
    }
    public void setPostParseHandlers(List<IImporterHandler> postParseHandlers) {
        CollectionUtil.setAll(this.postParseHandlers, postParseHandlers);
    }

    public List<IImporterResponseProcessor> getResponseProcessors() {
        return Collections.unmodifiableList(responseProcessors);
    }
    public void setResponseProcessors(
            List<IImporterResponseProcessor> responseProcessors) {
        CollectionUtil.setAll(this.responseProcessors, responseProcessors);
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
    public void loadFromXML(XML xml) {
        setTempDir(xml.getPath("tempDir", tempDir));
        setParseErrorsSaveDir(
                xml.getPath("parseErrorsSaveDir", parseErrorsSaveDir));
        setMaxFileCacheSize(
                xml.getInteger("maxFileCacheSize", maxFileCacheSize));
        setMaxFilePoolCacheSize(
                xml.getInteger("maxFilePoolCacheSize", maxFilePoolCacheSize));
        setPreParseHandlers(
                xml.getObjectList("preParseHandlers/*", preParseHandlers));
        setParserFactory(
                xml.getObject("documentParserFactory", documentParserFactory));
        setPostParseHandlers(
                xml.getObjectList("postParseHandlers/*", postParseHandlers));
        setResponseProcessors(xml.getObjectList(
                "responseProcessors/responseProcessor", responseProcessors));
    }

    @Override
    public void saveToXML(XML xml) {
        xml.addElement("tempDir", tempDir);
        xml.addElement("parseErrorsSaveDir", parseErrorsSaveDir);
        xml.addElement("maxFileCacheSize", maxFileCacheSize);
        xml.addElement("maxFilePoolCacheSize", maxFilePoolCacheSize);
        xml.addElementList("preParseHandlers", "handler", preParseHandlers);
        xml.addElement("documentParserFactory", documentParserFactory);
        xml.addElementList("postParseHandlers", "handler", postParseHandlers);
        xml.addElementList(
                "responseProcessors", "responseProcessor", responseProcessors);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
