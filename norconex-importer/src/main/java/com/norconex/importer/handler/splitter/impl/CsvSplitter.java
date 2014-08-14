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
import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.handler.splitter.AbstractCharStreamSplitter;

/**
 * Split files with Coma-Separated values (or any other characters, like tab) 
 * into one document per line. 
 * </p>
 * <p>Can be used both as a pre-parse (text documents) or post-parse handler
 * documents.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.splitter.impl.CsvSplitter"
 *          separatorCharacter=""
 *          quoteCharacter=""
 *          escapeCharacter=""
 *          useFirstRowAsFields="(false|true)"
 *          linesToSkip="(integer)" &gt;
 *      &lt;contentTypeRegex&gt;
 *          (regex to identify text content-types for pre-import, 
 *           overriding default)
 *      &lt;/contentTypeRegex&gt;
 *      &lt;restrictTo
 *              caseSensitive="[false|true]" &gt;
 *              property="(name of header/metadata name to match)"
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class CsvSplitter extends AbstractCharStreamSplitter
        implements IXMLConfigurable {

    private static final long serialVersionUID = 1818346309740398828L;

    public static final char DEFAULT_SEPARATOR_CHARACTER = ',';
    public static final char DEFAULT_QUOTE_CHARACTER = '"';
    public static final char DEFAULT_ESCAPE_CHARACTER = '\\';
    
    //--- Fields to define ---
    //TODO add to base class for most/all splitters with a protected method 
    // that has the strategy
    // for creating references, default is to append to parent with !
    // and also setting common fields such as "parent-reference"
    
    //TODO add to base class the automated setting of parent elements to child
    // make optional to transfer all properties as is, or prefix them all,
    // except for document.parent.reference which should remain as is.
    
    private char separatorCharacter = DEFAULT_SEPARATOR_CHARACTER;
    private char quoteCharacter = DEFAULT_QUOTE_CHARACTER;
    private char escapeCharacter = DEFAULT_ESCAPE_CHARACTER;
    private boolean useFirstRowAsFields;
    private int linesToSkip;
    
    
    @Override
    protected List<ImporterDocument> splitTextDocument(
            String reference, Reader input, Writer output, 
            ImporterMetadata metadata, boolean parsed) 
                    throws IOException {

        List<ImporterDocument> rows = new ArrayList<>();

        //TODO by default (or as an option), try to detect the format of the 
        // file (read first few lines and count number of tabs vs coma, 
        // quotes per line, etc.
        CSVReader cvsreader = new CSVReader(input, separatorCharacter, 
                quoteCharacter, escapeCharacter, linesToSkip);
        
        String [] cols;
        int count = 0;
        while ((cols = cvsreader.readNext()) != null) {
            count++;
            ImporterMetadata childMeta = new ImporterMetadata();
            childMeta.load(metadata);
            String childRef = reference + "!child-" + count;
            
            // replace csv by file format or reference field
            ImporterDocument childDoc = 
                    new ImporterDocument(childRef, childMeta); 
            childMeta.setDocumentReference(childRef);
            childMeta.setParentReference(reference);
            childMeta.setParentRootReference(reference);
            
            String[] colNames = null;
            if (count == 1 && useFirstRowAsFields) {
                colNames = cols;
            } else {
                for (int i = 0; i < cols.length; i++) {
                    String colValue = cols[i];
                    if (colNames == null || i >= colNames.length) {
                        childMeta.setString("column" + (i + 1), colValue);
                    } else {
                        childMeta.setString(colNames[i], colValue);
                    }
                }
                rows.add(childDoc);
            }
        }
        IOUtils.closeQuietly(cvsreader);
        return rows;
    }

    /**
     * Gets the value-separator character. 
     * @return value-separator character
     */
    public char getSeparatorCharacter() {
        return separatorCharacter;
    }
    /**
     * Sets the value-separator character. Default is a comma (,).
     * @param separatorCharacter value-separator character
     */
    public void setSeparatorCharacter(char separatorCharacter) {
        this.separatorCharacter = separatorCharacter;
    }

    /**
     * Get the value's surrounding quotes character.
     * @return value's surrounding quotes character
     */
    public char getQuoteCharacter() {
        return quoteCharacter;
    }
    /**
     * Sets the value's surrounding quotes character.  Default is the 
     * double-quote character (").
     * @param quoteCharacter value's surrounding quotes character
     */
    public void setQuoteCharacter(char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
    }

    /**
     * Gets the escape character.
     * @return escape character
     */
    public char getEscapeCharacter() {
        return escapeCharacter;
    }
    /**
     * Sets the escape character.  Default is the backslash character (\).
     * @param escapeCharacter escape character
     */
    public void setEscapeCharacter(char escapeCharacter) {
        this.escapeCharacter = escapeCharacter;
    }

    /**
     * Whether to use the first row as field names for values.
     * @return <code>true</code> if using first row as field names.
     */
    public boolean isUseFirstRowAsFields() {
        return useFirstRowAsFields;
    }
    /**
     * Sets whether to use the first row as field names for values.
     * Default is <code>false</code>.
     * @param useFirstRowAsFields <code>true</code> if using first row as 
     *        field names
     */
    public void setUseFirstRowAsFields(boolean useFirstRowAsFields) {
        this.useFirstRowAsFields = useFirstRowAsFields;
    }

    /**
     * Gets how many lines to skip before starting to parse lines.
     * @return how many lines to skip
     */
    public int getLinesToSkip() {
        return linesToSkip;
    }
    /**
     * Sets how many lines to skip before starting to parse lines. 
     * Default is 0.
     * @param linesToSkip how many lines to skip
     */
    public void setLinesToSkip(int linesToSkip) {
        this.linesToSkip = linesToSkip;
    }

    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
        setSeparatorCharacter(loadCharacter(
                xml, "[@separatorCharacter]", separatorCharacter));
        setQuoteCharacter(loadCharacter(
                xml, "[@quoteCharacter]", quoteCharacter));
        setQuoteCharacter(loadCharacter(
                xml, "[@escapeCharacter]", escapeCharacter));
        setUseFirstRowAsFields(xml.getBoolean("useFirstRowAsFields", false));
        setLinesToSkip(xml.getInt("linesToSkip", linesToSkip));
        super.loadFromXML(xml);
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("splitter");
            writer.writeAttribute("class", getClass().getCanonicalName());
            
            writer.writeAttribute(
                    "separatorCharacter", String.valueOf(separatorCharacter));
            writer.writeAttribute(
                    "quoteCharacter", String.valueOf(quoteCharacter));
            writer.writeAttribute(
                    "escapeCharacter", String.valueOf(escapeCharacter));
            writer.writeAttribute(
                    "useFirstRowAsFields", String.valueOf(useFirstRowAsFields));
            writer.writeAttribute("linesToSkip", String.valueOf(linesToSkip));
            
            super.saveToXML(writer);
            
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

    private char loadCharacter(
            XMLConfiguration xml, String key, char defaultCharacter) {
        String character = xml.getString(key, null);
        if (StringUtils.isEmpty(character)) {
            return defaultCharacter;
        }
        if (character.length() > 1) {
            throw new ConfigurationException(
                    "\"" + key + "\" value can only be a single character. "
                  + "Value: " + character);
        }
        return character.charAt(0);
    }
}
