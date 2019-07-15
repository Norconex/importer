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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
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
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.LanguageTagger"
 *          keepProbabilities="(false|true)"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)"
 *          fallbackLanguage="(default language when detection failed)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;languages&gt;
 *        (CSV list of language tag candidates. Defaults to the above list.)
 *      &lt;/languages&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following detects whether pages are English or French, falling back to
 * English if detection failed.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.LanguageTagger"
 *          fallbackLanguage="en" &gt;
 *      &lt;languages&gt;en, fr&lt;/languages&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
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
            LoggerFactory.getLogger(LanguageTagger.class);

    private LanguageDetector detector;
    private boolean keepProbabilities;
    private final List<String> languages = new ArrayList<>();
    private String fallbackLanguage;

    private final Comparator<LanguageResult> langResultComparator =
            (o1, o2) -> Float.compare(o2.getRawScore(), o1.getRawScore());

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
                metadata.set(prefix + ".tag", lang.getLanguage());
                metadata.set(prefix + ".probability", lang.getRawScore());
            }
        }
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
            try {
                if (languages.isEmpty()) {
                    d.loadModels();
                } else {
                    d.loadModels(new HashSet<>(languages));
                }
            } catch (IOException e) {
                throw new ImporterHandlerException(
                        "Cannot initialize language detector.", e);
            }
            detector = d;
        }
    }

    public List<String> getLanguages() {
        return Collections.unmodifiableList(languages);
    }
    /**
     * Sets the language candidates for the language detection.
     * @param languages languages to consider for detection
     */
    public void setLanguages(List<String> languages) {
        ensureNotInitialized();
        CollectionUtil.setAll(this.languages, languages);
    }

    private void ensureNotInitialized() {
        if (detector != null) {
            throw new IllegalStateException(
                    "You cannot set LanguageTagger properties after it "
                  + "has been initialized (started tagging documents).");
        }
    }

    @Override
    protected void loadStringTaggerFromXML(XML xml) {
        setKeepProbabilities(xml.getBoolean(
                "@keepProbabilities", keepProbabilities));
        setFallbackLanguage(xml.getString(
                "@fallbackLanguage", fallbackLanguage));
        setLanguages(xml.getDelimitedStringList("languages", languages));
    }

    @Override
    protected void saveStringTaggerToXML(XML xml) {
        xml.setAttribute("keepProbabilities", keepProbabilities);
        xml.setAttribute("fallbackLanguage", fallbackLanguage);
        xml.addDelimitedElementList("languages", languages);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
