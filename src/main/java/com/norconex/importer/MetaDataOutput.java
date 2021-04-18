// Written by Tom Pritchard (tom.pritchard1@ucalgary.ca), April 2021

package com.norconex.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import com.norconex.importer.doc.Doc;

public abstract class MetaDataOutput {
    protected File metaFile;
    protected FileOutputStream metaOut;

    protected abstract void setFile(StringBuilder path);
    protected abstract void storeFile(Doc doc);
    public void setOutStream() {
        try {
            metaOut = new FileOutputStream(metaFile);
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file: " + metaFile);
            e.printStackTrace(System.err);
            System.err.println();
            System.err.flush();
        }
    }
}
