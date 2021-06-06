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
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.flow.XMLFlow;
import com.norconex.importer.handler.HandlerConsumer;
import com.norconex.importer.handler.HandlerContext;
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
            DataUnit.MB.toBytes(100).intValue();
    public static final int DEFAULT_MAX_MEM_POOL =
            DataUnit.GB.toBytes(1).intValue();

    private IDocumentParserFactory documentParserFactory =
            new GenericDocumentParserFactory();

    private XMLFlow<HandlerContext> xmlFlow = new XMLFlow<>(
            HandlerConsumer.class, null);


//    @Deprecated
//    private final List<IImporterHandler> preParseHandlers = new ArrayList<>();
//    @Deprecated
//    private final List<IImporterHandler> postParseHandlers = new ArrayList<>();

    private Consumer<HandlerContext> preParseConsumer;
    private Consumer<HandlerContext> postParseConsumer;

    private final List<IImporterResponseProcessor> responseProcessors =
            new ArrayList<>();

    private Path tempDir = Paths.get(DEFAULT_TEMP_DIR_PATH);
    private int maxFileCacheSize = DEFAULT_MAX_MEM_INSTANCE;
    private int maxFilePoolCacheSize = DEFAULT_MAX_MEM_POOL;
    private Path parseErrorsSaveDir;

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

    //TODO document that this is mainly for XML configuration and
    // is overwritten when using set/get pre/post handler (which are no longer
    // loaded by configuration.
    //TODO Document to use HandlerConsumer#fromImporterHanders(...)
    // or FunctionUtil.allConsumers(...)

    public Consumer<HandlerContext> getPreParseConsumer() {
        return preParseConsumer;
    }
    public void setPreParseConsumer(Consumer<HandlerContext> consumer) {
        this.preParseConsumer = consumer;
    }
    public Consumer<HandlerContext> getPostParseConsumer() {
        return postParseConsumer;
    }
    public void setPostParseConsumer(Consumer<HandlerContext> consumer) {
        this.postParseConsumer = consumer;
    }

    //TODO document it is now unreliable if you use XMLFlow
    @Deprecated
    public List<IImporterHandler> getPreParseHandlers() {
        //TODO Document behavior (walking through all impls)
        // Document this will break any flow usage.
        List<IImporterHandler> handlers = new ArrayList<>();
        BeanUtil.visitAll(
                preParseConsumer, handlers::add, IImporterHandler.class);
        return Collections.unmodifiableList(handlers);
        //return Collections.unmodifiableList(preParseHandlers);
    }
    //TODO document it is now unreliable if you use XMLFlow
    @Deprecated
    public void setPreParseHandlers(List<IImporterHandler> preParseHandlers) {
        setPreParseConsumer(HandlerConsumer.fromHandlers(preParseHandlers));
        //CollectionUtil.setAll(this.preParseHandlers, preParseHandlers);
    }

    @Deprecated
    public List<IImporterHandler> getPostParseHandlers() {
        List<IImporterHandler> handlers = new ArrayList<>();
        BeanUtil.visitAll(
                postParseConsumer, handlers::add, IImporterHandler.class);
        return Collections.unmodifiableList(handlers);
//        return Collections.unmodifiableList(postParseHandlers);
    }
    @Deprecated
    public void setPostParseHandlers(List<IImporterHandler> postParseHandlers) {
        setPostParseConsumer(HandlerConsumer.fromHandlers(postParseHandlers));
        //CollectionUtil.setAll(this.postParseHandlers, postParseHandlers);
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

        //TODO what to call the new tag?  preParseFlow?  Keep it preParseHandlers?
        //TODO maybe do not deprecate the XML at all, just the methods?  Yeah, probably best
        //xml.checkDeprecated("preParseHandlers", "preParseFlow", false);

        setPreParseConsumer(xmlFlow.parse(xml.getXML("preParseHandlers")));
//        setPreParseHandlers(xml.getObjectListImpl(
//                IImporterHandler.class, "preParseHandlers/*", preParseHandlers));
        setParserFactory(xml.getObjectImpl(IDocumentParserFactory.class,
                "documentParserFactory", documentParserFactory));
//        setPostParseHandlers(xml.getObjectListImpl(IImporterHandler.class,
//                "postParseHandlers/*", postParseHandlers));
        setPostParseConsumer(xmlFlow.parse(xml.getXML("postParseHandlers")));

        setResponseProcessors(xml.getObjectListImpl(
                IImporterResponseProcessor.class,
                "responseProcessors/responseProcessor", responseProcessors));
    }

    @Override
    public void saveToXML(XML xml) {
        xml.addElement("tempDir", tempDir);
        xml.addElement("parseErrorsSaveDir", parseErrorsSaveDir);
        xml.addElement("maxFileCacheSize", maxFileCacheSize);
        xml.addElement("maxFilePoolCacheSize", maxFilePoolCacheSize);
        xmlFlow.write(xml.addElement("preParseHandlers"), preParseConsumer);
//        xml.addElementList("preParseHandlers", "handler", preParseHandlers);
        xml.addElement("documentParserFactory", documentParserFactory);
//        xml.addElementList("postParseHandlers", "handler", postParseHandlers);
        xmlFlow.write(xml.addElement("postParseHandlers"), preParseConsumer);
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
