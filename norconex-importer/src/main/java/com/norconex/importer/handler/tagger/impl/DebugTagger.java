/* Copyright 2014-2018 Norconex Inc.
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.norconex.commons.lang.SLF4JUtil;
import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.xml.XML;
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
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.DebugTagger"
 *          logFields="(CSV list of fields to log)"
 *          logContent="(false|true)"
 *          logLevel="(FATAL|ERROR|WARN|INFO|DEBUG|TRACE)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following logs the value of any "title" and "author" document metadata
 * fields.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.DebugTagger"
 *          logFields="title,author" logLevel="INFO" /&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class DebugTagger extends AbstractDocumentTagger {

    private static final Logger LOG =
            LoggerFactory.getLogger(DebugTagger.class);

    private final List<String> logFields = new ArrayList<>();
    private boolean logContent;
    private String logLevel;

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {

        Level level = Level.valueOf(
                ObjectUtils.defaultIfNull(logLevel, "debug").toUpperCase());

        if (logFields.isEmpty()) {
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
                SLF4JUtil.log(LOG, level, "CONTENT={}",
                        IOUtils.toString(document, StandardCharsets.UTF_8));
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
        SLF4JUtil.log(LOG, level, "{}={}", fieldName, b.toString());
    }

    public List<String> getLogFields() {
        return Collections.unmodifiableList(logFields);
    }
    public void setLogFields(List<String> logFields) {
        CollectionUtil.setAll(this.logFields, logFields);
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
    protected void loadHandlerFromXML(XML xml) {
        setLogContent(xml.getBoolean("@logContent", logContent));
        setLogFields(xml.getDelimitedStringList("@logFields", logFields));
        setLogLevel(xml.getString("@logLevel", logLevel));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("logContent", logContent);
        xml.setDelimitedAttributeList("logFields", logFields);
        xml.setAttribute("logLevel", logLevel);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
