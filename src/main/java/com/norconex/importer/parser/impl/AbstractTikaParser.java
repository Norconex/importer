/* Copyright 2010-2020 Norconex Inc.
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
package com.norconex.importer.parser.impl;

import static com.norconex.importer.doc.DocMetadata.EMBEDDED_REFERENCE;
import static com.norconex.importer.doc.DocMetadata.EMBEDDED_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.google.common.base.Objects;
import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.doc.DocInfo;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.GrobidConfig;
import com.norconex.importer.parser.IHintsAwareParser;
import com.norconex.importer.parser.OCRConfig;
import com.norconex.importer.parser.ParseHints;

/**
 * Base class wrapping Apache Tika parser for use by the importer.
 * 
 * @author Pascal Essiembre
 */
public class AbstractTikaParser implements IHintsAwareParser {

    public enum MetadataFieldPolicy {
        PRESERVE,
        LEGACY,
        BOTH
    }

    private static final String GROBID_REST_PARSER =
            "org.apache.tika.parser.journal.GrobidRESTParser";
    private static final String JOURNAL_PARSER =
            "org.apache.tika.parser.journal.JournalParser";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTikaParser.class);

    private final Parser parser;
    private TesseractOCRConfig ocrTesseractConfig;
    private ParseHints parseHints;
    private final ThreadSafeCacheableAutoDetectWrapper knownDetector;
    private MetadataFieldPolicy metadataFieldPolicy = MetadataFieldPolicy.BOTH;

    /**
     * Creates a new Tika-based parser.
     * 
     * @param parser Tika parser
     */
    public AbstractTikaParser(Parser parser) {
        super();
        this.parser = parser;
        if (parser instanceof AutoDetectParser) {
            AutoDetectParser p = (AutoDetectParser) parser;
            knownDetector = new ThreadSafeCacheableAutoDetectWrapper(p.getDetector());
            p.setDetector(knownDetector);
        } else {
            knownDetector = null;
        }
    }

    @Override
    public void initialize(ParseHints parserHints) {
        this.parseHints = parserHints;
        if (parseHints == null) {
            this.parseHints = new ParseHints();
        } else {
            this.ocrTesseractConfig = toTesseractConfig(parseHints.getOcrConfig());
        }
        applyGrobidConfig();
    }

    /**
     * Applies the Grobid configuration. When Grobid is disabled (the default),
     * {@code JournalParser} (which wraps {@code GrobidRESTParser} internally)
     * is removed from the {@code AutoDetectParser}'s internal parser list to
     * prevent network calls to a Grobid service that is not running.
     * When enabled, the configured service URL is propagated via a system
     * property that the Grobid client reads.
     * <p>
     * Implementation note: {@code CompositeParser} stores parsers internally as
     * a {@code List<Parser>}. The public {@code getParsers()} method returns a
     * dynamically-built {@code Map} snapshot where last-writer-wins for duplicate
     * MIME types, so using {@code getParsers()} / {@code setParsers(Map)} would
     * silently drop {@code PDFParser} when both it and {@code JournalParser}
     * map to {@code application/pdf}. To avoid that, this method reaches into
     * the {@code parsers} field directly via reflection.
     */
    private void applyGrobidConfig() {
        if (!(parser instanceof AutoDetectParser)) {
            return;
        }
        GrobidConfig grobidCfg = parseHints.getGrobidConfig();

        if (!grobidCfg.isEnabled()) {
            boolean removed = removeGrobidParsersFromList((CompositeParser) parser);
            if (removed) {
                LOG.debug("Grobid parsing is disabled. "
                        + "JournalParser has been removed from the Tika parser chain. "
                        + "Configure GrobidConfig with enabled=true to activate it.");
            }
        } else {
            // Propagate the configured service URL via system property.
            String serviceUrl = grobidCfg.getServiceUrl();
            if (StringUtils.isNotBlank(serviceUrl)) {
                System.setProperty("grobid.server.url", serviceUrl);
            }
            LOG.info("Grobid parsing is enabled (service URL: {}).", serviceUrl);
        }
    }

    /**
     * Walks the internal {@code parsers} list of the given
     * {@link CompositeParser} (and any nested {@code CompositeParser}s)
     * via reflection and removes all Grobid-backed parsers in place.
     *
     * @return {@code true} if at least one parser was removed
     */
    @SuppressWarnings("unchecked")
    private boolean removeGrobidParsersFromList(CompositeParser cp) {
        try {
            java.lang.reflect.Field field =
                    CompositeParser.class.getDeclaredField("parsers");
            field.setAccessible(true);
            List<Parser> list = (List<Parser>) field.get(cp);
            // The list itself may be unmodifiable; copy it first.
            List<Parser> mutable = new java.util.ArrayList<>(list);
            boolean removed = mutable.removeIf(p -> {
                // Unwrap ParserDecorator to get at the real class name.
                Parser unwrapped = unwrapDecorator(p);
                if (isGrobidParser(unwrapped)) {
                    return true;
                }
                // Recurse into nested CompositeParser entries.
                if (unwrapped instanceof CompositeParser) {
                    removeGrobidParsersFromList((CompositeParser) unwrapped);
                }
                return false;
            });
            if (removed) {
                field.set(cp, mutable);
            }
            return removed;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOG.warn("Could not disable Grobid parsers via reflection; "
                    + "Grobid-related warnings may appear.", e);
            return false;
        }
    }

    /**
     * Unwraps nested {@link ParserDecorator} layers to expose the underlying
     * parser class.
     */
    private static Parser unwrapDecorator(Parser p) {
        while (p instanceof ParserDecorator) {
            p = ((ParserDecorator) p).getWrappedParser();
        }
        return p;
    }

    /**
     * Returns {@code true} if the given parser is a Grobid-backed parser
     * that requires an external Grobid REST service.
     */
    private boolean isGrobidParser(Parser p) {
        if (p == null) {
            return false;
        }
        String name = p.getClass().getName();
        return GROBID_REST_PARSER.equals(name) || JOURNAL_PARSER.equals(name);
    }

    @Override
    public final List<Doc> parseDocument(
            Doc doc, Writer output)
            throws DocumentParserException {

        Metadata tikaMetadata = new Metadata();
        if (doc.getDocInfo().getContentType() == null) {
            throw new DocumentParserException(
                    "Doc must have a content-type.");
        }
        String contentType = doc.getDocInfo().getContentType().toString();
        // TODO getContent() here does a rewind just to get stream
        // which may be an unnecessary read. Have stream factory
        // directly on document instead to save a read?
        CachedInputStream content = doc.getInputStream();

        tikaMetadata.set(Metadata.CONTENT_TYPE, contentType);
        tikaMetadata.set(TikaCoreProperties.RESOURCE_NAME_KEY,
                doc.getReference());
        tikaMetadata.set(Metadata.CONTENT_ENCODING,
                doc.getDocInfo().getContentEncoding());
        tikaMetadata.set(Metadata.CONTENT_LENGTH,
                Long.toString(content.length()));

        RecursiveParser recursiveParser = null;
        try {
            if (knownDetector != null) {
                knownDetector.initCache(doc.getReference(), contentType);
            }
            ParseContext context = new ParseContext();

            // Create recursive parser (MergeEmbeddedParser or SplitEmbeddedParser)
            recursiveParser = createRecursiveParser(
                    doc.getReference(), contentType, output,
                    doc.getMetadata(), content.getStreamFactory());
            context.set(Parser.class, recursiveParser);

            PDFParserConfig pdfConfig = new PDFParserConfig();
            OCRConfig ocrConfig = parseHints.getOcrConfig();
            if (!ocrConfig.isEmpty()
                    && StringUtils.isNotBlank(ocrConfig.getPath())
                    && (StringUtils.isBlank(ocrConfig.getContentTypes())
                            || contentType.matches(ocrConfig.getContentTypes()))) {
                TesseractOCRParser tesseractParser = new TesseractOCRParser();
                tesseractParser.setTesseractPath(ocrConfig.getPath());
                context.set(TesseractOCRParser.class, tesseractParser);
                if (ocrTesseractConfig != null) {
                    context.set(TesseractOCRConfig.class,
                            ocrTesseractConfig);
                }
                pdfConfig.setExtractInlineImages(true);
            } else {
                pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
            }
            pdfConfig.setSuppressDuplicateOverlappingText(true);
            context.set(PDFParserConfig.class, pdfConfig);
            modifyParseContext(context);

            // Parse using recursive parser
            recursiveParser.parse(content, new BodyContentHandler(output),
                    tikaMetadata, context);

        } catch (ZeroByteFileException e) {
            LOG.warn("Document has no content: " + doc.getReference());
        } catch (Exception e) {
            throw new DocumentParserException(
                    "Could not parse document: " + doc.getReference(), e);
        }

        return recursiveParser != null
                ? recursiveParser.getEmbeddedDocuments()
                : new ArrayList<>();
    }

    /**
     * Override to apply your own settings on the Tika ParseContext.
     * The ParseContext is already configured before calling this method.
     * Changing existing settings may cause failure.
     * Only override if you know what you are doing.
     * The default implementation does nothing.
     * 
     * @param parseContext Tika parse context
     */
    protected void modifyParseContext(ParseContext parseContext) {
    }

    /**
     * Transfers metadata from Tika to Importer.
     * The behavior for Dublin Core (dc:) prefixed field names is controlled by
     * {@link #setMetadataFieldPolicy(MetadataFieldPolicy)}.
     * 
     * @param tikaMeta Tika metadata
     * @param metadata Importer metadata
     */
    protected void addTikaMetadataToImporterMetadata(
            Metadata tikaMeta, Properties metadata) {
        String[] names = tikaMeta.names();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (TikaCoreProperties.RESOURCE_NAME_KEY.equals(name)) {
                continue;
            }

            boolean isDcField = name.startsWith("dc:");
            String legacyFieldName = isDcField ? name.substring(3) : name;

            List<String> targetFieldNames = new ArrayList<>();
            if (!isDcField || metadataFieldPolicy == MetadataFieldPolicy.PRESERVE) {
                targetFieldNames.add(name);
            } else if (metadataFieldPolicy == MetadataFieldPolicy.LEGACY) {
                targetFieldNames.add(legacyFieldName);
            } else {
                targetFieldNames.add(name);
                if (!legacyFieldName.equals(name)) {
                    targetFieldNames.add(legacyFieldName);
                }
            }

            String[] tikaValues = tikaMeta.getValues(name);
            for (String fieldName : targetFieldNames) {
                List<String> nxValues = metadata.getStrings(fieldName);
                for (String tikaValue : tikaValues) {
                    if (!containsSameValue(fieldName, nxValues, tikaValue)) {
                        metadata.add(fieldName, tikaValue);
                    } else {
                        metadata.set(fieldName, tikaValue);
                    }
                }
            }
        }
    }

    public MetadataFieldPolicy getMetadataFieldPolicy() {
        return metadataFieldPolicy;
    }

    public void setMetadataFieldPolicy(MetadataFieldPolicy metadataFieldPolicy) {
        if (metadataFieldPolicy == null) {
            this.metadataFieldPolicy = MetadataFieldPolicy.BOTH;
            return;
        }
        this.metadataFieldPolicy = metadataFieldPolicy;
    }

    public void setMetadataFieldPolicy(String metadataFieldPolicy) {
        if (StringUtils.isBlank(metadataFieldPolicy)) {
            this.metadataFieldPolicy = MetadataFieldPolicy.BOTH;
            return;
        }
        this.metadataFieldPolicy = MetadataFieldPolicy.valueOf(
                metadataFieldPolicy.trim().toUpperCase(Locale.ROOT));
    }

    private boolean containsSameValue(
            String name, List<String> nxValues, String tikaValue) {
        if (EqualsUtil.equalsAnyIgnoreCase(
                name, Metadata.CONTENT_TYPE, Metadata.CONTENT_ENCODING)) {
            String tk = tikaValue.replaceAll("[\\s]", "");
            for (String nxValue : nxValues) {
                if (nxValue.replaceAll("[\\s]", "").equalsIgnoreCase(tk)) {
                    return true;
                }
            }
            return false;
        }
        return nxValues.contains(tikaValue);
    }

    protected RecursiveParser createRecursiveParser(
            String reference, String contentType, Writer writer,
            Properties metadata, CachedStreamFactory streamFactory) {
        String splitRegex = parseHints.getEmbeddedConfig().getSplitContentTypes();
        if (StringUtils.isNotBlank(splitRegex)
                && contentType.matches(splitRegex)) {
            return new SplitEmbbededParser(
                    reference, this.parser, metadata, streamFactory);
        }
        return new MergeEmbeddedParser(this.parser, writer, metadata);
    }

    private TesseractOCRConfig toTesseractConfig(OCRConfig ocrConfig) {
        if (ocrConfig == null) {
            return null;
        }
        TesseractOCRConfig tc = new TesseractOCRConfig();

        String langs = ocrConfig.getLanguages();
        if (StringUtils.isNotBlank(langs)) {
            langs = StringUtils.remove(langs, ' ');
            langs = langs.replace(',', '+');
            tc.setLanguage(langs);
        }
        return tc;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AbstractTikaParser)) {
            return false;
        }
        AbstractTikaParser castOther = (AbstractTikaParser) other;
        Class<?> thisParserClass = null;
        Class<?> otherParserClass = null;
        if (parser != null) {
            thisParserClass = parser.getClass();
        }
        if (castOther.parser != null) {
            otherParserClass = castOther.parser.getClass();
        }
        return new EqualsBuilder()
                .append(thisParserClass, otherParserClass)
                .append(parseHints, castOther.parseHints)
                .isEquals();
    }

    @Override
    public int hashCode() {
        Class<?> thisParserClass = null;
        if (parser != null) {
            thisParserClass = parser.getClass();
        }
        return new HashCodeBuilder()
                .append(thisParserClass)
                .append(parseHints)
                .toHashCode();
    }

    @Override
    public String toString() {
        String thisParserClass = null;
        if (parser != null) {
            thisParserClass = parser.getClass().getSimpleName();
        }
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("tika", thisParserClass)
                .toString();
    }

    protected class SplitEmbbededParser
            extends ParserDecorator implements RecursiveParser {
        private static final long serialVersionUID = -5011890258694908887L;
        private final String reference;
        private final Properties metadata;
        private final CachedStreamFactory streamFactory;
        private boolean isMasterDoc = true;
        private String masterType;
        private int embedCount;
        private List<Doc> embeddedDocs;

        public SplitEmbbededParser(String reference, Parser parser,
                Properties metadata, CachedStreamFactory streamFactory) {
            super(parser);
            this.streamFactory = streamFactory;
            this.reference = reference;
            this.metadata = metadata;
        }

        @Override
        public void parse(InputStream stream, ContentHandler handler,
                Metadata tikaMeta, ParseContext context)
                throws IOException, SAXException, TikaException {

            if (isMasterDoc) {
                isMasterDoc = false;
                if (hasNoExtractCondition()) {
                    masterType = knownDetector.detect(stream, tikaMeta).toString();
                }
                super.parse(stream, handler, tikaMeta, context);
                addTikaMetadataToImporterMetadata(tikaMeta, metadata);
            } else {

                boolean hasNoExtractFilter = hasNoExtractCondition();
                if (hasNoExtractFilter) {
                    String currentType = knownDetector.detect(stream, tikaMeta).toString();
                    if (!performExtract(masterType, currentType)) {
                        // do not extract this embedded doc
                        return;
                    }
                }

                embedCount++;
                if (embeddedDocs == null) {
                    embeddedDocs = new ArrayList<>();
                }

                Properties embedMeta = new Properties();
                addTikaMetadataToImporterMetadata(tikaMeta, embedMeta);

                DocInfo embedDocInfo = resolveEmbeddedResourceName(
                        tikaMeta, embedMeta, embedCount);

                // Read the steam into cache for reuse since Tika will
                // close the original stream on us causing exceptions later.
                CachedOutputStream embedOutput = streamFactory.newOuputStream();
                IOUtils.copy(stream, embedOutput);
                CachedInputStream embedInput = embedOutput.getInputStream();
                embedOutput.close();

                embedDocInfo.addEmbeddedParentReference(reference);
                Doc embedDoc = new Doc(embedDocInfo, embedInput, embedMeta);
                // embedMeta.setReference(embedRef);
                // embedMeta.setEmbeddedParentReference(reference);

                // String rootRef = metadata.getEmbeddedParentRootReference();
                // if (StringUtils.isBlank(rootRef)) {
                // rootRef = reference;
                // }
                // embedMeta.setEmbeddedParentRootReference(rootRef);

                embeddedDocs.add(embedDoc);
            }
        }

        @Override
        public List<Doc> getEmbeddedDocuments() {
            return embeddedDocs;
        }

        private DocInfo resolveEmbeddedResourceName(
                Metadata tikaMeta, Properties embedMeta, int embedCount) {

            DocInfo docInfo = new DocInfo();

            String name = null;

            // Package item file name (e.g. a file in a zip)
            name = tikaMeta.get(TikaCoreProperties.EMBEDDED_RELATIONSHIP_ID);
            if (StringUtils.isNotBlank(name)) {
                docInfo.setReference(reference + "!" + name);
                embedMeta.set(EMBEDDED_REFERENCE, name);
                embedMeta.set(EMBEDDED_TYPE, "package-file");

                // docInfo.setEmbeddedReference(name);
                // docInfo.setEmbeddedType("package-file");
                // embedMeta.setEmbeddedReference(name);
                // embedMeta.setEmbeddedType("package-file");
                // return name;
                return docInfo;
            }

            // Name of Embedded file in regular document
            // (e.g. excel file in a word doc)
            name = tikaMeta.get(TikaCoreProperties.RESOURCE_NAME_KEY);
            if (StringUtils.isNotBlank(name)) {
                docInfo.setReference(reference + "!" + name);

                embedMeta.set(EMBEDDED_REFERENCE, name);
                embedMeta.set(EMBEDDED_TYPE, "file-file");

                // docInfo.setEmbeddedReference(name);
                // docInfo.setEmbeddedType("package-file");
                // embedMeta.setEmbeddedReference(name);
                // embedMeta.setEmbeddedType("file-file");
                // return name;
                return docInfo;
            }

            // Name of embedded content in regular document
            // (e.g. image with no name in a word doc)
            // Make one up with content type
            // (which should be OK most of the time).
            name = tikaMeta.get(Metadata.CONTENT_TYPE);
            if (StringUtils.isNotBlank(name)) {
                ContentType ct = ContentType.valueOf(name);
                if (ct != null) {
                    String embedRef = "embedded-" + embedCount + "." + ct.getExtension();

                    docInfo.setReference(reference + "!" + embedRef);

                    embedMeta.set(EMBEDDED_REFERENCE, embedRef);
                    embedMeta.set(EMBEDDED_TYPE, "file-file");

                    // docInfo.setEmbeddedType("file-object");
                    // embedMeta.setEmbeddedType("file-object");
                    // return "embedded-" + embedCount + "." + ct.getExtension();
                    return docInfo;
                }
            }

            // Default... we could not find any name so make a unique one.
            // embedMeta.setEmbeddedType("unknown");

            String embedRef = "embedded-" + embedCount + ".unknown";

            embedMeta.set(EMBEDDED_REFERENCE, embedRef);
            embedMeta.set(EMBEDDED_TYPE, "unknown");

            // docInfo.setEmbeddedType("unknown");
            docInfo.setReference(reference + "!" + embedRef);
            // + "embedded-" + embedCount + ".unknown");
            return docInfo;
        }
    }

    protected class MergeEmbeddedParser
            extends ParserDecorator implements RecursiveParser {
        private static final long serialVersionUID = -5011890258694908887L;
        private final Writer writer;
        private final Properties metadata;

        private final LinkedList<String> hierarchy = new LinkedList<>();

        public MergeEmbeddedParser(Parser parser,
                Writer writer, Properties metadata) {
            super(parser);
            this.writer = writer;
            this.metadata = metadata;
        }

        @Override
        public void parse(InputStream stream, ContentHandler handler,
                Metadata tikaMeta, ParseContext context)
                throws IOException, SAXException, TikaException {
            boolean performExtract = true;
            boolean hasNoExtractFilter = hasNoExtractCondition();
            if (hasNoExtractFilter) {
                String parentType = hierarchy.peekLast();
                String currentType = knownDetector.detect(stream, tikaMeta).toString();
                hierarchy.add(currentType);
                performExtract = performExtract(parentType, currentType);
            }
            if (performExtract) {
                super.parse(stream,
                        new BodyContentHandler(writer), tikaMeta, context);
                addTikaMetadataToImporterMetadata(tikaMeta, metadata);
            }
            if (hasNoExtractFilter) {
                hierarchy.pollLast();
            }
        }

        @Override
        public List<Doc> getEmbeddedDocuments() {
            return null;
        }
    }

    protected interface RecursiveParser extends Parser {
        List<Doc> getEmbeddedDocuments();
    }

    private boolean hasNoExtractCondition() {
        if (parseHints == null) {
            return false;
        }
        if (StringUtils.isNotBlank(getNoExtractContainerRegex())) {
            return true;
        }
        if (StringUtils.isNotBlank(getNoExtractEmbeddedRegex())) {
            return true;
        }
        return false;
    }

    private String getNoExtractContainerRegex() {
        if (parseHints == null) {
            return null;
        }
        return parseHints.getEmbeddedConfig()
                .getNoExtractContainerContentTypes();
    }

    private String getNoExtractEmbeddedRegex() {
        if (parseHints == null) {
            return null;
        }
        return parseHints.getEmbeddedConfig()
                .getNoExtractEmbeddedContentTypes();
    }

    private boolean performExtract(String parentType, String currentType) {
        if (parseHints == null || parentType == null) {
            return true;
        }

        // --- Container ---
        String containerRegex = getNoExtractContainerRegex();
        if (StringUtils.isNotBlank(containerRegex)
                && parentType.matches(containerRegex)) {
            return false;
        }

        // --- Embedded ---
        String embeddedRegex = getNoExtractEmbeddedRegex();
        return StringUtils.isBlank(embeddedRegex)
                || !currentType.matches(embeddedRegex);
    }

    // TODO create in a separate class, and make it that it caches
    // embedded detections as well, with the cash reset upon setting
    // a new thread-safe reference via a new cacheReference(ref, contentType).
    // Class name:

    // Then use this to autodetect before actual parsing to skip embedded
    // if configured to do so.

    /**
     * This class prevents detecting the content type a second time when
     * we already know what it is. Only the content type for the root
     * reference is not detected again. Any embedded documents will have
     * default detection behavior applied on them.
     */
    class ThreadSafeCacheableAutoDetectWrapper implements Detector {
        private static final long serialVersionUID = 225979407457365951L;
        private final Detector originalDetector;
        private final ThreadLocal<Cache> threadCache = new ThreadLocal<>();

        public ThreadSafeCacheableAutoDetectWrapper(Detector originalDetector) {
            super();
            this.originalDetector = originalDetector;
        }

        void initCache(String reference, String contentType) {
            Cache cache = new Cache();
            cache.rootReference = reference;
            cache.currentReference = reference;
            if (StringUtils.isNotBlank(contentType)) {
                cache.currentType = new MediaType(
                        StringUtils.substringBefore(contentType, "/"),
                        StringUtils.substringAfter(contentType, "/"));
            }
            threadCache.set(cache);
        }

        @Override
        public MediaType detect(InputStream input, Metadata metadata)
                throws IOException {
            Cache cache = threadCache.get();

            String ref = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);
            String embedId = metadata.get(
                    TikaCoreProperties.EMBEDDED_RELATIONSHIP_ID);
            if (StringUtils.isNotBlank(embedId)) {
                ref = cache.rootReference + "__EMB__" + ref;
            }
            if (StringUtils.isBlank(ref)) {
                ref = cache.rootReference + "__hash__" + input.hashCode();
            }

            if (cache.currentType == null
                    || !Objects.equal(cache.currentReference, ref)) {
                cache.currentReference = ref;
                cache.currentType = originalDetector.detect(input, metadata);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Caching new media type : "
                            + ref + " => " + cache.currentType);
                }
            } else if (LOG.isTraceEnabled()) {
                LOG.trace("Using cached media type: "
                        + ref + " => " + cache.currentType);
            }
            return cache.currentType;
        }

        class Cache {
            private String rootReference;
            private String currentReference;
            private MediaType currentType;
        }
    }

    // --- Deprecated methods ---------------------------------------------------
    /**
     * Sets the OCR configuration.
     * 
     * @param ocrConfig the ocrConfig to set
     * @since 2.1.0
     * @deprecated Use {@link #initialize(ParseHints)}
     */
    @Deprecated
    public synchronized void setOCRConfig(OCRConfig ocrConfig) {
        if (parseHints == null) {
            parseHints = new ParseHints();
        }
        OCRConfig ocr = parseHints.getOcrConfig();
        ocr.setContentTypes(ocrConfig.getContentTypes());
        ocr.setLanguages(ocrConfig.getLanguages());
        ocr.setPath(ocrConfig.getPath());
        initialize(parseHints);
    }

    /**
     * Gets the OCR configuration (never null).
     * 
     * @return the OCR configuration
     * @since 2.1.0
     * @deprecated Use {@link #initialize(ParseHints)}
     */
    @Deprecated
    public OCRConfig getOCRConfig() {
        if (parseHints == null) {
            return null;
        }
        return parseHints.getOcrConfig();
    }

    /**
     * Gets whether embedded documents should be split to become "standalone"
     * distinct documents.
     * 
     * @return <code>true</code> if parser should split embedded documents.
     * @deprecated Use {@link #initialize(ParseHints)}
     */
    @Deprecated
    public boolean isSplitEmbedded() {
        if (parseHints == null) {
            return false;
        }
        return StringUtils.isNotBlank(
                parseHints.getEmbeddedConfig().getSplitContentTypes());
    }

    /**
     * Sets whether embedded documents should be split to become "standalone"
     * distinct documents.
     * 
     * @param splitEmbedded <code>true</code> if parser should split
     *                      embedded documents.
     * @deprecated Use {@link #initialize(ParseHints)}
     */
    @Deprecated
    public void setSplitEmbedded(boolean splitEmbedded) {
        if (parseHints == null) {
            parseHints = new ParseHints();
        }
        if (splitEmbedded) {
            parseHints.getEmbeddedConfig().setSplitContentTypes(".*");
        } else {
            parseHints.getEmbeddedConfig().setSplitContentTypes(null);
        }
        initialize(parseHints);
    }
}