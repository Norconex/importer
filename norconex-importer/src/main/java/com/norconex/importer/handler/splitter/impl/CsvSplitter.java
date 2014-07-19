/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.splitter.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.doc.Content;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.handler.splitter.AbstractCharStreamSplitter;

/**
 * Split Coma-Separated Files or Tab-Separated Files into one document per row. 
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler on text 
 * documents.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.splitter.impl.CsvSplitter"&gt;
 *      &lt;contentTypeRegex&gt;
 *          (regex to identify text content-types for pre-import, 
 *           overriding default)
 *      &lt;/contentTypeRegex&gt;
 *      &lt;restrictTo
 *              caseSensitive="[false|true]" &gt;
 *              property="(name of header/metadata name to match)"
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *     TODO : have a way to overwrite content types or regex on URL to accept/reject
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class CsvSplitter extends AbstractCharStreamSplitter
        implements IXMLConfigurable {

    //TODO do not releases before having "conditionals" implemented.
    
    private static final long serialVersionUID = 1818346309740398828L;

    //Document and enforce that default accepted content types are 
    // csv and tsv.
    
    //--- Fields to define ---
    //fileFormat = "csv" or "tsv"
    //useFirstRowAsFields = false|true
    //fieldNames = []
    //referenceColumnIndex = <the column holding reference value.
    //default = "column1", "column2", etc.
    
    //TODO add to base class a protected method that has the strategy
    //for creating references, default is to append to parent with !
    // and also setting common fields such as "parent-reference"
    
    //TODO add to base class the automated setting of parent elements to child
    // make optional to transfer all properties as is, or prefix them all,
    // except for document.parent.reference which should remain as is.
    
    @Override
    protected List<ImporterDocument> splitTextDocument(
            String reference, Reader input, Writer output, 
            ImporterMetadata metadata, boolean parsed) 
                    throws IOException {
        List<ImporterDocument> rows = new ArrayList<>();

        //TODO have all configuration options from open CVS.
        //TODO by default (or as an option), try to detect the format of the 
        // file (read first few lines and count number of tabs vs coma, 
        // quotes per line, etc.
        CSVReader cvsreader = new CSVReader(input, ',', '"');
        String [] cols;
        int count = 0;
        while ((cols = cvsreader.readNext()) != null) {
            count++;
            ImporterMetadata childMeta = new ImporterMetadata();
            childMeta.load(metadata);
            String childRef = reference + "!csv-" + count;
            
            // replace csv by file format or reference field
            ImporterDocument childDoc = new ImporterDocument(
                    childRef, new Content(), childMeta); 
            childMeta.setDocumentReference(childRef);
            childMeta.setParentReference(reference);
            
            for (int i = 0; i < cols.length; i++) {
                String col = cols[i];
                childMeta.setString("column" + (i + 1), col);
            }
            rows.add(childDoc);
        }
        IOUtils.closeQuietly(cvsreader);
        return rows;
    }

    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
//        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
        
        super.loadFromXML(xml);
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("splitter");
            writer.writeAttribute("class", getClass().getCanonicalName());

            super.saveToXML(writer);
            
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

}
