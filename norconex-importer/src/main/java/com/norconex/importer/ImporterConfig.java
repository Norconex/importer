/* Copyright 2010-2013 Norconex Inc.
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
import java.io.Serializable;

import org.apache.commons.io.FileUtils;

import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.parser.DefaultDocumentParserFactory;
import com.norconex.importer.parser.IDocumentParserFactory;

/**
 * Importer configuration.
 * @author Pascal Essiembre
 */
public class ImporterConfig implements Serializable {

    private static final long serialVersionUID = -7110188100703942075L;

    public static final String DEFAULT_TEMP_DIR_PATH = 
            FileUtils.getTempDirectoryPath();
    public static final int DEFAULT_FILE_MEM_CACHE_SIZE = 
            (int) DataUnit.MB.toBytes(1);
    
    private IDocumentParserFactory documentParserFactory = 
            new DefaultDocumentParserFactory();

    private IImporterHandler[] preParseHandlers;
    private IImporterHandler[] postParseHandlers;

    private File tempDir = new File(DEFAULT_TEMP_DIR_PATH);
    private int fileMemCacheSize = DEFAULT_FILE_MEM_CACHE_SIZE;
    
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

    public int getFileMemCacheSize() {
        return fileMemCacheSize;
    }
    public void setFileMemCacheSize(int fileMemCacheSize) {
        this.fileMemCacheSize = fileMemCacheSize;
    }
}
