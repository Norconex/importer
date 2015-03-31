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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractCharStreamTagger;
import com.norconex.language.detector.DetectedLanguage;
import com.norconex.language.detector.DetectedLanguages;
import com.norconex.language.detector.LanguageDetector;
import com.norconex.language.detector.LanguageDetectorException;

/**
 * <p>Detects a document language and adds it to the 
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
 * <h3>Short vs Long Text:</h3>
 * <p>To obtain optimal detection, long enough text is expected.  The default
 * detection algorithm is optimized for document with lots of text.  
 * If you know the documents to 
 * be analyzed are primarily made of short text (e.g. tweets, comments, etc), 
 * you can try to get better detection by configuring this tagger to 
 * use short-text optimization.</p>
 * 
 * <h3>Supported Languages:</h3>
 * <p>Languages are represented as 
 * <a href="http://tools.ietf.org/html/bcp47">IETF BCP 47 language tags</a>.
 * The list of supported languages can vary
 * slightly depending on whether you chose long or short text optimization.
 * They are:</p>
 * 
 * <table>
 *  <caption>Supported languages</caption>
 *  <tr>
 *   <td><b>Tag</b></td>
 *   <td><b>Name</b></td>
 *   <td><b>Note</b></td>
 *  </tr>
 *  <tr>
 *   <td>af</td>
 *   <td>Afrikaans</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>ar</td>
 *   <td>Arabic</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>bg</td>
 *   <td>Bulgarian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>bn</td>
 *   <td>Bengali</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>ca</td>
 *   <td>Catalan</td>
 *   <td>Short text only</td>
 *  </tr>
 *  <tr>
 *   <td>cs</td>
 *   <td>Czech</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>da</td>
 *   <td>Danish</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>de</td>
 *   <td>German</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>el</td>
 *   <td>Greek</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>en</td>
 *   <td>English</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>es</td>
 *   <td>Spanish</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>et</td>
 *   <td>Estonian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>fa</td>
 *   <td>Persian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>fi</td>
 *   <td>Finnish</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>fr</td>
 *   <td>French</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>gu</td>
 *   <td>Gujarati</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>he</td>
 *   <td>Hebrew</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>hi</td>
 *   <td>Hindi</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>hr</td>
 *   <td>Croatian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>hu</td>
 *   <td>Hungarian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>id</td>
 *   <td>Indonesian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>it</td>
 *   <td>Italian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>ja</td>
 *   <td>Japanese</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>kn</td>
 *   <td>Kannada</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>ko</td>
 *   <td>Korean</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>lt</td>
 *   <td>Lithuanian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>lv</td>
 *   <td>Latvian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>mk</td>
 *   <td>Macedonian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>ml</td>
 *   <td>Malayalam</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>mr</td>
 *   <td>Marathi</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>ne</td>
 *   <td>Nepali</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>nl</td>
 *   <td>Dutch</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>no</td>
 *   <td>Norwegian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>pa</td>
 *   <td>Punjabi</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>pl</td>
 *   <td>Polish</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>pt</td>
 *   <td>Portuguese</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>ro</td>
 *   <td>Romanian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>ru</td>
 *   <td>Russian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>si</td>
 *   <td>Sinhala</td>
 *   <td>Short text only</td>
 *  </tr>
 *  <tr>
 *   <td>sk</td>
 *   <td>Slovak</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>sl</td>
 *   <td>Slovene</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>so</td>
 *   <td>Somali</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>sq</td>
 *   <td>Albanian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>sv</td>
 *   <td>Swedish</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>sw</td>
 *   <td>Swahili</td>
 *   <td>Long text only</td>
 *  </tr>
 *  <tr>
 *   <td>ta</td>
 *   <td>Tamil</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>te</td>
 *   <td>Telugu</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>th</td>
 *   <td>Thai</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>tl</td>
 *   <td>Tagalog</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>tr</td>
 *   <td>Turkish</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>uk</td>
 *   <td>Ukrainian</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>ur</td>
 *   <td>Urdu</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>vi</td>
 *   <td>Vietnamese</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>zh-cn</td>
 *   <td>Simplified Chinese</td>
 *   <td></td>
 *  </tr>
 *  <tr>
 *   <td>zh-tw</td>
 *   <td>Traditional Chinese</td>
 *   <td></td>
 *  </tr>
 * </table>
 * 
 * <p>If you do not restrict the list of language candidates to detect, the default
 * behavior is to try match all languages currently supported for your 
 * selected long/short text optimization.</p>
 * 
 * <h3>XML configuration usage:</h3>
 * 
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.LanguageTagger"
 *          shortText="(false|true)"
 *          keepProbabilities="(false|true)"
 *          fallbackLanguage="" &gt;
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
@SuppressWarnings("nls")
public class LanguageTagger extends AbstractCharStreamTagger
        implements IXMLConfigurable {

    //TODO Check if doc.size is defined in metadata? If so, use it to 
    //determine if we are going with small or long text?
    
    //TODO provide ways to overwrite or specify custom language profiles 
    // in this tagger configuration?
    
    private static final Logger LOG = 
            LogManager.getLogger(LanguageTagger.class);
    
    private LanguageDetector detector;
    private boolean shortText;
    private boolean keepProbabilities;
    private String[] languages;
    private String fallbackLanguage;
    
    @Override
    protected void tagTextDocument(
            String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        
        String languageTag = null;

        try {
            LanguageDetector detector = getInitializedDetector();
            DetectedLanguages langs = detector.detect(input);
            if (!langs.isEmpty()) {
                languageTag = langs.getBestLanguage().getTag();
            }
            if (StringUtils.isBlank(languageTag)) {
                languageTag = fallbackLanguage;
            }
            if (StringUtils.isNotBlank(languageTag)) {
                metadata.setLanguage(languageTag);
            }
            if (keepProbabilities) {
                int count = 0;
                for (DetectedLanguage lang : langs) {
                    count++;
                    String prefix = ImporterMetadata.DOC_LANGUAGE + "." + count;
                    metadata.setString(prefix + ".tag", lang.getTag());
                    metadata.setDouble(
                            prefix + ".probability", lang.getProbability());
                }
            }
        } catch (LanguageDetectorException e) {
            LOG.warn("Could not detect language. Using fallback language \""
                    + fallbackLanguage + "\" for: " + reference);
            LOG.debug("", e);
            if (StringUtils.isNotBlank(fallbackLanguage)) {
                metadata.setLanguage(fallbackLanguage);
            }
        }
    }

    public boolean isShortText() {
        return shortText;
    }
    /**
     * Sets whether to use a detection algorithm optimized for short text.
     * Default is <code>false</code> (optimized for long text).
     * @param shortText <code>true</code> to use a detection algorithm 
     *                  optimized for short text
     */
    public void setShortText(boolean shortText) {
        ensureNotInitialized();
        this.shortText = shortText;
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

    private LanguageDetector getInitializedDetector() {
        if (detector == null) {
            if (ArrayUtils.isEmpty(languages)) {
                detector = new LanguageDetector(shortText);
            } else {
                detector = new LanguageDetector(shortText, languages);
            }
        }
        return detector;
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
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setShortText(xml.getBoolean("[@shortText]", isShortText()));
        setKeepProbabilities(xml.getBoolean(
                "[@keepProbabilities]", isKeepProbabilities()));
        setFallbackLanguage(xml.getString(
                "[@fallbackLanguage]", getFallbackLanguage()));
        String languages = xml.getString("languages");
        if (StringUtils.isNotEmpty(languages)) {
            String[] langArray = languages.split("[\\s,]+");
            setLanguages(langArray);
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute("shortText", Boolean.toString(shortText));
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
        return new EqualsBuilder().appendSuper(super.equals(other))
                .append(detector, castOther.detector)
                .append(shortText, castOther.shortText)
                .append(keepProbabilities, castOther.keepProbabilities)
                .append(languages, castOther.languages)
                .append(fallbackLanguage, castOther.fallbackLanguage)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode())
                .append(detector).append(shortText).append(keepProbabilities)
                .append(languages).append(fallbackLanguage).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString())
                .append("detector", detector).append("shortText", shortText)
                .append("keepProbabilities", keepProbabilities)
                .append("languages", languages)
                .append("fallbackLanguage", fallbackLanguage).toString();
    }
}
