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
import java.math.BigDecimal;
import java.text.BreakIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractCharStreamTagger;

/**
 * Analyzes the content of the supplied document and adds statistical
 * information about its content or field as metadata fields.  Default
 * behavior provide the statistics about the content. Refer to the following
 * for the new metadata fields to be created along with their description.
 * <p />
 * <table border="1">
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
 * You can specify a field name to obtain statistics about that field instead. 
 * When you do so, the field name will be inserted in the above
 * names, right after "document.stat.". E.g.:
 * <code>document.stat.myfield.characterCount</code>
 * <p />
 * Can be used both as a pre-parse (text-only) or post-parse handler.
 * <p>
 * XML configuration usage:
 * <p />
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TextStatisticsTagger"
 *          fieldName="(optional field name instead of using content)" /&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("nls")
public class TextStatisticsTagger extends AbstractCharStreamTagger 
        implements IXMLConfigurable {

    private static final long serialVersionUID = -8403612827025724376L;

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
    public void loadFromXML(Reader in) throws IOException {
        try {
            XMLConfiguration xml = ConfigurationLoader.loadXML(in);
            setFieldName(xml.getString("[@fieldName]", getFieldName()));
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
            if (StringUtils.isNotBlank(fieldName)) {
                writer.writeAttribute("fieldName", fieldName);
            }
            super.saveToXML(writer);
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

}
