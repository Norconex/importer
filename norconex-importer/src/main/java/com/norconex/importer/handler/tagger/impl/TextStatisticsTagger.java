/* Copyright 2014-2018 Norconex Inc.
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
import java.math.BigDecimal;
import java.text.BreakIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractCharStreamTagger;

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
 * <p>You can specify a field name to obtain statistics about that field instead.
 * When you do so, the field name will be inserted in the above
 * names, right after "document.stat.". E.g.:
 * <code>document.stat.myfield.characterCount</code></p>
 *
 * <p>Can be used both as a pre-parse (text-only) or post-parse handler.</p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TextStatisticsTagger"
 *          sourceCharset="(character encoding)"
 *          fieldName="(optional field name instead of using content)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following store the statistics in a field called "statistics".
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TextStatisticsTagger"
 *          fieldName="statistics" /&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class TextStatisticsTagger extends AbstractCharStreamTagger
        implements IXMLConfigurable {

    private static final Pattern PATTERN_WORD = Pattern.compile(
            "\\w+\\-{0,1}\\w*", Pattern.UNICODE_CHARACTER_CLASS);

    private String fieldName;

    @Override
    protected void tagTextDocument(String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        long charCount = 0;
        long wordCharCount = 0;
        long wordCount = 0;
        long sentenceCount = 0;
        long sentenceCharCount = 0;
        long paragraphCount = 0;

        //TODO make this more efficient, by doing all this in one pass.
        LineIterator it = IOUtils.lineIterator(input);
        while (it.hasNext()) {
            String line = it.nextLine().trim();
            if (StringUtils.isBlank(line)) {
                continue;
            }

            // Paragraph
            paragraphCount++;

            // Character
            charCount += line.length();

            // Word
            Matcher matcher = PATTERN_WORD.matcher(line);
            while (matcher.find()) {
                int wordLength = matcher.end() - matcher.start();
                wordCount++;
                wordCharCount += wordLength;
            }

            // Sentence
            BreakIterator boundary = BreakIterator.getSentenceInstance();
            boundary.setText(line);
            int start = boundary.first();
            for (int end = boundary.next(); end != BreakIterator.DONE;
                    start = end, end = boundary.next()) {
                sentenceCharCount += (end - start);
                sentenceCount++;
            }
        }

        String field = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(fieldName)) {
            field = fieldName.trim() + ".";
        }

        //--- Add fields ---
        metadata.addLong(
                "document.stat." + field + "characterCount", charCount);
        metadata.addLong("document.stat." + field + "wordCount", wordCount);
        metadata.addLong(
                "document.stat." + field + "sentenceCount", sentenceCount);
        metadata.addLong(
                "document.stat." + field + "paragraphCount", paragraphCount);
        metadata.addString(
                "document.stat." + field + "averageWordCharacterCount",
                divide(wordCharCount, wordCount));
        metadata.addString(
                "document.stat." + field + "averageSentenceCharacterCount",
                divide(sentenceCharCount, sentenceCount));
        metadata.addString(
                "document.stat." + field + "averageSentenceWordCount",
                divide(wordCount, sentenceCount));
        metadata.addString(
                "document.stat." + field + "averageParagraphCharacterCount",
                divide(charCount, paragraphCount));
        metadata.addString(
                "document.stat." + field + "averageParagraphSentenceCount",
                divide(sentenceCount, paragraphCount));
        metadata.addString(
                "document.stat." + field + "averageParagraphWordCount",
                divide(wordCount, paragraphCount));

    }

    private String divide(long value, long divisor) {
        return BigDecimal.valueOf(value).divide(
                BigDecimal.valueOf(divisor), 1,
                        BigDecimal.ROUND_HALF_UP).toString();
    }

    public String getFieldName() {
        return fieldName;
    }
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    protected void loadCharStreamTaggerFromXML(XML xml) {
        setFieldName(xml.getString("@fieldName", getFieldName()));
    }

    @Override
    protected void saveCharStreamTaggerToXML(XML xml) {
        xml.setAttribute("fieldName", fieldName);
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
