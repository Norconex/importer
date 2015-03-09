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

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

/**
 * Extracts text from a WordPerfect document according to WP6 File Format.
 * This format appears to be compatible with more recent versions too.
 * @author Pascal Essiembre
 * @since 2.1.0
 */
public class WP6TextExtractor {

    public void extract(InputStream input, Writer out) 
            throws IOException {
        WPInputStream in = new WPInputStream(input);
        WP6FileHeader header = parseFileHeader(in);

        // For text extraction we can safely ignore WP Index Area and
        // Packet Data Area and jump right away to Document Area.
        extractDocumentText(in, header.getDocAreaPointer(), out);
    }

    private void extractDocumentText(
            WPInputStream in, long offset, Writer out) 
                    throws IOException {
        
        // Move to offset (for some reason skip() did not work).
        for (int i = 0; i < offset; i++) {
            in.readWPByte();
        }

        int c;
        while ((c = in.read()) != -1) {
            if (c > 0 && c <= 32) {
                out.write(WP6Constants.DEFAULT_EXTENDED_INTL_CHARS[c]);
            } else if (c >= 33 && c <= 126) {
                out.write((char) c);
            } else if (c == 128) {
                out.write(' ');      // Soft space
            } else if (c == 129) {
                out.write('\u00A0'); // Hard space
            } else if (c == 129) {
                out.write('-');      // Hard hyphen
            } else if (c == 135 || c == 137) {
                out.write('\n');      // Dormant Hard return
            } else if (c == 138) {
                // skip to closing pair surrounding page number
                skipUntilChar(in, 139);
            } else if (c == 198) {
                // end of cell
                out.write('\t');
            } else if (c >= 180 && c <= 207) {
                out.write('\n');
            } else if (c >= 208 && c <= 239) {
                // Variable-Length Multi-Byte Functions
                int subgroup = in.read();
                int functionSize = in.readWPShort();
                for (int i = 0; i < functionSize - 4; i++) {
                    in.read(); 
                }
                
                // End-of-Line group
                if (c == 208) {
                    if (subgroup >= 1 && subgroup <= 3) {
                        out.write(' ');
                    } else if (subgroup == 10) {
                        // end of cell
                        out.write('\t');
                    } else if (subgroup >= 4 && subgroup <= 19) {
                        out.write('\n');
                    } else if (subgroup >= 20 && subgroup <= 22) {
                        out.write(' ');
                    } else if (subgroup >= 23 && subgroup <= 28) {
                        out.write('\n');
                    }
                } else if (c == 213) {
                    out.write(' ');
                } else if (c == 224) {
                    out.write('\t');
                }
                //TODO Are there functions containing data? Like footnotes?
                
            } else if (c == 240) {
                // extended char
                int charval = in.read();
                int charset = in.read();
                in.read(); // closing character
  
                //TODO implement all charsets
                if (charset == 4 || charset == 5) {
                    out.write(
                            WP6Constants.EXTENDED_CHARSETS[charset][charval]);
                } else {
                    out.write("[TODO:charset" + charset + "]");
                }
            } else if (c >= 241 && c <= 254) {
                skipUntilChar(in, c);
            } else if (c == 255) {
                skipUntilChar(in, c);
            }
        }
        
        // Ignored codes above 127:
        
        // 130,131,133: soft hyphens
        // 134: invisible return in line
        // 136: soft end of center/align
        // 140: style separator mark
        // 141,142: start/end of text to skip
        // 143: exited hyphenation
        // 144: cancel hyphenation
        // 145-151: match functions
        // 152-179: unknown/ignored
        // 255: reserved, cannot be used
        
        out.flush();
    }

    // Skips until the given character is encountered.
    private int skipUntilChar(WPInputStream in, int targetChar)
            throws IOException {
        int count = 0;
        int c;
        while ((c = in.read()) != -1) {
            count++;
            if (c == targetChar) {
                return count;
            }
        }
        return count;
    }
    
    private WP6FileHeader parseFileHeader(WPInputStream in) 
            throws IOException {
        WP6FileHeader header = new WP6FileHeader();

        // File header
        in.mark(30);
        header.setFileId(in.readWPString(4));         // 1-4
        header.setDocAreaPointer(in.readWPLong());    // 5-8
        header.setProductType(in.read());             // 9
        header.setFileType(in.readWPChar());          // 10
        header.setMajorVersion(in.read());            // 11
        header.setMinorVersion(in.read());            // 12
        header.setEncrypted(in.readWPShort() != 0);   // 13-14
        header.setIndexAreaPointer(in.readWPShort()); // 15-16
        try {
            in.skip(4); // 4 reserved bytes: skip     // 17-20
            header.setFileSize(in.readWPLong());      // 21-24
        } catch (IOException e) {
            // May fail if not extended error, which is fine.
        }
        in.reset();
        
        //TODO header may be shared between corel products, so move validation
        //specific to each product elsewhere?
        //TODO convert to logs only, and let it fail elsewhere?
//        if (!WP6Constants.WP6_FILE_ID.equals(header.getFileId())) {
//            throw new IOException("Not a WordPerfect file. File must start "
//                    + "with " + WP6Constants.WP6_FILE_ID + " but was "
//                    + header.getFileId());
//        }
//        if (WP6Constants.WP6_PRODUCT_TYPE != header.getProductType()) {
//            throw new IOException("Not a WordPerfect file. Product type "
//                    + "must be " + WP6Constants.WP6_PRODUCT_TYPE + " but was "
//                    + header.getProductType());
//        }
        //TODO perform file type validation?
        return header;
    }

}
