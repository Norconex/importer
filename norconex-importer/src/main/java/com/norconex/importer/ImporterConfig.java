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
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ExpressionEngine;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.parser.DefaultDocumentParserFactory;
import com.norconex.importer.parser.IDocumentParserFactory;
import com.norconex.importer.response.IImporterResponseProcessor;

/**
 * Importer configuration.
 * @author Pascal Essiembre
 */
public class ImporterConfig implements IXMLConfigurable {

    private static final long serialVersionUID = -7110188100703942075L;
    private static final Logger LOG = 
            LogManager.getLogger(ImporterConfig.class);
    
    public static final String DEFAULT_TEMP_DIR_PATH = 
            FileUtils.getTempDirectoryPath();
    public static final int DEFAULT_MAX_FILE_CACHE_SIZE = 
            (int) DataUnit.MB.toBytes(1);
    public static final int DEFAULT_MAX_FILE_POOL_CACHE_SIZE = 
            (int) DataUnit.MB.toBytes(10);
    
    private IDocumentParserFactory documentParserFactory = 
            new DefaultDocumentParserFactory();

    private IImporterHandler[] preParseHandlers;
    private IImporterHandler[] postParseHandlers;
    private IImporterResponseProcessor[] responseProcessors;

    private File tempDir = new File(DEFAULT_TEMP_DIR_PATH);
    private int maxFileCacheSize = DEFAULT_MAX_FILE_CACHE_SIZE;
    private int maxFilePoolCacheSize = DEFAULT_MAX_FILE_POOL_CACHE_SIZE;
    
    
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

    public void setPreParseHandlers(IImporterHandler... handlers) {
        preParseHandlers = handlers;
    }
    public IImporterHandler[] getPreParseHandlers() {
        return preParseHandlers;
    }

    public void setPostParseHandlers(IImporterHandler... handlers) {
        postParseHandlers = handlers;
    }
    public IImporterHandler[] getPostParseHandlers() {
        return postParseHandlers;
    }
    
    public IImporterResponseProcessor[] getResponseProcessors() {
        return responseProcessors;
    }
    public void setResponseProcessors(
            IImporterResponseProcessor... responseProcessors) {
        this.responseProcessors = responseProcessors;
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
        XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
        try {
            //--- Temp directory -----------------------------------------------
            setTempDir(new File(xml.getString(
                    "tempDir", ImporterConfig.DEFAULT_TEMP_DIR_PATH)));
    
            //--- File Mem Cache Size ------------------------------------------
            setMaxFileCacheSize(xml.getInt("maxFileCacheSize", 
                    ImporterConfig.DEFAULT_MAX_FILE_CACHE_SIZE));
            //--- File Pool Mem Cache Size ------------------------------------------
            setMaxFileCacheSize(xml.getInt("maxFilePoolCacheSize", 
                    ImporterConfig.DEFAULT_MAX_FILE_POOL_CACHE_SIZE));
            
            //--- Pre-Import Handlers ------------------------------------------
            setPreParseHandlers(loadImportHandlers(xml, "preParseHandlers"));
    
            //--- Document Parser Factory --------------------------------------
            setParserFactory(ConfigurationUtil.newInstance(
                    xml, "documentParserFactory", getParserFactory()));
    
            //--- Post-Import Handlers -----------------------------------------
            setPostParseHandlers(loadImportHandlers(xml, "postParseHandlers"));
                        
            //--- Response Processors ------------------------------------------
            setResponseProcessors(loadResponseProcessors(
                    xml, "responseProcessors.responseProcessor"));
        } catch (Exception e) {
            throw new ConfigurationException("Could not load configuration "
                    + "from XMLConfiguration instance.", e);
        }
    }
    
    private IImporterHandler[] loadImportHandlers(
            XMLConfiguration xml, String xmlPath) {
        List<IImporterHandler> handlers = new ArrayList<>();
    
        ExpressionEngine originalEngine = xml.getExpressionEngine();
        xml.setExpressionEngine(new XPathExpressionEngine());
        List<HierarchicalConfiguration> xmlHandlers = 
                xml.configurationsAt(xmlPath + "/*");
        xml.setExpressionEngine(originalEngine);
        for (HierarchicalConfiguration xmlHandler : xmlHandlers) {
            xmlHandler.setExpressionEngine(originalEngine);
            handlers.add((IImporterHandler) 
                    ConfigurationUtil.newInstance(xmlHandler));
        }
        return handlers.toArray(new IImporterHandler[]{});
    }
    

    private IImporterResponseProcessor[] loadResponseProcessors(
            XMLConfiguration xml, String xmlPath) {
        List<IImporterResponseProcessor> processors = new ArrayList<>();
    
        List<HierarchicalConfiguration> procNodes = 
                xml.configurationsAt(xmlPath);
        for (HierarchicalConfiguration procNode : procNodes) {
            IImporterResponseProcessor proc = 
                    ConfigurationUtil.newInstance(procNode);
            if (proc != null) {
                processors.add(proc);
                LOG.info("Reponse processor loaded: " + proc);
            } else {
                LOG.error("Problem loading reponse processors, "
                        + "please check for other log messages.");
            }
        }
        return processors.toArray(new IImporterResponseProcessor[] {});
    }
    
    @Override
    public void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            writer.writeStartElement("importer");
            writer.writeElementString("tempdir", getTempDir().toString());
            writer.writeElementInteger(
                    "maxFileCacheSize", getMaxFileCacheSize());
            writer.writeElementInteger(
                    "maxFilePoolCacheSize", getMaxFilePoolCacheSize());
            writer.flush();
            
            writeHandlers(out, "preParseHandlers", getPreParseHandlers());
            writeObject(out, "documentParserFactory", getParserFactory());
            writeHandlers(out, "postParseHandlers", getPostParseHandlers());
            writeResponseProcessors(
                    out, "responseProcessors", getResponseProcessors());
            
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException("Could not save importer config.", e);
        }
    }

    private void writeResponseProcessors(Writer out, String listTagName, 
            IImporterResponseProcessor[] processors) throws IOException {
        if (ArrayUtils.isEmpty(processors)) {
            return;
        }
        out.write("<" + listTagName + ">"); 
        for (IImporterResponseProcessor processor: processors) {
            writeObject(out, null, processor);
        }
        out.write("</" + listTagName + ">"); 
        out.flush();
    }
    private void writeHandlers(Writer out, String listTagName, 
            IImporterHandler[] handlers) throws IOException {
        if (ArrayUtils.isEmpty(handlers)) {
            return;
        }
        out.write("<" + listTagName + ">"); 
        for (IImporterHandler handler: handlers) {
            writeObject(out, null, handler);
        }
        out.write("</" + listTagName + ">"); 
        out.flush();
    }
    private void writeObject(
            Writer out, String tagName, Object object) throws IOException {
        writeObject(out, tagName, object, false);
    }
    private void writeObject(
            Writer out, String tagName, Object object, boolean ignore) 
                    throws IOException {
        if (object == null) {
            if (ignore) {
                out.write("<" + tagName + " ignore=\"" + ignore + "\" />");
            }
            return;
        }
        StringWriter w = new StringWriter();
        if (object instanceof IXMLConfigurable) {
            ((IXMLConfigurable) object).saveToXML(w);
        } else {
            w.write("<" + tagName + " class=\"" 
                    + object.getClass().getCanonicalName() + "\" />");
        }
        if (ignore) {
            w.toString().replace("<" + tagName + " class=\"" , 
                    "<" + tagName + " ignore=\"true\" class=\"" );
        }
        out.flush();
    }
}
