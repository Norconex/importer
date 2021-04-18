// Written by Tom Pritchard (tom.pritchard1@ucalgary.ca), April 2021
//
// Handles metadata outputs in JSON format

package com.norconex.importer;

import java.io.File;
import java.io.FileOutputStream;
import com.norconex.importer.doc.Doc;

public class JSONOutput extends MetaDataOutput {
    
    public void setFile(StringBuilder path) {
        metaFile = new File(path.toString() + ".json");
    }

    public void storeFile(Doc doc) {
        doc.getMetadata().storeToJSON(metaOut);
    }
}
