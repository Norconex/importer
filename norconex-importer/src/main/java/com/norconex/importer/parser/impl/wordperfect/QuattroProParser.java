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
package com.norconex.importer.parser.impl.wordperfect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;


/**
 * Parser for Quattro Pro documents version 9 (and possibly others).
 * Only text from the body is extracted, and no metadata is added by this 
 * parser.
 * @author Pascal Essiembre 
 * @since 2.1.0
 */
//NOTE: Excluded from Sonar coverage calculations.
public class QuattroProParser implements IDocumentParser {

    private static final String[] QUATTRO_TERMS = {
        "ActivePagename", "PreviousCell", "SelectedVersion", "CurrentVersion",
        "NameExists", "DeleteVersion", "_AddVersion", "_SortGroup",
        "ConfirmPrompt", "_ShowRecallDlg", "OKConfirmMacro", "_ShowConfirmDlg",
        "ShowStoreAsDlg", "_StoreCurrentPointer", "_GotoPreviousCell",
        "SortDirection", "DeletePressed", "_ReplaceVersion", "_RecallVersion",
        "_ToggleHighlight", "IsHighlight", "Retval2", "IsValidName", 
        "VersionList", "_RecallSample", "_RecallEmpty", "_NBSTARTMACRO",
        "Context", "IsValidContext", "IsQuickBar", "_SetVersionIndicator",
        "ListName", "NameToVerify", "RedrawState", "_SetRedraw",
        "_RetryPrintPage", "_CreateNamedSetting", "ConfirmBtnNo", "QTVersion",
        "QPW9", "_NBSTARTMACRO", "_NBEXITMACRO", "_GetHelpContext", 
        "_PrintActivePage", "_PreviewActivePage", "_Print",
        "_RetryPrintPage", "_CreateNamedSetting", "_StoreCurrentPointer",
        "_ToggleHighlight2",
        "_SortGroup", 
        "ConfirmBtnNo", "ConfirmPrompt", "QPWVersion", "CurrentVersion",
        "DeletePressed", "IsHighlight", "Region2Exists", "RedrawSetting",
        "QuickBarName1", "QuickBarName", "EmptyName", "SampleName", "Delete",
        "Replace", "OKBtnText", "CancelBtnText", "StoreAsText", "RecallText",
        "OKExit", "Preview?", "Comma0", "Currency0", "MacroCode", "_Home"
        
    };
    private static final String[] FONT_NAMES = {
        "Arial", "Helvetica", "System", "Wingdings", "Times New Roman", 
        "Lucida Sans Typewriter", "Lucida Bright", "Courier New"
    };
    
    private static final String[] PHRASES = {
        "Normal Comma Currency Percent Fixed Date Title BlockName Total",
        "> Heading <",
        "Exit macro Help Printing Utilities",
        "# Value #",
        "Label_Text #",
        "{CALC}{",
        "@Property(",
        "Startup macro",
        "Block Name"
    };
    
    
    @Override
    public List<ImporterDocument> parseDocument(ImporterDocument doc,
            Writer output) throws DocumentParserException {
        try {
            parse(doc.getContent(), output);
        } catch (IOException e) {
            throw new DocumentParserException(e);
        }
        return null;
    }
    
    private void parse(InputStream in, Writer out) throws IOException {
        BufferedReader buffer = new BufferedReader(
                new InputStreamReader(in, CharEncoding.US_ASCII));
        StringBuilder b = new StringBuilder();
        int c = 0;
        int lastLineStartIndex = 0;
        
        while((c = buffer.read()) != -1) {
            
            if (isMerging(b, c)) {
                // do not write this end of line character
                continue;
            }
            if (isLineEnd(c)) {
                String line = b.substring(lastLineStartIndex);
                if (!isValidLine(line)) {
                    b.setLength(lastLineStartIndex);
                } else {
                    String fixedLine = fixLine(line);
                    if (!Objects.equal(fixedLine, line)) {
                        b.setLength(lastLineStartIndex);
                        b.append(fixedLine);
                    }
                }
                lastLineStartIndex = b.length();
            }
            
            if (isTextCharacter(c)) {
                b.append((char) c);
            } else {
                spaceIt(b);
            }
            
        }
        
        if (lastLineStartIndex != b.length()) {
            String line = b.substring(lastLineStartIndex);
            if (!isValidLine(line)) {
                b.setLength(lastLineStartIndex);
            } else {
                String fixedLine = fixLine(line);
                if (!Objects.equal(fixedLine, line)) {
                    b.setLength(lastLineStartIndex);
                    b.append(fixedLine);
                }
            }
            
        }
        
        out.write(b.toString());
        out.flush();
    }
    
    private String fixLine(String line) {
        String[] words = StringUtils.split(line);
        StringBuilder b = new StringBuilder();
        for (String word : words) {
            boolean goodWord = true;
            for (String qproTerm : QUATTRO_TERMS) {
                if (Objects.equal(qproTerm, word)) {
                    goodWord = false;
                    continue;
                }
            }
            if (goodWord) {
                goodWord = isNormalWord(word);
            }
            if (goodWord) {
                b.append(word);
                b.append(' ');
            }
        }
        
        String fixedLine = b.toString();
        
        // remove 3 consecutive single characters
        fixedLine = fixedLine.replaceAll("\\s+\\S\\s+\\S\\s+\\S\\s+", "");
        
        return fixedLine.trim() + '\n';
    }
    
    private boolean isValidLine(String line) {
        int count = 0;
        String[] lineTokens = null;

        // exact term match (strip starting `)
        lineTokens = StringUtils.split(line);
        if (lineTokens.length == 0) {
            return false;
        }
        for (String lineToken : lineTokens) {
            for (String term : QUATTRO_TERMS) {
                if (StringUtils.removeStart(lineToken, "`").equals(term)) {
                    count++;
                    if (!isValidMatchCount(lineTokens.length, count)) {
                        return false;
                    }
                }
            }
        }
        
        // "contains" match
        lineTokens = StringUtils.splitByWholeSeparator(line, "  ");
        for (String lineToken : lineTokens) {
            for (String term : FONT_NAMES) {
                if (lineToken.contains(term)) {
                    count++;
                    if (!isValidMatchCount(lineTokens.length, count)) {
                        return false;
                    }
                }
            }
        }

        // reject lines with 50%+ single chars or 75% single or double chars
        count = 0;
        int withDouble = 0;
        //line.split("[^\\w\\.-]");
        lineTokens = StringUtils.split(StringUtils.trim(line));
        for (String token : lineTokens) {
            if (token.length() == 1) {
                count++;
                withDouble++;
            } else if (token.length() == 2) {
                withDouble++;
            }
        }
        if (count >= (lineTokens.length) / 2
                || withDouble >= (lineTokens.length * 3) / 4) {
            return false;
        }

        // Phrases
        String plainLine = line.replaceAll("\\s+", " ");
        for (String phrase : PHRASES) {
            if (StringUtils.contains(plainLine, phrase)) {
                return false;
            }
        }
        
        // reject lines with 4 or more consecutive characters somewhere in it.
        Pattern p = Pattern.compile("([a-zA-Z])\\1{3,}");
        Matcher m = p.matcher(line);
        if (m.find()) {
            return false;
        }
        
        return true;
    }
    
    private boolean isValidMatchCount(int wordCount, int matchCount) {
        int  maxMatch = 3;
        return !(wordCount >= 1 && wordCount <= 3 && matchCount >= 1)
                && !(wordCount > 3 && wordCount <= 6 && matchCount >= 2)
                && !(matchCount >= maxMatch);
    }
    
    private boolean isMerging(StringBuilder b, int c) {
        return isLineEnd(c) && b.length() >= 5 
                && b.substring(b.length() -5).equals("  @  ");
    }
    
    private void spaceIt(StringBuilder b) {
        int length = b.length();
        
        if (length < 2 || !(b.charAt(length -1) == ' '
                && b.charAt(length -2) == ' ')) {
            b.append(' ');
        }
    }
    

    // overrides StringExtractor.isTextCharacter
   private boolean isTextCharacter(int charNumber) {
               // readable ASCII characters
        return charNumber >= 32 && charNumber <= 126 
                // letters with accents, currency symbols, etc.
                || charNumber >= 128 && charNumber <= 168
                // tab
                || charNumber == 9 
                // accented ANSI character
                || charNumber >= 0xC0 && charNumber <= 0xFF
                // backquote
                || charNumber == 0x91 
                // quote
                || charNumber == 0x92
                
                // line feed
                || charNumber == 0x0A
                // carriage return
                || charNumber == 0x0D;
    }
   
    private boolean isLineEnd(int charNumber) {
            // line feed
        return charNumber == 0x0A
            // carriage return
            || charNumber == 0x0D;
    }
   
    
    private boolean isNormalWord(String word) {
        boolean result = false;

        int wordLength = word.length();

        if (wordLength > 0) {
            char lastChar = word.charAt(wordLength - 1);
            if (lastChar == '.' || lastChar == ',' 
                    || lastChar == '?' || lastChar == '!'
                    || lastChar == '…' || lastChar == ')'
                    || lastChar == ':' || lastChar == ';') {
                wordLength--;
            }
        }

        if (wordLength >= 2) {
            result = true;

            // check if the word contains non-letter characters
            for (int i = 0; i < wordLength && result == true; i++) {
                if (!Character.isLetter(word.charAt(i))) {
                    result = false;
                }
            }

            // Check use of upper- and lower case. Note that we can't check for
            // lower case, as some non-Latin characters
            // do not have a case at all.
            // Allowed patterns are:
            // - all upper case
            // - none upper case
            // - first character upper case, rest not upper case
            if (Character.isUpperCase(word.charAt(0))) {
                if (Character.isUpperCase(word.charAt(1))) {
                    // all upper case?
                    for (int i = 2; i < wordLength && result == true; i++) {
                        result = Character.isUpperCase(word.charAt(i));
                    }
                } else {
                    // rest not upper case?
                    for (int i = 2; i < wordLength && result == true; i++) {
                        result = !Character.isUpperCase(word.charAt(i));
                    }
                }
            } else {
                // all not upper case?
                for (int i = 0; i < wordLength && result == true; i++) {
                    result = !Character.isUpperCase(word.charAt(i));
                }
            }
        }
        return result;
    }
}
