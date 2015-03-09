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
package com.norconex.importer.parser.impl.quattro;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.parser.impl.wordperfect.WPInputStream;

/**
 * Extracts text from a Quattro Pro document according to QPW v9 File Format.
 * This format appears to be compatible with more recent versions too.
 * @author Pascal Essiembre
 * @since 2.1.0
 */
public class QPWTextExtractor {

    public static final String META_CREATOR = "creator";
    public static final String META_LAST_USER = "last-user";
    
    private static final Logger LOG = 
            LogManager.getLogger(QPWTextExtractor.class);
    
    private static final String OLE_DOCUMENT_NAME = "NativeContent_MAIN";

    private enum Extractor{
        IGNORE { @Override public void extract(Context ctx) throws IOException {
            ctx.in.skipWPByte(ctx.bodyLength);
        }},
        BOF { @Override public void extract(Context ctx) throws IOException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("QuattroPro id=" + ctx.in.readWPString(4)
                        + "; Version=" + ctx.in.readWPShort()
                        + "; Build=" + ctx.in.readWPShort()
                        + "; Last saved bits=" + ctx.in.readWPShort()
                        + "; Lowest version=" + ctx.in.readWPShort()
                        + "; Number of pages=" + ctx.in.readWPShort());
                ctx.in.skipWPByte(ctx.bodyLength - 14);
            } else {
                ctx.in.skipWPByte(ctx.bodyLength);
            }
        }},
        USER { @Override public void extract(Context ctx) throws IOException {
            addMeta(ctx, META_CREATOR, getQstrLabel(ctx.in));
            addMeta(ctx, META_LAST_USER, getQstrLabel(ctx.in));
        }},
        EXT_LINK { @Override public void extract(Context ctx) 
                throws IOException {
            ctx.in.readWPShort(); // index
            ctx.in.readWPShort(); // page first
            ctx.in.readWPShort(); // page last
            ctx.out.write(getQstrLabel(ctx.in));
            ctx.out.write(System.lineSeparator());
        }},
        STRING_TABLE { @Override public void extract(Context ctx) 
                throws IOException {
            long entries = ctx.in.readWPLong();
            ctx.in.readWPLong();  // Total used
            ctx.in.readWPLong();  // Total saved
            for (int i = 0; i < entries; i++) {
                ctx.out.write(getQstrLabel(ctx.in));
            }
            ctx.out.write(System.lineSeparator());
        }},
        BOS { @Override public void extract(Context ctx) throws IOException {
            ctx.in.readWPShort(); // sheet #
            ctx.in.readWPShort(); // first col index
            ctx.in.readWPShort(); // last col index
            ctx.in.readWPLong();  // first row index
            ctx.in.readWPLong();  // last row index
            ctx.in.readWPShort(); // format
            ctx.in.readWPShort(); // flags
            ctx.out.write(getQstrLabel(ctx.in));
            ctx.out.write(System.lineSeparator());
        }},
        SHEET_HEADFOOT { @Override public void extract(Context ctx) 
                throws IOException {
            ctx.in.readWPShort(); // flag
            ctx.out.write(getQstrLabel(ctx.in));
            ctx.out.write(System.lineSeparator());
        }},


        
        DEBUG { @Override public void extract(Context ctx) throws IOException {
            System.out.println("REC:" + ctx.in.readWPString(ctx.bodyLength));
        }},
        
        ;
        public abstract void extract(Context ctx) throws IOException;
    }
    
    // Holds extractors for each record types we are interested in.
    // All record types not defined here will be skipped.
    private static final Map<Integer, Extractor> EXTRACTORS = 
            new HashMap<Integer, Extractor>();
    static {
        //--- Global Records ---
        EXTRACTORS.put(0x0001, Extractor.BOF);     // Beginning of file
        EXTRACTORS.put(0x0005, Extractor.USER);    // User

        //--- Notebook Records ---
        EXTRACTORS.put(0x0403, Extractor.EXT_LINK);// External link
        EXTRACTORS.put(0x0407, Extractor.STRING_TABLE); // String table

        //--- Sheet Records ---
        EXTRACTORS.put(0x0601, Extractor.BOS); // Beginning of sheet
        EXTRACTORS.put(0x0605, Extractor.SHEET_HEADFOOT); // Sheet header
        EXTRACTORS.put(0x0606, Extractor.SHEET_HEADFOOT); // Sheet footer
    }
    
    class Context {
        private final WPInputStream in;
        private final Writer out;
        private final ImporterMetadata metadata;
        private int type;
        private int bodyLength;
        public Context(
                WPInputStream in, Writer out, ImporterMetadata metadata) {
            super();
            this.in = in;
            this.out = out;
            this.metadata = metadata;
        }
    }
    
    public void extract(
            InputStream input, Writer out, ImporterMetadata metadata)
                    throws IOException {
        //TODO shall we validate and throw warning/error if the file does not 
        //start with a BOF and ends with a EOF?
        try (WPInputStream in = new WPInputStream(new POIFSFileSystem(
                input).createDocumentInputStream(OLE_DOCUMENT_NAME))) {
            Context ctx = new Context(in, out, metadata);
            while (hasNext(in)) {
                ctx.type = in.readWPShort();
                ctx.bodyLength = in.readWPShort();
                Extractor extractor = EXTRACTORS.get(ctx.type);
                if (extractor != null) {
                    extractor.extract(ctx);
                } else {
                    Extractor.IGNORE.extract(ctx);
                }
            }
        }
    }
    
    
    private boolean hasNext(InputStream in) throws IOException {
        try {
            in.mark(1);
            return in.read() != -1;
        } finally {
            in.reset();
        }
    }
    
    private static void addMeta(Context ctx, String key, String value)
                throws IOException {
        if (StringUtils.isNotBlank(value)) {
            ctx.metadata.addString(key, value.trim());
        }
    }
    
    private static String getQstrLabel(WPInputStream in) throws IOException {
        // QSTR
        int count = in.readWPShort();
        in.readWPByte(); // string type
        char[] text = new char[count+1];
        text[0] = in.readWPChar();

        // QSTR
        for (int i = 0; i < count; i++) {
            text[i+1] = in.readWPChar();
        }
        return new String(text);
    }

}
