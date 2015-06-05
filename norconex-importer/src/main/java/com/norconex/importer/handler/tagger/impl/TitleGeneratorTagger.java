/* Copyright 2015 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.io.TextReader;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractStringTagger;

/**
 * <p>Attempts to generate a title from the document content (default) or 
 * a specified metadata field. It does not consider a document format
 * to give value to more terms than other. For instance, it would not
 * consider text found in &lt;H1&gt; tags more importantly than other
 * text in HTML documents.</p>
 * 
 * <p>If {@link #detectHeading} is set to <code>true</code>, this handler 
 * will check if the content starts with a stand-alone, single-sentence line.  
 * That is, a line of text with only one sentence in it, followed by one or 
 * more new line characters. To help
 * eliminate cases where such sentence are inappropriate, you can specify a
 * minimum and maximum number of characters that first line should have
 * with {@link #setDetectHeadingMinLength(int)} and 
 * {@link #setDetectHeadingMaxLength(int)} (e.g. to ignore "Page 1" text and 
 * the like).</p>
 * 
 * <p>Unless a target field name is provided, the default field name
 * where the title will be stored is <code>document.generatedTitle</code>.
 * Unless, {@link #setOverwrite(boolean)} is set to <code>true</code>, 
 * no title will be generated if one already exists in the target field.</p>
 * 
 * <p>If it cannot generate a title, it will fallback to retrieving the 
 * first sentence from the text, up to a maximum number of characters
 * set by {@link #setFallbackMaxLength(int)} (default is 150).  Specifying
 * a zero or negative fallback maximum length will prevent fallback titles
 * from being used.</p>
 * 
 * <p>This class currently supports 19 languages: Arabic (experimental),
 * Chinese Simplified (experimental), Danish, Dutch, English, Finnish, 
 * French, German, Hungarian, Italian, Korean, Norwegian, Polish, Portuguese,
 * Romanian, Russian, Spanish, Swedish, Turkish</p>
 * 
 * <p>This class should be used as a post-parsing handler only
 * (on unformatted text).</p>
 * 
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TitleGeneratorTagger"
 *          fromField="(field of text to use/default uses document content)" 
 *          toField="(target field where to store generated title)"
 *          overwrite="[false|true]" 
 *          fallbackMaxLength="(max num of chars for fallback title)"
 *          detectHeading="[false|true]"
 *          detectHeadingMinLength="(min length a heading title can have)"
 *          detectHeadingMaxLength="(max length a heading title can have)"
 *          maxReadSize="(max characters to read at once)" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.1.0
 */
public class TitleGeneratorTagger 
        extends AbstractStringTagger implements IXMLConfigurable {


    private static final Logger LOG = 
            LogManager.getLogger(TitleGeneratorTagger.class);
    
    public static final String DEFAULT_TO_FIELD = 
            ImporterMetadata.DOC_GENERATED_TITLE;
    private static final int DEFAULT_FALLBACK_MAX_LENGTH = 150;
    private static final int DEFAULT_HEADING_MIN_LENGTH = 10;
    private static final int DEFAULT_HEADING_MAX_LENGTH = 150;
    
    private static final int MIN_CONTENT_LENGTH = 1000;
    private static final int MIN_SECTION_MAX_BREAK_SIZE = 200;
    private static final int NUM_SECTIONS = 25;
    private static final int MIN_ACCEPTABLE_SCORE = 1;
    private static final Pattern PATTERN_HEADING = Pattern.compile(
            "^.*?([^\\n\\r]+)[\\n\\r]", Pattern.DOTALL);

    
    private final Controller controller = ControllerFactory.createSimple();
    
    private String fromField;
    private String toField = DEFAULT_TO_FIELD;
    private boolean overwrite;
    private int fallbackMaxLength = DEFAULT_FALLBACK_MAX_LENGTH;
    private boolean detectHeading;
    private int detectHeadingMinLength = DEFAULT_HEADING_MIN_LENGTH;
    private int detectHeadingMaxLength = DEFAULT_HEADING_MAX_LENGTH;

    
    @Override
    protected void tagStringContent(String reference, StringBuilder content,
            ImporterMetadata metadata, boolean parsed, int sectionIndex)
                    throws ImporterHandlerException {

        //TODO check if partial content and !first run(?) do not add title again
        
        // If title already exists and not overwriting, leave now
        if (overwrite && StringUtils.isNotBlank(
                metadata.getString(getTargetField()))) {
            return;
        }
        
        // Get the text to evaluate
        String text = null;
        if (StringUtils.isNotBlank(fromField)) {
            text = metadata.getString(fromField);
        } else {
            text = content.toString();
        }
        
        String title = null;

        // Try detecting if there is a text heading
        if (isDetectHeading()) {
            title = getHeadingTitle(text);
            if (StringUtils.isNotBlank(title)) {
                metadata.setString(getTargetField(), title);
                return;
            }
        }
        
        // If text is too small to extract a title, get fallback title.
        if (text.length() < MIN_CONTENT_LENGTH && fallbackMaxLength > 0) {
            metadata.setString(
                    getTargetField(), getFallbackTitle(text));
            return;
        }


        // Try from Carrot
        try {
            title = getCarrotTitle(text);
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot generate title for " + reference, e);
        }

        // No title, use fallback
        if (StringUtils.isBlank(title) && fallbackMaxLength > 0) {
            title = getFallbackTitle(text);
        }
        metadata.setString(getTargetField(), title);
    }

    public String getToField() {
        return toField;
    }
    public void setToField(String toField) {
        this.toField = toField;
    }

    public boolean isOverwrite() {
        return overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
    
    public String getFromField() {
        return fromField;
    }
    public void setFromField(String fromField) {
        this.fromField = fromField;
    }
    
    public int getFallbackMaxLength() {
        return fallbackMaxLength;
    }
    public void setFallbackMaxLength(int fallbackMaxLength) {
        this.fallbackMaxLength = fallbackMaxLength;
    }
    
    public boolean isDetectHeading() {
        return detectHeading;
    }
    public void setDetectHeading(boolean detectHeading) {
        this.detectHeading = detectHeading;
    }

    public int getDetectHeadingMinLength() {
        return detectHeadingMinLength;
    }
    public void setDetectHeadingMinLength(int detectHeadingMinLength) {
        this.detectHeadingMinLength = detectHeadingMinLength;
    }

    public int getDetectHeadingMaxLength() {
        return detectHeadingMaxLength;
    }
    public void setDetectHeadingMaxLength(int detectHeadingMaxLength) {
        this.detectHeadingMaxLength = detectHeadingMaxLength;
    }

    private String getTargetField() {
        if (StringUtils.isBlank(toField)) {
            return DEFAULT_TO_FIELD;
        }
        return toField;
    }

    private String getHeadingTitle(String text) {
        String firstLine = null;
        Matcher m = PATTERN_HEADING.matcher(text);
        if (m.find()) {
            firstLine = StringUtils.trim(m.group());
        }
        if (StringUtils.isBlank(firstLine)) {
            return null;
        }
        
        // if more than one sentence, ignore
        if (StringUtils.split(firstLine, "?!.").length != 1) {
            return null;
        }
        // must match min/max lengths.
        if (firstLine.length() < detectHeadingMinLength
                || firstLine.length() > detectHeadingMaxLength) {
            return null;
        }
        return firstLine;
    }
    
    private String getCarrotTitle(String text) throws IOException {
        // Use Carrot2 to try get an OK title.
        int maxSectionSize = Math.max(MIN_SECTION_MAX_BREAK_SIZE, 
                text.length() / NUM_SECTIONS);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Content-length: " + text.length()
                    + "; maxSectionSize: " + maxSectionSize);
        }
        
        List<Document> sections = new ArrayList<Document>();
        try (TextReader reader = new TextReader(new StringReader(
                text), maxSectionSize)) {
            String section = null;
            while((section = reader.readText()) != null) {
                sections.add(new Document(section));
            }
        }
        
        final ProcessingResult byDomainClusters = controller.process(
                sections, null, STCClusteringAlgorithm.class);
        final List<Cluster> clusters = byDomainClusters.getClusters();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Title generator clusters= " + clusters);
        }
        TreeMap<Double, String> titles = new TreeMap<>();
        for (Cluster cluster : clusters) {
            titles.put(cluster.getScore(), cluster.getLabel());
            if (LOG.isDebugEnabled()) {
                LOG.info(cluster.getScore() + "=" + cluster.getLabel());            
            }
        }
        Entry<Double, String> titleEntry = titles.lastEntry();
        if (titleEntry == null || titleEntry.getKey() < MIN_ACCEPTABLE_SCORE) {
            return null;
        }
        return titleEntry.getValue();
    }
    
    private String getFallbackTitle(String content) {
        String title = content;
        
        String[] sentences = StringUtils.split(title, "?!.\n\r");
        if (sentences.length != 0) {
            title = sentences[0];
        }
        if (title.length() > fallbackMaxLength) {
            title = title.substring(0, fallbackMaxLength) + "...";
        }
        return title;
    }
    
    @Override
    protected void loadStringTaggerFromXML(XMLConfiguration xml)
            throws IOException {
        setFromField(xml.getString("[@fromField]", getFromField()));
        setToField(xml.getString("[@toField]", getToField()));
        setOverwrite(xml.getBoolean("[@overwrite]", isOverwrite()));
        setFallbackMaxLength(xml.getInt(
                "[@fallbackMaxLength]", getFallbackMaxLength()));
        setDetectHeading(xml.getBoolean("[@detectHeading]", isDetectHeading()));
        setDetectHeadingMinLength(xml.getInt(
                "[@detectHeadingMinLength]", getDetectHeadingMinLength()));
        setDetectHeadingMaxLength(xml.getInt(
                "[@detectHeadingMaxLength]", getDetectHeadingMaxLength()));
    }
    
    @Override
    protected void saveStringTaggerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("fromField", getFromField());
        writer.writeAttributeString("toField", getToField());
        writer.writeAttributeBoolean("overwrite", isOverwrite());
        writer.writeAttributeInteger(
                "fallbackMaxLength", getFallbackMaxLength());
        writer.writeAttributeBoolean("detectHeading", isDetectHeading());
        writer.writeAttributeInteger(
                "detectHeadingMinLength", getDetectHeadingMinLength());
        writer.writeAttributeInteger(
                "detectHeadingMaxLength", getDetectHeadingMaxLength());
    }


    
}
