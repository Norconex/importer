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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.mime.MimeTypesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.file.ContentType;

/**
 * Master class to detect all content types. This class is thread-safe.
 * 
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class ContentTypeDetector {

    private static final Logger LOG = LoggerFactory.getLogger(ContentTypeDetector.class);

    private static final Pattern EXTENSION_PATTERN = Pattern.compile("^.*(\\.[A-z0-9]+).*");
        private static final byte[] OLE_HEADER = new byte[] {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
        };
        private static final byte[] ZIP_HEADER = new byte[] {
            0x50, 0x4B, 0x03, 0x04
        };
    private static final MimeTypes CUSTOM_MIME_TYPES;
    private static final Detector DETECTOR;
    private static final Tika TIKA;
    static {
        configureCustomMimeTypes();
        CUSTOM_MIME_TYPES = createCustomMimeTypes();
        DETECTOR = new DefaultDetector(CUSTOM_MIME_TYPES);
        TIKA = new Tika(DETECTOR);
    }

    /**
     * Constructor.
     */
    private ContentTypeDetector() {
        super();
    }

    private static void configureCustomMimeTypes() {
        if (System.getProperty(MimeTypesFactory.CUSTOM_MIMES_SYS_PROP) != null) {
            return;
        }
        URL resource = ContentTypeDetector.class.getClassLoader().getResource(
                "org/apache/tika/mime/custom-mimetypes.xml");
        if (resource == null) {
            return;
        }
        try {
            if ("file".equalsIgnoreCase(resource.getProtocol())) {
                Path path = Path.of(resource.toURI());
                System.setProperty(MimeTypesFactory.CUSTOM_MIMES_SYS_PROP,
                        path.toString());
                return;
            }
            try (InputStream in = resource.openStream()) {
                Path temp = Files.createTempFile(
                        "tika-custom-mimetypes-", ".xml");
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                temp.toFile().deleteOnExit();
                System.setProperty(MimeTypesFactory.CUSTOM_MIMES_SYS_PROP,
                        temp.toString());
            }
        } catch (Exception e) {
            LOG.warn("Could not register custom mime types resource.", e);
        }
    }

    private static MimeTypes createCustomMimeTypes() {
        try {
            return MimeTypesFactory.create(
                    "tika-mimetypes.xml",
                    "custom-mimetypes.xml",
                    ContentTypeDetector.class.getClassLoader());
        } catch (IOException | MimeTypeException e) {
            LOG.warn("Could not load custom mime types; falling back to defaults.", e);
            return MimeTypes.getDefaultMimeTypes();
        }
    }

    /**
     * Detects the content type of the given file.
     * 
     * @param file file on which to detect content type
     * @return the detected content type
     * @throws IOException problem detecting content type
     */
    public static ContentType detect(File file) throws IOException {
        return detect(file, file.getName());
    }

    /**
     * Detects the content type of the given file.
     * 
     * @param file     file on which to detect content type
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
     * 
     * @param content the content on which to detect content type
     * @return the detected content type
     * @throws IOException problem detecting content type
     */
    public static ContentType detect(InputStream content)
            throws IOException {
        return doDetect(content, null);
    }

    /**
     * Detects the content type from the given input stream.
     * 
     * @param content  the content on which to detect content type
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
            if (StringUtils.isNotBlank(fileName)) {
                meta.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }
            String extension = extractExtension(fileName);
            MediaType media;
            try {
                tikaStream.mark(Integer.MAX_VALUE);
                media = DETECTOR.detect(tikaStream, meta);
            } catch (Exception e) {
                if (e instanceof ArchiveException
                        || e.getCause() instanceof ArchiveException) {
                    try {
                        tikaStream.reset();
                        media = CUSTOM_MIME_TYPES.detect(tikaStream, meta);
                        return ContentType.valueOf(media.toString());
                    } catch (IOException | RuntimeException resetFailure) {
                        String fallback = TIKA.detect(fileName);
                        if (StringUtils.isBlank(fallback)) {
                            fallback = MediaType.OCTET_STREAM.toString();
                        }
                        return ContentType.valueOf(fallback);
                    }
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new IOException(
                        "Could not detect content type for " + fileName, e);
            }

            if (extension != null) {
                media = normalizeOfficeMimeByExtension(media, extension);
            } else {
                media = normalizeOfficeMimeByContent(media, tikaStream);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Detected \"" + media.toString()
                        + "\" content-type for: " + fileName);
            }
            return ContentType.valueOf(media.toString());
        }
    }

    private static MediaType normalizeOfficeMimeByContent(
            MediaType media, TikaInputStream stream) {
        if (media == null) {
            return media;
        }
        String mediaString = media.toString();
        if (!"application/x-tika-msoffice".equals(mediaString)
                && !"application/x-tika-ooxml".equals(mediaString)
                && !"application/zip".equals(mediaString)
                && !MediaType.OCTET_STREAM.toString().equals(mediaString)) {
            return media;
        }
        try {
            stream.reset();
        } catch (IOException e) {
            return media;
        }
        if (hasHeader(stream, OLE_HEADER)) {
            MediaType oleType = detectOleOfficeType(stream);
            if (oleType != null) {
                return oleType;
            }
        }
        if (hasHeader(stream, ZIP_HEADER)) {
            MediaType ooxmlType = detectOoxmlTypeFromZip(stream);
            if (ooxmlType != null) {
                return ooxmlType;
            }
        }
        return media;
    }

    private static boolean hasHeader(TikaInputStream stream, byte[] header) {
        try {
            stream.mark(header.length);
            byte[] buf = new byte[header.length];
            int read = stream.read(buf);
            stream.reset();
            return read == header.length && Arrays.equals(buf, header);
        } catch (IOException e) {
            return false;
        }
    }

    private static MediaType detectOleOfficeType(InputStream stream) {
        try (POIFSFileSystem fs = new POIFSFileSystem(stream)) {
            DirectoryNode root = fs.getRoot();
            if (root.hasEntry("WordDocument")) {
                return MediaType.parse("application/msword");
            }
            if (root.hasEntry("Workbook") || root.hasEntry("Book")) {
                return MediaType.parse("application/vnd.ms-excel");
            }
            if (root.hasEntry("PowerPoint Document")) {
                return MediaType.parse("application/vnd.ms-powerpoint");
            }
            if (root.hasEntry("VisioDocument")) {
                return MediaType.parse("application/vnd.visio");
            }
            MediaType appType = detectOleApplicationName(root);
            if (appType != null) {
                return appType;
            }
        } catch (OfficeXmlFileException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static MediaType detectOleApplicationName(DirectoryNode root) {
        String appName = null;
        try {
            if (root.hasEntry("\u0005SummaryInformation")) {
                DocumentEntry entry = (DocumentEntry) root.getEntry(
                        "\u0005SummaryInformation");
                try (DocumentInputStream dis = new DocumentInputStream(entry)) {
                    SummaryInformation info = (SummaryInformation) PropertySetFactory.create(dis);
                    appName = info.getApplicationName();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        if (StringUtils.isBlank(appName)) {
            return null;
        }
        if (appName == null) {
            return null;
        }
        String lower = appName.toLowerCase();
        if (lower.contains("word")) {
            return MediaType.parse("application/msword");
        }
        if (lower.contains("excel")) {
            return MediaType.parse("application/vnd.ms-excel");
        }
        if (lower.contains("powerpoint")) {
            return MediaType.parse("application/vnd.ms-powerpoint");
        }
        if (lower.contains("visio")) {
            return MediaType.parse("application/vnd.visio");
        }
        if (lower.contains("publisher")) {
            return MediaType.parse("application/x-mspublisher");
        }
        return null;
    }

    private static MediaType detectOoxmlTypeFromZip(InputStream stream) {
        String contentTypes = null;
        boolean hasWord = false;
        boolean hasExcel = false;
        boolean hasPowerPoint = false;
        try (ZipInputStream zis = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if ("[Content_Types].xml".equals(name)) {
                    contentTypes = IOUtils.toString(zis, StandardCharsets.UTF_8);
                } else if (name.startsWith("word/")) {
                    hasWord = true;
                } else if (name.startsWith("xl/")) {
                    hasExcel = true;
                } else if (name.startsWith("ppt/")) {
                    hasPowerPoint = true;
                }
                if (contentTypes != null
                        && (hasWord || hasExcel || hasPowerPoint)) {
                    // keep scanning to end of entry, then break
                }
            }
        } catch (IOException e) {
            return null;
        }
        if (contentTypes != null) {
            String ct = contentTypes;
            if (ct.contains("ms-word.document.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-word.document.macroenabled.12");
            }
            if (ct.contains("ms-word.template.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-word.template.macroenabled.12");
            }
            if (ct.contains("wordprocessingml.template")) {
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
            }
            if (ct.contains("wordprocessingml.document")) {
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }

            if (ct.contains("ms-excel.sheet.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-excel.sheet.macroenabled.12");
            }
            if (ct.contains("ms-excel.template.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-excel.template.macroenabled.12");
            }
            if (ct.contains("ms-excel.addin.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-excel.addin.macroenabled.12");
            }
            if (ct.contains("spreadsheetml.template")) {
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
            }
            if (ct.contains("spreadsheetml.sheet")) {
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }

            if (ct.contains("ms-powerpoint.presentation.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-powerpoint.presentation.macroenabled.12");
            }
            if (ct.contains("ms-powerpoint.template.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-powerpoint.template.macroenabled.12");
            }
            if (ct.contains("ms-powerpoint.slideshow.macroenabled.12")) {
                return MediaType.parse(
                        "application/vnd.ms-powerpoint.slideshow.macroenabled.12");
            }
            if (ct.contains("presentationml.template")) {
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.presentationml.template");
            }
            if (ct.contains("presentationml.slideshow")) {
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
            }
            if (ct.contains("presentationml.presentation")) {
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            }
        }
        if (hasWord) {
            return MediaType.parse(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        if (hasExcel) {
            return MediaType.parse(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        if (hasPowerPoint) {
            return MediaType.parse(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        }
        return null;
    }

    private static String extractExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        String cleanName = StringUtils.substringBefore(fileName, "?");
        cleanName = StringUtils.substringBefore(cleanName, "#");
        Matcher matcher = EXTENSION_PATTERN.matcher(cleanName);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1).toLowerCase();
    }

    private static MediaType normalizeOfficeMimeByExtension(
            MediaType media, String extension) {
        if (media == null || extension == null) {
            return media;
        }
        if (".qpw".equals(extension)) {
            return MediaType.parse("application/x-quattro-pro; version=9");
        }
        String mediaString = media.toString();
        if (!"application/x-tika-msoffice".equals(mediaString)
                && !"application/x-tika-ooxml".equals(mediaString)
                && !"application/zip".equals(mediaString)
                && !MediaType.OCTET_STREAM.toString().equals(mediaString)) {
            return media;
        }
        switch (extension) {
            case ".doc":
            case ".dot":
                return MediaType.parse("application/msword");
            case ".xls":
            case ".xla":
            case ".xlm":
            case ".xlt":
                return MediaType.parse("application/vnd.ms-excel");
            case ".pps":
                return MediaType.parse("application/vnd.ms-powerpoint");
            case ".vsd":
            case ".vss":
            case ".vst":
            case ".vdx":
            case ".vsx":
            case ".vtx":
                return MediaType.parse("application/vnd.visio");
            case ".qpw":
                return MediaType.parse("application/x-quattro-pro; version=9");
            case ".docx":
            case ".dotx":
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case ".xlsx":
            case ".xltx":
            case ".xlsm":
            case ".xltm":
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case ".pptx":
            case ".potx":
            case ".ppsx":
            case ".pptm":
            case ".potm":
            case ".ppsm":
                return MediaType.parse(
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            default:
                return media;
        }
    }
}
