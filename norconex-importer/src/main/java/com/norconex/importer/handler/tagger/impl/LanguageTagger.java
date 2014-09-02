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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractCharStreamTagger;
import com.norconex.language.detector.DetectedLanguage;
import com.norconex.language.detector.DetectedLanguages;
import com.norconex.language.detector.LanguageDetector;

/**
 * Detects a document language and adds it to a metadata field called
 * "<code>document.language</code>".  
 * Optionally adds all potential languages detected with their 
 * probability score as well as additional fields following this pattern:
 * <pre>
 * document.language.&lt;rank&gt;.tag
 * document.language.&lt;rank&gt;.probability</pre>
 * <p />
 * <code>&lt;rank&gt;</code> is to indicate the match order, based
 * on match probability score (starting at 1).
 * <p />
 * This tagger can be used both as a pre-parse (on text only) 
 * or post-parse handler.
 * <p />
 * <h3>Short vs Long Text:</h3>
 * To obtain optimal detection, long enough text is expected.  The default
 * detection algorithm is optimized for document with lots of text.  
 * If you know the documents to 
 * be analyzed are primarily made of short text (e.g. tweets, comments, etc), 
 * you can try to get better detection by configuring this tagger to 
 * use short-text optimization.  
 * <p />
 * <h3>Supported Languages:</h3>
 * Languages are represented as 
 * <a href="http://tools.ietf.org/html/bcp47">IETF BCP 47 language tags</a>.
 * The list of supported languages can vary
 * slightly depending on whether you chose long or short text optimization.
 * They are:
 * <p />
 * <table>
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
 * <p />
 * If you do not restrict the list of candidate languages to detect, the default
 * behavior is to try match all languages currently supported for your 
 * selected long/short text optimization.
 * <p />
 * <h3>XML configuration usage:</h3>
 * 
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.LanguageTagger"
 *          shortText="(false|true)"
 *          keepProbabilities="(false|true)"
 *          fallbackLanguage="" &gt;
 *      &lt;languages&gt
 *        (CSV list of language tag candidates. Defaults to the above list.)
 *      &lt;/languages&gt
 *      &lt;contentTypeRegex&gt;
 *          (regex to identify text content-types, overridding default)
 *      &lt;/contentTypeRegex&gt;
 *      &lt;restrictTo
 *              caseSensitive="[false|true]" &gt;
 *              property="(name of header/metadata name to match)"
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class LanguageTagger extends AbstractCharStreamTagger
        implements IXMLConfigurable {

    //TODO Check if doc.size is defined in metadata? If so, use it to 
    //determine if we are going with small or long text?
    
    //TODO provide ways to overwrite or specify custom language profiles 
    // in this tagger configuration?
    
    private static final long serialVersionUID = -7893789801356890263L;
    
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
        
        LanguageDetector detector = getInitializedDetector();
        DetectedLanguages langs = detector.detect(input);

        String languageTag = null;
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
        return languages;
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
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
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
            super.loadFromXML(xml);
        } catch (ConfigurationException e) {
            throw new IOException("Cannot load XML.", e);
        }
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("tagger");
            writer.writeAttribute("class", getClass().getCanonicalName());
            writer.writeAttribute("shortText", Boolean.toString(shortText));
            writer.writeAttribute(
                    "keepProbabilities", Boolean.toString(keepProbabilities));
            writer.writeAttribute("fallbackLanguage", fallbackLanguage);
            
            if (ArrayUtils.isNotEmpty(languages)) {
                writer.writeStartElement("languages");
                writer.writeCharacters(StringUtils.join(languages, ','));
                writer.writeEndElement();
            }

            super.saveToXML(writer);

            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((detector == null) ? 0 : detector.hashCode());
        result = prime * result + ((fallbackLanguage == null) 
                ? 0 : fallbackLanguage.hashCode());
        result = prime * result + (keepProbabilities ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(languages);
        result = prime * result + (shortText ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LanguageTagger)) {
            return false;
        }
        LanguageTagger other = (LanguageTagger) obj;
        if (detector == null) {
            if (other.detector != null) {
                return false;
            }
        } else if (!detector.equals(other.detector)) {
            return false;
        }
        if (fallbackLanguage == null) {
            if (other.fallbackLanguage != null) {
                return false;
            }
        } else if (!fallbackLanguage.equals(other.fallbackLanguage)) {
            return false;
        }
        if (keepProbabilities != other.keepProbabilities) {
            return false;
        }
        if (!Arrays.equals(languages, other.languages)) {
            return false;
        }
        if (shortText != other.shortText) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return "LanguageTagger [detector="
                + detector
                + ", shortText="
                + shortText
                + ", keepProbabilities="
                + keepProbabilities
                + ", languages="
                + (languages != null ? Arrays.asList(languages).subList(0,
                        Math.min(languages.length, maxLen)) : null)
                + ", fallbackLanguage=" + fallbackLanguage + "]";
    }
    
}
