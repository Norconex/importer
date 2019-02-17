/* Copyright 2015-2019 Norconex Inc.
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
package com.norconex.importer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.io.ByteArrayOutputStream;
import com.norconex.commons.lang.io.CachedInputStream;

/**
 * Character set utility methods.
 * @author Pascal Essiembre
 * @since 2.5.0
 */
public final class CharsetUtil {

    private static final Logger LOG =
            LoggerFactory.getLogger(CharsetUtil.class);

    /**
     * Constructor.
     */
    private CharsetUtil() {
    }

    /**
     * Converts the character encoding of the supplied input value.
     * @param input input value to apply conversion
     * @param inputCharset character set of the input value
     * @param outputCharset desired character set of the output value
     * @return the converted value
     * @throws IOException problem converting character set
     */
    public static String convertCharset(
            String input, String inputCharset,
            String outputCharset) throws IOException {
        try (ByteArrayInputStream is =
                new ByteArrayInputStream(input.getBytes(inputCharset));
                ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            convertCharset(is, inputCharset, os, outputCharset);
            os.flush();
            return os.toString(outputCharset);
        }
    }

    /**
     * Converts the character encoding of the supplied input.
     * @param input input stream to apply conversion
     * @param inputCharset character set of the input stream
     * @param output where converted stream will be stored
     * @param outputCharset desired character set of the output stream
     * @throws IOException problem converting character set
     */
    public static void convertCharset(
            InputStream input, String inputCharset,
            OutputStream output, String outputCharset) throws IOException {
        CharsetDecoder decoder = Charset.forName(inputCharset).newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharsetEncoder encoder = Charset.forName(outputCharset).newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPLACE);
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        Reader reader = new InputStreamReader(input, decoder);
        Writer writer = new OutputStreamWriter(output, encoder);
        IOUtils.copyLarge(reader, writer);
        writer.flush();
        rewind(input);
    }

    /**
     * Detects the character encoding of a string.
     * @param input the input to detect encoding on
     * @return the character encoding official name or <code>null</code>
     *         if the input is null or blank
     * @throws IOException if there is a problem find the character encoding
     */
    public static String detectCharset(String input) throws IOException {
        return detectCharset(input, null);
    }

    /**
     * Detects the character encoding of a string. When the character
     * encoding of what the input is supposed to be is known, specifying
     * it as a declared encoding will influence the detection result.
     * @param input the input to detect encoding on
     * @param declaredEncoding declared input encoding, if known
     * @return the character encoding official name or <code>null</code>
     *         if the input is null or blank
     * @throws IOException if there is a problem find the character encoding
     */
    public static String detectCharset(
            String input, String declaredEncoding) throws IOException {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        CharsetDetector cd = new CharsetDetector();
        if (StringUtils.isNotBlank(declaredEncoding)) {
            cd.setDeclaredEncoding(declaredEncoding);
        }
        String charset = null;
        cd.enableInputFilter(true);
        cd.setText(input.getBytes("UTF-8"));
        CharsetMatch match = cd.detect();
        charset = match.getName();
        LOG.debug("Detected encoding: {}", charset);
        return charset;
    }

    /**
     * Detects the character encoding of an input stream.
     * {@link InputStream#markSupported()} must return <code>true</code>
     * otherwise no decoding will be attempted.
     * @param input the input to detect encoding on
     * @return the character encoding official name or <code>null</code>
     *         if input is null
     * @throws IOException if there is a problem find the character encoding
     */
    public static String detectCharset(InputStream input) throws IOException {
        return detectCharset(input, null);
    }

    /**
     * Detects the character encoding of an input stream. When the character
     * encoding of what the input is supposed to be is known, specifying
     * it as a declared encoding will influence the detection result.
     * {@link InputStream#markSupported()} must return <code>true</code>
     * otherwise no decoding will be attempted.
     * @param input the input to detect encoding on
     * @param declaredEncoding declared input encoding, if known
     * @return the character encoding official name or <code>null</code>
     *         if input is null
     * @throws IOException if there is a problem find the character encoding
     */
    public static String detectCharset(
            InputStream input, String declaredEncoding) throws IOException {
        if (input == null) {
            return null;
        }
        if (!input.markSupported()) {
            LOG.warn("mark/reset not supported on input stream. "
                    + "Will not attempt to detect encoding.");
            return declaredEncoding;
        }

        CharsetDetector cd = new CharsetDetector();
        if (StringUtils.isNotBlank(declaredEncoding)) {
            cd.setDeclaredEncoding(declaredEncoding);
        }
        String charset = null;
        cd.enableInputFilter(true);
        cd.setText(input);
        rewind(input);
        CharsetMatch match = cd.detect();
        charset = match.getName();
        LOG.debug("Detected encoding: {}", charset);
        return charset;
    }

    private static void rewind(InputStream is) throws IOException {
        //TODO investigate why regular reset on CachedInputStream has
        //no effect and returns an empty stream when read again. Fix that
        //instead of having this method.
        if (is instanceof CachedInputStream) {
            ((CachedInputStream) is).rewind();;
        }
    }
}
