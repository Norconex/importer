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
package com.norconex.importer.handler.splitter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.language.translate.CachedTranslator;
import org.apache.tika.language.translate.MicrosoftTranslator;
import org.apache.tika.language.translate.MosesTranslator;
import org.apache.tika.language.translate.Translator;

import com.memetix.mst.language.Language;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.io.TextReader;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.ImporterRuntimeException;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.splitter.AbstractDocumentSplitter;
import com.norconex.importer.handler.splitter.SplittableDocument;

/**
 * Translate documents using one of the supported translation API.  The 
 * following lists the supported APIs, along with the authentication properties
 * required by each:
 * <ul>
 *   <li><a href="http://blogs.msdn.com/b/translation/p/gettingstarted1.aspx">microsoft</a>
 *     <ul>
 *       <li>clientId</li>
 *       <li>clientSecret</li>
 *     </ul>
 *   </li>
 *   <li><a href="https://cloud.google.com/translate/">google</a>
 *     <ul>
 *       <li>apiKey</li>
 *     </ul>
 *   </li>
 *   <li><a href="http://www.lingo24.com/">lingo24</a>
 *     <ul>
 *       <li>userKey</li>
 *     </ul>
 *   </li>
 *   <li><a href="http://www.statmt.org/moses/">moses</a>
 *     <ul>
 *       <li>smtPath</li>
 *       <li>scriptPath</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <p/>
 * For example, the
 * Microsoft Translation API requires a client ID and a client 
 * secret,
 * both obtained on Microsoft Azure Marketplace with your Microsoft account.
 * <p/>
 * Translated documents will have the original document language stored in 
 * a field "document.translatedFrom".
 * <p/>
 * This class is not a document "splitter" per se, but like regular splitters, 
 * the translation
 * will create children documents for each translation performed.  The parent
 * document will always remain the original document, while the children
 * will always be the translations.
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;splitter class="com.norconex.importer.handler.splitter.impl.TranslatorSplitter"
 *          api="(microsoft|google|lingo24|moses)" &gt;
 *      &lt;ignoreContent&gt;(false|true)&lt;/ignoreContent&gt;
 *      &lt;ignoreNonTranslatedFields&gt;(false|true)&lt;/ignoreNonTranslatedFields&gt;
 *      &lt;fieldsToTranslate&gt;(coma-separated list of fields)&lt;/fieldsToTranslate&gt;
 *      &lt;sourceLanguageField&gt;(field containing language)&lt;/sourceLanguageField&gt;
 *      &lt;sourceLanguage&gt;(language when no source language field)&lt;/sourceLanguage&gt;
 *      &lt;targetLanguages&gt;(coma-separated list of languages)&lt;/targetLanguages&gt;
 *      
 *      &lt;!-- Microsoft --&gt;
 *      &lt;clientId&gt;...&lt;/clientId&gt;
 *      &lt;clientSecret&gt;...&lt;/clientSecret&gt;
 *      
 *      &lt;!-- Google --&gt;
 *      &lt;apiKey&gt;...&lt;/apiKey&gt;
 *      
 *      &lt;!-- Lingo24 --&gt;
 *      &lt;userKey&gt;...&lt;/userKey&gt;
 *      
 *      &lt;!-- Moses --&gt;
 *      &lt;smtPath&gt;...&lt;/smtPath&gt;
 *      &lt;scriptPath&gt;...&lt;/scriptPath&gt;
 *          
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/splitter&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.1.0
 */
public class TranslatorSplitter extends AbstractDocumentSplitter {

    public static final String API_MICROSOFT = "microsoft";
    public static final String API_GOOGLE = "google";
    public static final String API_LINGO24 = "lingo24";
    public static final String API_MOSES = "moses";
    
    private final Map<String, TranslatorStrategy> translators = new HashMap<>();
    
    private String api;
    
    private boolean ignoreContent;
    
    private String[] fieldsToTranslate;
    private boolean ignoreNonTranslatedFields;
            
    //TODO method: add a field with _fr suffix
    // splitter is for new docs... adding field should be in transformer?

    private String sourceLanguageField;
    private String sourceLanguage;
    private String[] targetLanguages;
    
    // Microsoft
    private String clientId;
    private String clientSecret;

    // Google
    private String apiKey;

    // Lingo24
    private String userKey;
    
    // Moses
    private String smtPath;
    private String scriptPath;
    
    
    /**
     * Constructor.
     */
    public TranslatorSplitter() {
        super();
        translators.put(API_MICROSOFT, new TranslatorStrategy() {
            @Override
            public Translator createTranslator() {
                MicrosoftTranslator t = new MicrosoftTranslator();
                t.setId(getClientId());
                t.setSecret(getClientSecret());
                return t;
            }
            @Override
            public void validateProperties() throws ImporterHandlerException {
                if (StringUtils.isAnyBlank(getClientId(), getClientSecret())) {
                    throw new ImporterHandlerException(
                           "Both clientId and clientSecret must be specified.");
                }
            }
        });
        translators.put(API_GOOGLE, new TranslatorStrategy() {
            @Override
            public Translator createTranslator() {
                FixedGoogleTranslator t = new FixedGoogleTranslator();
                t.setApiKey(getApiKey());
                return t;
            }
            @Override
            public void validateProperties() throws ImporterHandlerException {
                if (StringUtils.isAnyBlank(getApiKey())) {
                    throw new ImporterHandlerException(
                           "apiKey must be specified.");
                }
            }
        });
        translators.put(API_LINGO24, new TranslatorStrategy() {
            @Override
            public Translator createTranslator() {
                FixedLingo24Translator t = new FixedLingo24Translator();
                t.setUserKey(getUserKey());
                return t;
            }
            @Override
            public void validateProperties() throws ImporterHandlerException {
                if (StringUtils.isAnyBlank(getUserKey())) {
                    throw new ImporterHandlerException(
                           "userKey must be specified.");
                }
            }
        });
        translators.put(API_MOSES, new TranslatorStrategy() {
            @Override
            public Translator createTranslator() {
                return new MosesTranslator(getSmtPath(), getScriptPath());
            }
            @Override
            public void validateProperties() throws ImporterHandlerException {
                if (StringUtils.isAnyBlank(getSmtPath(), getScriptPath())) {
                    throw new ImporterHandlerException(
                           "Both smtPath and scriptPath must be specified.");
                }
            }
        });
    }

    @Override
    protected List<ImporterDocument> splitApplicableDocument(
            SplittableDocument doc, OutputStream output,
            CachedStreamFactory streamFactory, boolean parsed)
            throws ImporterHandlerException {
  
        // Do not re-translate a document already translated
        if (doc.getMetadata().containsKey(
                ImporterMetadata.DOC_TRANSLATED_FROM)) {
            return null;
        }

        validateProperties(doc);

        List<ImporterDocument> translatedDocs = new ArrayList<>();
        
        InputStream is = doc.getInput();
        CachedInputStream cachedInput = null;
        if (is instanceof CachedInputStream) {
            cachedInput = (CachedInputStream) is;
        } else {
            cachedInput = streamFactory.newInputStream(is);
        }
        
        for (String lang : targetLanguages) {
            if (Objects.equals(sourceLanguage, lang)) {
                continue;
            }
            cachedInput.rewind();
            try (TextReader reader = new TextReader(
                    new InputStreamReader(cachedInput, CharEncoding.UTF_8), 
                    getTranslatorStrategy().getReadSize())) {
                translatedDocs.add(
                        translateDocument(doc, streamFactory, lang, reader));
            } catch (Exception e) {
                String extra = "";
                if (API_GOOGLE.equals(api) 
                        && e instanceof IndexOutOfBoundsException) {
                    extra = " \"apiKey\" is likely invalid.";
                }
                throw new ImporterHandlerException(
                        "Translation failed form \"" + sourceLanguage
                      + "\" to \"" + lang + "\" for: \""
                      + doc.getReference() + "\"." + extra, e);
            }
        }
        return translatedDocs;
    }
    
    public boolean isIgnoreContent() {
        return ignoreContent;
    }
    public void setIgnoreContent(boolean ignoreContent) {
        this.ignoreContent = ignoreContent;
    }

    public String[] getFieldsToTranslate() {
        return fieldsToTranslate;
    }
    public void setFieldsToTranslate(String... fieldsToTranslate) {
        this.fieldsToTranslate = fieldsToTranslate;
    }

    public boolean isIgnoreNonTranslatedFields() {
        return ignoreNonTranslatedFields;
    }
    public void setIgnoreNonTranslatedFields(
            boolean ignoreNonTranslatedFields) {
        this.ignoreNonTranslatedFields = ignoreNonTranslatedFields;
    }

    public String getSourceLanguageField() {
        return sourceLanguageField;
    }
    public void setSourceLanguageField(String sourceLanguageField) {
        this.sourceLanguageField = sourceLanguageField;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }
    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String[] getTargetLanguages() {
        return targetLanguages;
    }
    public void setTargetLanguages(String... targetLanguages) {
        this.targetLanguages = targetLanguages;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getUserKey() {
        return userKey;
    }
    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }
    
    public String getSmtPath() {
        return smtPath;
    }
    public void setSmtPath(String smtPath) {
        this.smtPath = smtPath;
    }

    public String getScriptPath() {
        return scriptPath;
    }
    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    private TranslatorStrategy getTranslatorStrategy() {
        TranslatorStrategy strategy = translators.get(api);
        if (strategy == null) {
            throw new ImporterRuntimeException(
                    "Unsupported translation api: " + api);
        }
        return strategy;
    }
    
    private ImporterDocument translateDocument(SplittableDocument doc,
            CachedStreamFactory streamFactory, String targetLang,
            TextReader reader) throws Exception {
        
        Translator translator = getTranslatorStrategy().getTranslator();
        String sourceLang = getResolvedSourceLanguage(doc);
        
        
        //--- Do Fields ---
        ImporterMetadata childMeta = translateFields(
                doc, translator, sourceLang, targetLang);
                
        //--- Do Content ---
        CachedInputStream childInput = null;
        if (!ignoreContent) {
            CachedOutputStream childContent = streamFactory.newOuputStream();
            
            String text = null;
            while ((text = reader.readText()) != null) {
                String txt = translator.translate(text, sourceLang, targetLang);
                childContent.write(txt.getBytes(CharEncoding.UTF_8));
                childContent.flush();
            }
            IOUtils.closeQuietly(reader);
            childInput = childContent.getInputStream();
        } else {
            childInput = streamFactory.newInputStream();
        }

        //--- Build child document ---
        String childEmbedRef = "translation-" + targetLang;
        String childDocRef = doc.getReference() + "!" + childEmbedRef;

        ImporterDocument childDoc = new ImporterDocument(
                childDocRef, childInput, childMeta); 

        childMeta.setReference(childDocRef);
        childMeta.setEmbeddedReference(childEmbedRef);
        childMeta.setEmbeddedParentReference(doc.getReference());
        childMeta.setEmbeddedParentRootReference(doc.getReference());
        childMeta.setString(ImporterMetadata.DOC_LANGUAGE, targetLang);
        childMeta.setString(ImporterMetadata.DOC_TRANSLATED_FROM, sourceLang);
        return childDoc;
    }
    
    private ImporterMetadata translateFields(
            SplittableDocument doc, Translator translator, 
            String sourceLang, String targetLang) throws Exception {
        ImporterMetadata childMeta = new ImporterMetadata();
        if (ignoreNonTranslatedFields) {
            if (fieldsToTranslate == null) {
                return childMeta;
            }
            for (String key : fieldsToTranslate) {
                List<String> values = doc.getMetadata().get(key);
                if (values != null) {
                    childMeta.put(key, values);
                }
            }
        } else {
            childMeta.load(doc.getMetadata());
            if (fieldsToTranslate == null) {
                return childMeta;
            }
        }
        
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < fieldsToTranslate.length; i++) {
            List<String> values = doc.getMetadata().get(fieldsToTranslate[i]);
            for (String value : values) {
                b.append("[" + value.replaceAll("[\n\\[\\]]", " ") + "]");
            }
            b.append("\n");
        }
        if (b.length() == 0) {
            return childMeta;
        }
        
        String txt = translator.translate(b.toString(), sourceLang, targetLang);
        List<String> lines = IOUtils.readLines(new StringReader(txt));
        int index = 0;
        for (String line : lines) {
            line = StringUtils.removeStart(line, "[");
            line = StringUtils.removeEnd(line, "]");
            String[] values = StringUtils.splitByWholeSeparator(line, "][");
            childMeta.setString(fieldsToTranslate[index], values);
            index++;
        }
        return childMeta;
    }
    
    private void validateProperties(SplittableDocument doc) 
            throws ImporterHandlerException {
        if (StringUtils.isBlank(getApi())) {
            throw new ImporterHandlerException(
                    "Must specify a translation api.");
        }
        if (ArrayUtils.isEmpty(targetLanguages)) {
            throw new ImporterHandlerException(
                    "No translation target language(s) specified.");
        }
        
        String sourceLang = getResolvedSourceLanguage(doc);
        if (sourceLang == null || Language.fromString(sourceLang) == null) {
            throw new ImporterHandlerException(
                    "Unsupported source language: \"" + sourceLang + "\"");
        }
        for (String targetLang : targetLanguages) {
            if (Language.fromString(targetLang) == null) {
                throw new ImporterHandlerException(
                        "Unsupported target language: \"" + targetLang + "\"");
            }
        }
        getTranslatorStrategy().validateProperties();
    }

    private String getResolvedSourceLanguage(SplittableDocument doc) {
        String lang = doc.getMetadata().getString(sourceLanguageField);
        if (StringUtils.isBlank(lang)) {
            lang = sourceLanguage;
        }
        return lang;
    }

    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getApi() {
        return api;
    }
    public void setApi(String api) {
        this.api = api;
    }

    public static void main(String[] args) throws ImporterHandlerException {
        new TranslatorSplitter().splitApplicableDocument(null, null, null, false);
    }
    
    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setApi(xml.getString("[@api]", api));
        setIgnoreContent(xml.getBoolean("ignoreContent", ignoreContent));
        setIgnoreNonTranslatedFields(xml.getBoolean(
                "ignoreNonTranslatedFields", ignoreNonTranslatedFields));
        
        String[] fields = fieldsToTranslate;
        String fieldsString = xml.getString("fieldsToTranslate", null);
        if (StringUtils.isNotBlank(fieldsString)) {
            fields = fieldsString.split("\\s*,\\s*");
        }
        setFieldsToTranslate(fields);

        setSourceLanguageField(xml.getString(
                "sourceLanguageField", sourceLanguageField));
        setSourceLanguage(xml.getString("sourceLanguage", sourceLanguage));

        String[] langs = targetLanguages;
        String langsString = xml.getString("targetLanguages", null);
        if (StringUtils.isNotBlank(langsString)) {
            langs = langsString.split("\\s*,\\s*");
        }
        setTargetLanguages(langs);
        
        setClientId(xml.getString("clientId", clientId));
        setClientSecret(xml.getString("clientSecret", clientSecret));
        setApiKey(xml.getString("apiKey", apiKey));
        setUserKey(xml.getString("userKey", userKey));
        setSmtPath(xml.getString("smtPath", smtPath));
        setScriptPath(xml.getString("scriptPath", scriptPath));
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute("api", api);
        writer.writeElementBoolean("ignoreContent", ignoreContent);
        writer.writeElementBoolean(
                "ignoreNonTranslatedFields", ignoreNonTranslatedFields);
        writer.writeElementString("fieldsToTranslate", 
                StringUtils.join(fieldsToTranslate, ","));
        writer.writeElementString("sourceLanguageField", sourceLanguageField);
        writer.writeElementString("sourceLanguage", sourceLanguage);
        writer.writeElementString("targetLanguages", 
                StringUtils.join(targetLanguages, ","));
        
        writer.writeElementString("clientId", clientId);
        writer.writeElementString("clientSecret", clientSecret);
        writer.writeElementString("apiKey", apiKey);
        writer.writeElementString("userKey", userKey);
        writer.writeElementString("smtPath", smtPath);
        writer.writeElementString("scriptPath", scriptPath);
    }
    
    private static abstract class TranslatorStrategy {
        private final int DEFAULT_READ_SIZE = 
                (int) DataUnit.KB.toBytes(2);
        private Translator translator;
        public int getReadSize() {
            return DEFAULT_READ_SIZE;
        }
        public final Translator getTranslator() {
            if (translator == null) {
                translator = new CachedTranslator(createTranslator());
            }
            return translator;
        }
        protected abstract Translator createTranslator();
        public abstract void validateProperties() 
                throws ImporterHandlerException;
    }
}
