/* Copyright 2014-2017 Norconex Inc.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractStringTagger;

/**
 * <p>Detects a document language based on Tika language detection capability.
 * It adds the detected language to the 
 * "<code>document.language</code>" metadata field.  
 * Optionally adds all potential languages detected with their 
 * probability score as well as additional fields following this pattern:</p>
 * <pre>
 * document.language.&lt;rank&gt;.tag
 * document.language.&lt;rank&gt;.probability</pre>
 * <p>
 * <code>&lt;rank&gt;</code> is to indicate the match order, based
 * on match probability score (starting at 1).
 * </p>
 * <p>This tagger can be used both as a pre-parse (on text only) 
 * or post-parse handler.</p>
 * 
 * <h3>Accuracy:</h3>
 * <p>
 * To obtain optimal detection, long enough text is expected.  The default
 * detection algorithm is optimized for document with lots of text.
 * This tagger relies on Tika language detection capabilities and future 
 * versions may provide better precision for documents made of short 
 * text (e.g. tweets, comments, etc).
 * </p>
 * <p>
 * If you know what mix of languages are used by your site(s), you can increase
 * accuracy in many cases by limiting the set of languages supported
 * for detection.
 * </p>
 * 
 * <h3>Supported Languages:</h3>
 * <p>
 * Languages are represented as code values. As of 2.6.0, at least the 
 * following 70 languages are supported by the Tika version used:
 * </p>
 * 
 * <ul>
 *   <li>af Afrikaans</li>
 *   <li>an Aragonese</li>
 *   <li>ar Arabic</li>
 *   <li>ast Asturian</li>
 *   <li>be Belarusian</li>
 *   <li>br Breton</li>
 *   <li>ca Catalan</li>
 *   <li>bg Bulgarian</li>
 *   <li>bn Bengali</li>
 *   <li>cs Czech</li>
 *   <li>cy Welsh</li>
 *   <li>da Danish</li>
 *   <li>de German</li>
 *   <li>el Greek</li>
 *   <li>en English</li>
 *   <li>es Spanish</li>
 *   <li>et Estonian</li>
 *   <li>eu Basque</li>
 *   <li>fa Persian</li>
 *   <li>fi Finnish</li>
 *   <li>fr French</li>
 *   <li>ga Irish</li>
 *   <li>gl Galician</li>
 *   <li>gu Gujarati</li>
 *   <li>he Hebrew</li>
 *   <li>hi Hindi</li>
 *   <li>hr Croatian</li>
 *   <li>ht Haitian</li>
 *   <li>hu Hungarian</li>
 *   <li>id Indonesian</li>
 *   <li>is Icelandic</li>
 *   <li>it Italian</li>
 *   <li>ja Japanese</li>
 *   <li>km Khmer</li>
 *   <li>kn Kannada</li>
 *   <li>ko Korean</li>
 *   <li>lt Lithuanian</li>
 *   <li>lv Latvian</li>
 *   <li>mk Macedonian</li>
 *   <li>ml Malayalam</li>
 *   <li>mr Marathi</li>
 *   <li>ms Malay</li>
 *   <li>mt Maltese</li>
 *   <li>ne Nepali</li>
 *   <li>nl Dutch</li>
 *   <li>no Norwegian</li>
 *   <li>oc Occitan</li>
 *   <li>pa Punjabi</li>
 *   <li>pl Polish</li>
 *   <li>pt Portuguese</li>
 *   <li>ro Romanian</li>
 *   <li>ru Russian</li>
 *   <li>sk Slovak</li>
 *   <li>sl Slovene</li>
 *   <li>so Somali</li>
 *   <li>sq Albanian</li>
 *   <li>sr Serbian</li>
 *   <li>sv Swedish</li>
 *   <li>sw Swahili</li>
 *   <li>ta Tamil</li>
 *   <li>te Telugu</li>
 *   <li>th Thai</li>
 *   <li>tl Tagalog</li>
 *   <li>tr Turkish</li>
 *   <li>uk Ukrainian</li>
 *   <li>ur Urdu</li>
 *   <li>vi Vietnamese</li>
 *   <li>yi Yiddish</li>
 *   <li>zh-cn Simplified Chinese</li>
 *   <li>zh-tw Traditional Chinese</li>
 * </ul>
 * 
 * <p>
 * It is possible more will be supported automatically with future Tika 
 * upgrades.
 * </p>
 * 
 * <p>If you do not restrict the list of language candidates to detect, 
 * the default behavior is to try match all languages currently supported.
 * </p>
 * 
 * <p>
 * <b>Since 2.6.0</b>, this tagger uses Tika for language detection. As a
 * result, more languages are supported, at the expense of less accuracy with
 * short text.
 * </p>
 * 
 * <h3>XML configuration usage:</h3>
 * 
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.LanguageTagger"
 *          keepProbabilities="(false|true)"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)"
 *          fallbackLanguage="(default language when detection failed)" &gt;
 *      &lt;languages&gt;
 *        (CSV list of language tag candidates. Defaults to the above list.)
 *      &lt;/languages&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class LanguageTagger extends AbstractStringTagger
        implements IXMLConfigurable {

    //TODO Check if doc.size is defined in metadata? If so, use it to 
    //determine if we are going with small or long text?
    
    //TODO provide ways to overwrite or specify custom language profiles 
    // in this tagger configuration?
    
    private static final Logger LOG = 
            LogManager.getLogger(LanguageTagger.class);
    
    private LanguageDetector detector;
    private boolean keepProbabilities;
    private String[] languages;
    private String fallbackLanguage;
    
    private final Comparator<LanguageResult> langResultComparator = 
            new Comparator<LanguageResult>() {
        @Override
        public int compare(LanguageResult o1, LanguageResult o2) {
            return Float.compare(o1.getRawScore(), o2.getRawScore());
        }
    };
    
    @Override
    protected void tagStringContent(
            String reference, StringBuilder content, 
            ImporterMetadata metadata, boolean parsed,
            int sectionIndex) throws ImporterHandlerException {
        
        // For massive docs: only use first section of document to detect langs
        if (sectionIndex > 0) {
            return;
        }

        ensureDetectorInitialization();
        
        List<LanguageResult> results = detector.detectAll(content.toString());
        
        // leave now if no matches
        if (results.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No language found, using fallback language for " 
                        + reference);
            }
            metadata.setLanguage(fallbackLanguage);
            return;
        }
        
        Collections.sort(results, langResultComparator);
        metadata.setLanguage(results.get(0).getLanguage());
        
        if (keepProbabilities) {
            int count = 0;
            for (LanguageResult lang : results) {
                count++;
                String prefix = ImporterMetadata.DOC_LANGUAGE + "." + count;
                metadata.setString(prefix + ".tag", lang.getLanguage());
                metadata.setDouble(prefix + ".probability", lang.getRawScore());
            }
        }
    }

    /**
     * Gets whether to enable short text detection. 
     * @return <code>true</code> to use short text detection
     * @deprecated Since 2.6.0, no special optimization exists for short text
     * and this method always returns false
     */
    @Deprecated
    public boolean isShortText() {
        return false;
    }
    /**
     * Sets whether to use a detection algorithm optimized for short text.
     * Default is <code>false</code> (optimized for long text).
     * @param shortText <code>true</code> to use a detection algorithm 
     *                  optimized for short text
     * @deprecated Since 2.6.0, no special optimization exists for short text
     * and calling this method has no effect
     */
    @Deprecated
    public void setShortText(boolean shortText) {
        LOG.warn("Since 2.6.0, short text optimization is no longer"
                + "supported. It may come back in a future release "
                + "based on Tika language detection improvements.");
    }

    public boolean isKeepProbabilities() {
        return keepProbabilities;
    }
    /**
     * Sets whether to keep the match probabilities for each languages
     * detected.  Default is <code>false</code>.
     * @param keepProbabilities <code>true</code> to keep probabilities
     */
    public void setKeepProbabilities(boolean keepProbabilities) {
        ensureNotInitialized();
        this.keepProbabilities = keepProbabilities;
    }

    public String getFallbackLanguage() {
        return fallbackLanguage;
    }
    /**
     * Sets the fallback language when none are detected.  Default behavior
     * is to not tag incoming documents with a language field when no detection
     * occurs.
     * @param fallbackLanguage the default languages when no detection
     */
    public void setFallbackLanguage(String fallbackLanguage) {
        ensureNotInitialized();
        this.fallbackLanguage = fallbackLanguage;
    }

    private synchronized void ensureDetectorInitialization() 
            throws ImporterHandlerException {
        if (detector == null) {
            OptimaizeLangDetector d = new OptimaizeLangDetector();
            d.setShortText(isShortText());
           // d.setMixedLanguages(mixedLanguages)
            try {
                if (ArrayUtils.isEmpty(languages)) {
                    d.loadModels();
                } else {
                    d.loadModels(new HashSet<String>(Arrays.asList(languages)));
                }
            } catch (IOException e) {
                LOG.error("Cannot initialize language detector.", e);
                throw new ImporterHandlerException(e);
            }
            detector = d;
        }
    }

    public String[] getLanguages() {
        return ArrayUtils.clone(languages);
    }
    /**
     * Sets the language candidates for the language detection.
     * @param languages languages to consider for detection
     */
    public void setLanguages(String... languages) {
        ensureNotInitialized();
        this.languages = languages;
    }

    private void ensureNotInitialized() {
        if (detector != null) {
            throw new IllegalStateException(
                    "You cannot set LanguageTagger properties after it "
                  + "has been initialized (started tagging documents).");
        }
    }
    
    @Override
    protected void loadStringTaggerFromXML(
            XMLConfiguration xml) throws IOException {
        setShortText(xml.getBoolean("[@shortText]", isShortText()));
        setKeepProbabilities(xml.getBoolean(
                "[@keepProbabilities]", isKeepProbabilities()));
        setFallbackLanguage(xml.getString(
                "[@fallbackLanguage]", getFallbackLanguage()));
        setLanguages(ConfigurationUtil.getCSVArray(
                xml, "languages", getLanguages()));
    }

    @Override
    protected void saveStringTaggerToXML(
            EnhancedXMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute(
                "keepProbabilities", Boolean.toString(keepProbabilities));
        writer.writeAttribute("fallbackLanguage", fallbackLanguage);
        
        if (ArrayUtils.isNotEmpty(languages)) {
            writer.writeStartElement("languages");
            writer.writeCharacters(StringUtils.join(languages, ','));
            writer.writeEndElement();
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof LanguageTagger))
            return false;
        LanguageTagger castOther = (LanguageTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(other))
                .append(detector, castOther.detector)
                .append(keepProbabilities, castOther.keepProbabilities)
                .append(languages, castOther.languages)
                .append(fallbackLanguage, castOther.fallbackLanguage)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(detector)
                .append(keepProbabilities)
                .append(languages)
                .append(fallbackLanguage)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("detector", detector)
                .append("keepProbabilities", keepProbabilities)
                .append("languages", languages)
                .append("fallbackLanguage", fallbackLanguage)
                .toString();
    }
}
