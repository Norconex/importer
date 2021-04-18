// Written by Tom Pritchard (tom.pritchard1@ucalgary.ca), April 2021

package com.norconex.importer;

import java.io.File;
import java.io.FileOutputStream;
import com.norconex.importer.doc.Doc;

public class PropertiesOutput extends MetaDataOutput {
    
    public void setFile(StringBuilder path) {
        metaFile = new File(path.toString() + ".properties");
    }

    public void storeFile(Doc doc) {
        doc.getMetadata().storeToProperties(metaOut);
    }
}