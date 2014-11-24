/* Copyright 2014 Norconex Inc.
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
