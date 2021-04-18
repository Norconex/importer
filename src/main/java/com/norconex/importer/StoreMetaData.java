// Written by Tom Pritchard (tom.pritchard1@ucalgary.ca), April 2021

package com.norconex.importer;

import com.norconex.importer.StoreMetaDataFactory;
import com.norconex.importer.doc.Doc;

public class StoreMetaData {
    public void storeOutput(Doc doc, StringBuilder path, String outputFormat) {
        StoreMetaDataFactory factory = new StoreMetaDataFactory();
        MetaDataOutput output = factory.getOutputType(outputFormat);
        output.setFile(path);
        output.setOutStream();
        output.storeFile(doc);
    }
}