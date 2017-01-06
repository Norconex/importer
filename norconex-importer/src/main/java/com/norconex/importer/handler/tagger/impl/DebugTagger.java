/* Copyright 2014-2016 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>A utility tagger to help with troubleshooting of document importing.
 * Place this tagger anywhere in your handler configuration to print to 
 * the log stream the metadata fields or content so far when this handler 
 * gets invoked.
 * This handler does not impact the data being imported at all 
 * (it only reads it).</p>
 * 
 * <p>The default behavior logs all metadata fields using the DEBUG log level.
 * You can optionally set which fields to log and whether to also log the 
 * document content or not, as well as specifying a different log level.</p>
 * 
 * <p><b>Be careful:</b> Logging the content when you deal with very large 
 * content can result in memory exceptions.</p>
 * 
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.DebugTagger"
 *          logFields="(CSV list of fields to log)"
 *          logContent="(false|true)"
 *          logLevel="(ERROR|WARN|INFO|DEBUG)" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
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
                LOG.log(level, "CONTENT=" + IOUtils.toString(
                        document, CharEncoding.UTF_8));
            } catch (IOException e) {
                throw new ImporterHandlerException(
                        "Count not stream content.", e);
            }
        }
    }
    
    private void logField(Level level, String fieldName, List<String> values) {
        StringBuilder b = new StringBuilder();
        if (values == null) {
            b.append("<null>");
        } else {
            for (String value : values) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append(value);
            }
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
        setLogFields(ConfigurationUtil.getCSVArray(
                xml, "[@logFields]", getLogFields()));
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
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("logFields", logFields)
                .append("logContent", logContent)
                .append("logLevel", logLevel)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DebugTagger)) {
            return false;
        }
        DebugTagger castOther = (DebugTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(other))
                .append(logFields, castOther.logFields)
                .append(logContent, castOther.logContent)
                .append(logLevel, castOther.logLevel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(logFields)
                .append(logContent)
                .append(logLevel)
                .toHashCode();
    }

}
