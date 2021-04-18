// Written by Tom Pritchard (tom.pritchard1@ucalgary.ca), April 2021
//
// Creates output type based on outputFormat

package com.norconex.importer;

public class StoreMetaDataFactory {
    public MetaDataOutput getOutputType(String outputFormat) {
        if ("json".equalsIgnoreCase(outputFormat))
            return new JSONOutput();
        else if ("xml".equalsIgnoreCase(outputFormat))
            return new XMLOutput();
        else
            return new PropertiesOutput();
    }
}
