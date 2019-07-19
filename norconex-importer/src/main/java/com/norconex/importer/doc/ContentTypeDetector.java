/* Copyright 2014-2019 Norconex Inc.
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
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.file.ContentType;

/**
 * Master class to detect all content types.  This class is thread-safe.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class ContentTypeDetector {

    private static final Logger LOG =
            LoggerFactory.getLogger(ContentTypeDetector.class);

    private static final Pattern EXTENSION_PATTERN =
            Pattern.compile("^.*(\\.[A-z0-9]+).*");
    private static final Tika TIKA = new Tika();

    /**
     * Constructor.
     */
    private ContentTypeDetector() {
        super();
    }
    /**
     * Detects the content type of the given file.
     * @param file file on which to detect content type
     * @return the detected content type
     * @throws IOException problem detecting content type
     */
    public static ContentType detect(File file) throws IOException {
        return detect(file, file.getName());
    }
    /**
     * Detects the content type of the given file.
     * @param file file on which to detect content type
     * @param fileName a file name which can help influence detection
     * @return the detected content type
     * @throws IOException problem detecting content type
     */
    public static ContentType detect(
            File file, String fileName) throws IOException {
        String safeFileName = fileName;
        if (StringUtils.isBlank(safeFileName)) {
            safeFileName = file.getName();
        }
        return doDetect(TikaInputStream.get(file.toPath()), safeFileName);
    }
    /**
     * Detects the content type from the given input stream.
     * @param content the content on which to detect content type
     * @return the detected content type
     * @throws IOException problem detecting content type
     */
    public static ContentType detect(InputStream content)
            throws IOException {
        String contentType = TIKA.detect(content);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Detected \"" + contentType
                    + "\" content-type for input stream.");
        }
        return ContentType.valueOf(contentType);
    }
    /**
     * Detects the content type from the given input stream.
     * @param content the content on which to detect content type
     * @param fileName a file name which can help influence detection
     * @return the detected content type
     * @throws IOException problem detecting content type
     */
    public static ContentType detect(InputStream content, String fileName)
            throws IOException {
        return doDetect(content, fileName);
    }

    private static ContentType doDetect(
            InputStream is, String fileName) throws IOException {
        try (TikaInputStream tikaStream = TikaInputStream.get(is)) {
            Metadata meta = new Metadata();
            String extension = EXTENSION_PATTERN.matcher(
                    fileName).replaceFirst("$1");
            meta.set(Metadata.RESOURCE_NAME_KEY, "file:///detect" + extension);
            MediaType media = TIKA.getDetector().detect(tikaStream, meta);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Detected \"" + media.toString()
                        + "\" content-type for: " + fileName);
            }
            return ContentType.valueOf(media.toString());
        }
    }
}
