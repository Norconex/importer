/* Copyright 2017 Norconex Inc.
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
package com.norconex.importer.util.regex;

import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import com.norconex.commons.lang.map.Properties;

/**
 * Utility methods for various regular expression usage.
 * @author Pascal Essiembre
 * @since 2.8.0
 */
public final class RegexUtil {
//TODO consider moving to Norconex Commons Lang?
//TODO have a PatternBuilder?
    
    private RegexUtil() {
    }

    /**
     * Compiles a case insensitive "dotall" pattern 
     * (dots match all, including new lines).
     * @param regex regular expression
     * @return compiled pattern
     */
    public static Pattern compileDotAll(String regex) {
        return compileDotAll(regex, false);
    }
    /**
     * Compiles a case insensitive "dotall" pattern 
     * (dots match all, including new lines).
     * @param regex regular expression
     * @param caseSensitive <code>true</code> to match character case.
     * @return compiled pattern
     */
    public static Pattern compileDotAll(String regex, boolean caseSensitive) {
        // we allow empty regex here, but not null ones
        if (regex == null) {
            throw new IllegalArgumentException("\"regex\" cannot be null");
        }
        int flags = Pattern.DOTALL;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return Pattern.compile(regex, flags);
    }
    
    public static void extractFields(Properties dest, 
            CharSequence text, RegexFieldExtractor... extractors) {
        if (ArrayUtils.isEmpty(extractors)) {
            return;
        }
        for (RegexFieldExtractor extractor : extractors) {
            extractor.extractFields(dest, text);
        }
    }
    
    public static Properties extractFields(
            CharSequence text, RegexFieldExtractor... patterns) {
        Properties dest = new Properties();
        extractFields(dest, text, patterns);
        return dest;
    }
}
