// Written by Tom Pritchard (tom.pritchard1@ucalgary.ca), April 2021
//
// Handles metadata outputs in XML format

package com.norconex.importer;

import java.io.File;
import java.io.FileOutputStream;
import com.norconex.importer.doc.Doc;

public class XMLOutput extends MetaDataOutput {
    
    public void setFile(StringBuilder path) {
        metaFile = new File(path.toString() + ".xml");
    }

    public void storeFile(Doc doc) {
        doc.getMetadata().storeToXML(metaOut);
    }
}