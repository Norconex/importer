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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * A utility tagger to help with troubleshooting of document importing.
 * Place this tagger anywhere in your handler configuration to print to 
 * the log stream the metadata fields or content so far when this handler 
 * gets invoked.
 * This handler does not impact the data being imported at all 
 * (it only reads it).  
 * <p />
 * The default behavior logs all metadata fields using the DEBUG log level.
 * You can optionally set which fields to log and whether to also log the 
 * document content or not, as well as specifying a different log level.
 * <p />
 * <b>Be careful:</b> Logging the content when you deal with very large content
 * can result in memory exceptions.
 * <p />
 * Can be used both as a pre-parse or post-parse handler.
 * <p>
 * XML configuration usage:
 * <p />
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DebugTagger"
 *          logFields="(CSV list of fields to log)"
 *          logContent="(false|true)"
 *          logLevel="(ERROR|WARN|INFO|DEBUG)" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]" &gt;
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("nls")
public class DebugTagger extends AbstractDocumentTagger {

    private static final Logger LOG = 
            LogManager.getLogger(DebugTagger.class);
    
    private String[] logFields;
    private boolean logContent;
    private String logLevel;
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {

        Level level = Level.toLevel(logLevel);

        if (ArrayUtils.isEmpty(logFields)) {
            for (Entry<String, List<String>> entry : metadata.entrySet()) {
                logField(level, entry.getKey(), entry.getValue());
            }
        } else {
            for (String fieldName : logFields) {
                logField(level, fieldName, metadata.get(fieldName));
            }
        }

        if (logContent) {
            try {
                LOG.log(level, "CONTENT=" + IOUtils.toString(document));
            } catch (IOException e) {
                throw new ImporterHandlerException(
                        "Count not stream content.", e);
            }
        }
    }
    
    private void logField(Level level, String fieldName, List<String> values) {
        StringBuilder b = new StringBuilder();
        for (String value : values) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(value);
        }
        LOG.log(level, fieldName + "=" + b.toString());
    }
    
    public String[] getLogFields() {
        return ArrayUtils.clone(logFields);
    }
    public void setLogFields(String... logFields) {
        this.logFields = logFields;
    }

    public boolean isLogContent() {
        return logContent;
    }
    public void setLogContent(boolean logContent) {
        this.logContent = logContent;
    }

    public String getLogLevel() {
        return logLevel;
    }
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setLogContent(xml.getBoolean("[@logContent]", isLogContent()));
        String csvFields = xml.getString("[@logFields]", null);
        if (StringUtils.isNotBlank(csvFields)) {
            setLogFields(StringUtils.split(csvFields, ','));
        }
        setLogLevel(xml.getString("[@logLevel]", getLogLevel()));
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement("tagger");
        writer.writeAttribute("class", getClass().getCanonicalName());
        writer.writeAttribute(
                "logContent", Boolean.toString(isLogContent()));
        if (ArrayUtils.isNotEmpty(getLogFields())) {
            writer.writeAttribute(
                    "logFields", StringUtils.join(getLogFields(), ','));
        }
        if (StringUtils.isNotBlank(getLogLevel())) {
            writer.writeAttribute("logLevel", getLogLevel());
        }
    }


    

    @Override
    public String toString() {
        final int maxLen = 10;
        return "DebugTagger [logFields="
                + (logFields != null ? Arrays.asList(logFields).subList(0,
                        Math.min(logFields.length, maxLen)) : null)
                + ", logContent=" + logContent + ", logLevel=" + logLevel + "]";
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DebugTagger)) {
            return false;
        }
        DebugTagger castOther = (DebugTagger) other;
        return new EqualsBuilder().appendSuper(super.equals(other))
                .append(logFields, castOther.logFields)
                .append(logContent, castOther.logContent)
                .append(logLevel, castOther.logLevel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode())
                .append(logFields).append(logContent).append(logLevel)
                .toHashCode();
    }

}
