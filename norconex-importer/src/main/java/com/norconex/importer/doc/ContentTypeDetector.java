/* Copyright 2014-2015 Norconex Inc.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
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
//TODO force the use of CachedInputStream?
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
        return narrowContentType(ContentType.valueOf(contentType), content);        
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
        return narrowContentType(ContentType.valueOf(media.toString()), is);
    }
    
    private ContentType narrowContentType(ContentType ct, InputStream is)
            throws IOException {
        try {
            if (ct == null || StringUtils.isBlank(ct.toString())) {
                return ct;
            }
            if ("application/x-tika-ooxml".equalsIgnoreCase(ct.toString())) {
                return detectMSOfficeOOXML(is, ct);
            } else if (ct.toString().startsWith("application/x-tika")) {
                return detectOLE(is, ct);
            }
            return ct;
        } finally {
            if (is instanceof BufferedInputStream) {
                ((BufferedInputStream) is).reset();
            }
        }
    }
    
    //--- MS Office OOXML Formats ----------------------------------------------
    //TODO create an interface to plug custom detectors, or move part
    //of Tika detector in case it behaves better with embedded docs
    private static final String OOXML_TYPE_PREFIX = 
            "application/vnd.openxmlformats-officedocument.";
    private static final String MS_TYPE_PREFIX = "application/vnd.ms-";
    private static final MultiValueMap<String, String> OOXML_PART_TYPES = 
            new MultiValueMap<>();
    static {
        //TODO check if .12 is really required for some types?
        OOXML_PART_TYPES.putAll("/word/document.xml", Arrays.asList(
                OOXML_TYPE_PREFIX + "wordprocessingml.document",
                OOXML_TYPE_PREFIX + "wordprocessingml.template",
                MS_TYPE_PREFIX + "word.document.macroEnabled.12",
                MS_TYPE_PREFIX + "word.document.macroEnabled",
                MS_TYPE_PREFIX + "word.template.macroEnabled.12",
                MS_TYPE_PREFIX + "word.template.macroEnabled"
        ));
        OOXML_PART_TYPES.putAll("/ppt/presentation.xml", Arrays.asList(
                OOXML_TYPE_PREFIX + "presentationml.presentation",
                OOXML_TYPE_PREFIX + "presentationml.template",
                OOXML_TYPE_PREFIX + "presentationml.slideshow"
        ));
        OOXML_PART_TYPES.putAll("/xl/workbook.xml", Arrays.asList(
                OOXML_TYPE_PREFIX + "spreadsheetml.sheet",
                OOXML_TYPE_PREFIX + "spreadsheetml.template",
                MS_TYPE_PREFIX + "excel.sheet.macroEnabled.12",
                MS_TYPE_PREFIX + "excel.sheet.macroEnabled",
                MS_TYPE_PREFIX + "excel.template.macroEnabled.12",
                MS_TYPE_PREFIX + "excel.template.macroEnabled",
                MS_TYPE_PREFIX + "excel.addin.macroEnabled.12",
                MS_TYPE_PREFIX + "excel.addin.macroEnabled",
                MS_TYPE_PREFIX + "excel.sheet.binary.macroEnabled.12",
                MS_TYPE_PREFIX + "excel.sheet.binary.macroEnabled"
        ));
        OOXML_PART_TYPES.putAll("/visio/document.xml", Arrays.asList(
                MS_TYPE_PREFIX + "visio.drawing",
                MS_TYPE_PREFIX + "visio.template",
                MS_TYPE_PREFIX + "visio.stencil",
                MS_TYPE_PREFIX + "visio.drawing.macroEnabled",
                MS_TYPE_PREFIX + "visio.template.macroEnabled",
                MS_TYPE_PREFIX + "visio.stencil.macroEnabled"
        ));
    }
    private ContentType detectMSOfficeOOXML(
            InputStream is, ContentType defaultContentType) throws IOException {
        ZipInputStream zin = new ZipInputStream(is);
        for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
            if ("[Content_Types].xml".equals(e.getName())) {
                String content = IOUtils.toString(zin);
                ContentType ct = detectMSOfficeOOXML(content);
                if (ct != null) {
                    return ct;
                }
            }
        }
        return defaultContentType;
    }
    private ContentType detectMSOfficeOOXML(String content) {
        Iterator<Entry<String, String>> it = OOXML_PART_TYPES.iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            if (content.contains("<Override PartName=\""
                    + entry.getKey() + "\" ContentType=\""
                    + entry.getValue() + ".main+xml\"/>")) {
                return ContentType.valueOf(entry.getValue());
            }
        }
        return null;
    }

    //--- MS + Corel OLE Formats -----------------------------------------------
    private static final byte[] ID_QUATTRO_PRO =
            new byte[]{ 81, 80, 87, 57 };  // QPW9
    private ContentType detectOLE(
            InputStream is, ContentType defaultContentType) throws IOException {
        
        POIFSFileSystem poi = new POIFSFileSystem(
                POIFSFileSystem.createNonClosingInputStream(is));

        Set<String> entryNames = poi.getRoot().getEntryNames();
        for (String name : entryNames) {
            if ("WordDocument".equals(name)) {
                return ContentType.valueOf("application/vnd.ms-word");
            } else if ("NativeContent_MAIN".equals(name)) {
                ContentType ct = detectOLE(
                        poi.createDocumentInputStream("NativeContent_MAIN"));
                if (ct != null) {
                    return ct;
                }
            }
        }
        return defaultContentType;
    }
    private ContentType detectOLE(InputStream is) throws IOException {
        is.mark(16);
        byte[] bytes = new byte[16];
        int bytesRead = is.read(bytes);
        is.reset();
        if (bytesRead < 16) {
            return null;
        }
        if (Arrays.equals(ID_QUATTRO_PRO, ArrayUtils.subarray(bytes, 4, 8))) {
            return ContentType.valueOf("application/x-quattro-pro");
        }
        return null;
    }
    
}
