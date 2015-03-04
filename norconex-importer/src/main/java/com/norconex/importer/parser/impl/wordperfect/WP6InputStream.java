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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.norconex.commons.lang.io.IOUtil;

/**
 * {@link InputStream} wrapper adding WordPerfect-specific byte-reading methods.
 * @author Pascal Essiembre
 * @since 2.1.0
 */
public class WP6InputStream extends InputStream {

    private final DataInputStream in;
    
    /**
     * Constructor.
     * @param in input stream
     */
    public WP6InputStream(InputStream in) {
        this.in = new DataInputStream(IOUtil.toBufferedInputStream(in));
    }

    /**
     * Reads a WordPerfect "short": a 2 bytes (16-bit) unsigned value in 
     * reverse order.
     * @return an integer value
     * @throws IOException if not enough bytes remain
     */
    public int readWPShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch2 << 8) + (ch1 << 0);
    }

    /**
     * Reads a WordPerfect "long": a 4 bytes (32-bit) unsigned value in 
     * reverse order.
     * @return a long value
     * @throws IOException if not enough bytes remain
     */
    public long readWPLong() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0)); 
    }

    /**
     * Reads a WordPerfect byte (8-bit).
     * @return byte value
     * @throws IOException if not enough bytes remain
     */
    public byte readWPByte() throws IOException {
        return in.readByte();
    }

    /**
     * Reads a WordPerfect character (8-bit).
     * @return character
     * @throws IOException if not enough bytes remain
     */
    public char readWPChar() throws IOException {
        return (char) in.read();
    }

    /**
     * Reads a WordPerfect string of specified length (1 byte per character).
     * @param length how many characters to read
     * @return character
     * @throws IOException if not enough bytes remain
     */
    public String readWPString(int length) throws IOException {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            int c = in.read();
            if (c == -1) {
                throw new EOFException();
            }
            chars[i] = (char) c;
        }
        return new String(chars);
    }
    
    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
