/* Copyright 2019 Norconex Inc.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>A utility tagger that reports in a CSV file the fields discovered
 * in a crawl session, captured at the point of your choice in the
 * importing process.
 * If you use this class to report on all fields discovered, make sure you
 * use it as a post-parse handler, before you are limiting which fields
 * you want to keep.
 * </p>
 * <p>
 * The report will list one field per row, along with a few sample values
 * (3 by default).  The samples will be the first ones encountered.
 * </p>
 * <p>
 * This handler does not impact the data being imported at all
 * (it only reads it). It also do not store the "content" as a field.
 * </p>
 *
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.FieldReportTagger"
 *          maxSamples="(max number of sample values)"
 *          file="(path to a local file)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following logs all discovered fields into a "field-report.csv" file,
 * along with only 1 example value..
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.FieldReportTagger"
 *          maxSamples="1" file="C:\reports\field-report.csv" /&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.9.1
 */
public class FieldReportTagger extends AbstractDocumentTagger {

    private static final Logger LOG =
            LogManager.getLogger(FieldReportTagger.class);

    public static final int DEFAULT_MAX_SAMPLES = 3;
    private int maxSamples = DEFAULT_MAX_SAMPLES;
    private File file;
    private final SetValuedMap<String, String> fields =
            new HashSetValuedHashMap<>();

    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }

    public int getMaxSamples() {
        return maxSamples;
    }
    public void setMaxSamples(int maxSamples) {
        this.maxSamples = maxSamples;
    }

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {
        reportFields(metadata);
    }

    private synchronized void reportFields(ImporterMetadata metadata) {
            boolean dirty = false;
            for (Entry<String, List<String>> en : metadata.entrySet()) {
                if (reportField(en.getKey(), en.getValue())) {
                    dirty = true;
                }
            }
            if (dirty) {
                saveReport();
            }
    }

    private boolean reportField(String field, List<String> values) {
        boolean dirty = false;
        if (!fields.containsKey(field)) {
            dirty = true;
        }
        Set<String> existingSamples = fields.get(field);
        int beforeCount = existingSamples.size();
        for (String value : values) {
            if (existingSamples.size() < maxSamples
                    && StringUtils.isNotBlank(value)) {
                existingSamples.add(value);
            } else {
                break;
            }
        }
        return dirty || beforeCount != existingSamples.size();
    }

    private void saveReport() {
        try (CSVPrinter printer =
                new CSVPrinter(new FileWriter(file), CSVFormat.DEFAULT)) {
            for (Entry<String, Collection<String>> en :
                    fields.asMap().entrySet()) {
                String field = en.getKey();
                Collection<String> values = en.getValue();

                printer.print(field);
                for (String value : values) {
                    printer.print(value);
                }
                // fill the blanks
                for (int i = 0; i < maxSamples - values.size(); i++) {
                    printer.print("");
                }
                printer.println();
            }
            printer.flush();
        } catch (IOException e) {
            LOG.error("Could not write field report to: " + file, e);
        }
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setMaxSamples(xml.getInt("[@maxSamples]", maxSamples));
        String f = xml.getString("[@file]");
        if (StringUtils.isNotBlank(f)) {
            setFile(new File(f));
        }
    }

    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement("tagger");
        writer.writeAttribute("class", getClass().getCanonicalName());
        writer.writeAttributeInteger("maxSamples", maxSamples);
        if (file != null) {
            writer.writeAttribute("file", file.toString());
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("maxSamples", maxSamples)
                .append("file", file)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof FieldReportTagger)) {
            return false;
        }
        FieldReportTagger castOther = (FieldReportTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(other))
                .append(maxSamples, castOther.maxSamples)
                .append(file, castOther.file)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(maxSamples)
                .append(file)
                .toHashCode();
    }
}
