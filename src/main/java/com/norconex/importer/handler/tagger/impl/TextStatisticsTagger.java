/* Copyright 2014-2020 Norconex Inc.
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

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.BreakIterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractCharStreamTagger;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Analyzes the content of the supplied document and adds statistical
 * information about its content or field as metadata fields.  Default
 * behavior provide the statistics about the content. Refer to the following
 * for the new metadata fields to be created along with their description.</p>
 *
 * <table border="1">
 *  <caption>Statistic fields</caption>
 *   <tr>
 *     <th>Field name</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>document.stat.characterCount</td>
 *     <td>Total number of characters (excluding carriage returns/line
 *         feed).</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.wordCount</td>
 *     <td>Total number of words.</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.sentenceCount</td>
 *     <td>Total number of sentences.</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.paragraphCount</td>
 *     <td>Total number of paragraph.</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.averageWordCharacterCount</td>
 *     <td>Average number of character in every words.</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.averageSentenceCharacterCount</td>
 *     <td>Average number of character in sentences (including non-word
 *         characters, such as spaces, or slashes).</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.averageSentenceWordCount</td>
 *     <td>Average number of words per sentences.</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.averageParagraphCharacterCount</td>
 *     <td>Average number of characters in paragraphs (including non-word
 *         characters, such as spaces, or slashes).</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.averageParagraphSentenceCount</td>
 *     <td>Average number of sentences per paragraphs.</td>
 *   </tr>
 *   <tr>
 *     <td>document.stat.averageParagraphWordCount</td>
 *     <td>Average number of words per paragraphs.</td>
 *   </tr>
 * </table>
 *
 * <p>You can specify a field matcher to obtain statistics about matching
 * fields instead.
 * When you do so, the field name will be inserted in the above
 * names, right after "document.stat.". E.g.:
 * <code>document.stat.myfield.characterCount</code></p>
 *
 * <p>Can be used both as a pre-parse (text-only) or post-parse handler.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.TextStatisticsTagger"
 *     {@nx.include com.norconex.importer.handler.tagger.AbstractCharStreamTagger#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *     (optional expression matching source fields to analyze instead of content)
 *   </fieldMatcher>
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="TextStatisticsTagger">
 *   <fieldMatcher>statistics</fieldMatcher>
 * </handler>
 * }
 * <p>
 * The above create statistics from the value of a field called "statistics".
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("javadoc")
public class TextStatisticsTagger extends AbstractCharStreamTagger
        implements IXMLConfigurable {

    private static final Pattern PATTERN_WORD = Pattern.compile(
            "\\w+\\-{0,1}\\w*", Pattern.UNICODE_CHARACTER_CLASS);

    private final TextMatcher fieldMatcher = new TextMatcher();

    @Override
    protected void tagTextDocument(
            HandlerDoc doc, Reader input, ParseState parseState)
                    throws ImporterHandlerException {
        if (fieldMatcher.getPattern() == null) {
            analyze(input, doc.getMetadata(), null);
        } else {
            for (Entry<String, List<String>> en :
                    doc.getMetadata().matchKeys(fieldMatcher).entrySet()) {
                analyze(new StringReader(StringUtils.join(
                       en.getValue(), "\n\n")), doc.getMetadata(), en.getKey());
            }
        }
    }

    protected void analyze(Reader input, Properties metadata, String field) {
        var charCount = 0L;
        var wordCharCount = 0L;
        var wordCount = 0L;
        var sentenceCount = 0L;
        var sentenceCharCount = 0L;
        var paragraphCount = 0L;

        //TODO make this more efficient, by doing all this in one pass.
        var it = IOUtils.lineIterator(input);
        while (it.hasNext()) {
            var line = it.nextLine().trim();
            if (StringUtils.isBlank(line)) {
                continue;
            }

            // Paragraph
            paragraphCount++;

            // Character
            charCount += line.length();

            // Word
            var matcher = PATTERN_WORD.matcher(line);
            while (matcher.find()) {
                var wordLength = matcher.end() - matcher.start();
                wordCount++;
                wordCharCount += wordLength;
            }

            // Sentence
            var boundary = BreakIterator.getSentenceInstance();
            boundary.setText(line);
            var start = boundary.first();
            for (var end = boundary.next(); end != BreakIterator.DONE;
                    start = end, end = boundary.next()) {
                sentenceCharCount += (end - start);
                sentenceCount++;
            }
        }

        //--- Add fields ---
        var prefix = "document.stat.";
        if (StringUtils.isNotBlank(field)) {
            prefix += field.trim() + ".";
        }
        metadata.add(prefix + "characterCount", charCount);
        metadata.add(prefix + "wordCount", wordCount);
        metadata.add(prefix + "sentenceCount", sentenceCount);
        metadata.add(prefix + "paragraphCount", paragraphCount);
        metadata.add(prefix + "averageWordCharacterCount",
                divide(wordCharCount, wordCount));
        metadata.add(prefix + "averageSentenceCharacterCount",
                divide(sentenceCharCount, sentenceCount));
        metadata.add(prefix + "averageSentenceWordCount",
                divide(wordCount, sentenceCount));
        metadata.add(prefix + "averageParagraphCharacterCount",
                divide(charCount, paragraphCount));
        metadata.add(prefix + "averageParagraphSentenceCount",
                divide(sentenceCount, paragraphCount));
        metadata.add(prefix + "averageParagraphWordCount",
                divide(wordCount, paragraphCount));

    }

    private String divide(long value, long divisor) {
        return BigDecimal.valueOf(value).divide(
                BigDecimal.valueOf(divisor), 1,
                        RoundingMode.HALF_UP).toString();
    }

    /**
     * Gets the name of field containing the text to analyze.
     * @return field name
     * @deprecated Since 3.0.0, use {@link #getFieldMatcher()}.
     */
    @Deprecated
    public String getFieldName() {
        return fieldMatcher.getPattern();
    }
    /**
     * Sets the name of field containing the text to analyze.
     * @param fieldName field name
     * @deprecated Since 3.0.0, use {@link #setFieldMatcher(TextMatcher)}.
     */
    @Deprecated
    public void setFieldName(String fieldName) {
        fieldMatcher.setPattern(fieldName);
    }
    /**
     * Gets field matcher for fields to split.
     * @return field matcher
     * @since 3.0.0
     */
    public TextMatcher getFieldMatcher() {
        return fieldMatcher;
    }
    /**
     * Sets the field matcher for fields to split.
     * @param fieldMatcher field matcher
     * @since 3.0.0
     */
    public void setFieldMatcher(TextMatcher fieldMatcher) {
        this.fieldMatcher.copyFrom(fieldMatcher);
    }


    @Override
    protected void loadCharStreamTaggerFromXML(XML xml) {
        xml.checkDeprecated("@fieldName", "fieldMatcher", true);
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
    }

    @Override
    protected void saveCharStreamTaggerToXML(XML xml) {
        fieldMatcher.saveToXML(xml.addElement("fieldMatcher"));
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
