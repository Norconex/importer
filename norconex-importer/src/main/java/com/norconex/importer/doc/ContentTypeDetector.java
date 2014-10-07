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
package com.norconex.importer.doc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.norconex.commons.lang.file.ContentType;

/**
 * Detects content types.  This class is thread-safe.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ContentTypeDetector {

    private static final Logger LOG = 
            LogManager.getLogger(ContentTypeDetector.class);
    
    private final Pattern extPattern = Pattern.compile("^.*(\\.[A-z0-9]+).*");
    
    private TikaConfig tikaConfig;

    public ContentTypeDetector() {
        super();
    }
    
    public ContentType detect(File file) throws IOException {
        return detect(file, file.getName());
    }
    public ContentType detect(
            File file, String fileName) throws IOException {
        String safeFileName = fileName;
        if (StringUtils.isBlank(safeFileName)) {
            safeFileName = file.getName();
        }
        return doDetect(TikaInputStream.get(file), safeFileName);
    }
    public ContentType detect(InputStream content)
            throws IOException {
        Tika tika = new Tika();
        String contentType = tika.detect(content);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Detected \"" + contentType
                    + "\" content-type for input stream.");
        }
        return ContentType.valueOf(contentType);        
    }
    public ContentType detect(InputStream content, String fileName)
            throws IOException {
        return doDetect(content, fileName);
    }
    
    private TikaConfig getTikaConfig() throws IOException {
        if (tikaConfig == null) {
            try {
                initTikaConfig();
                this.tikaConfig = new TikaConfig();
            } catch (TikaException | IOException e) {
                throw new IOException("Could not create Tika Configuration "
                        + "for content type detector.", e);
            }
        }
        return tikaConfig;
    }
    private synchronized void initTikaConfig() throws IOException {
        if (tikaConfig != null) {
            return;
        }
        try {
            this.tikaConfig = new TikaConfig();
        } catch (TikaException | IOException e) {
            throw new IOException("Could not create Tika Configuration "
                    + "for content type detector.", e);
        }
    }
    
    private ContentType doDetect(
            InputStream is, String fileName) throws IOException {
        Metadata meta = new Metadata();
        String extension = extPattern.matcher(fileName).replaceFirst("$1");
        meta.set(Metadata.RESOURCE_NAME_KEY, "file:///detect" + extension);
        MediaType media = getTikaConfig().getDetector().detect(is, meta);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Detected \"" + media.toString()
                    + "\" content-type for: " + fileName);
        }
        return ContentType.valueOf(media.toString());
    }
}
