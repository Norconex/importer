/* Copyright 2010-2015 Norconex Inc.
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
package com.norconex.importer.handler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;

/**
 * Base class for handlers applying only to certain type of documents
 * by providing a way to restrict applicable documents based on 
 * a metadata field value, where the value matches a regular expression. For
 * instance, to apply a handler only to text documents, you can use the 
 * following:
 * 
 * <pre>
 *   myHandler.setRestriction("document.contentType", "^text/.*$");
 * </pre> 
 * 
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * 
 * <pre>
 *  &lt;restrictTo caseSensitive="[false|true]"
 *          field="(name of header/metadata field name to match)"&gt;
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 *  &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 * </pre>
 * <p>
 * Subclasses <b>must</b> test if a document is accepted using the 
 * {@link #isApplicable(String, ImporterMetadata, boolean)} method.
 * </p>
 * <p>
 * Subclasses can safely be used as either pre-parse or post-parse handlers.
 * </p>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public abstract class AbstractImporterHandler implements IXMLConfigurable {

    private static final Logger LOG = 
            LogManager.getLogger(AbstractImporterHandler.class);
    
    private final List<Restriction> restrictions = new ArrayList<>();
    private final String xmltag;
    
    public AbstractImporterHandler(String xmltag) {
        super();
        this.xmltag = xmltag;
    }

    /**
     * Adds a restriction this handler should be restricted to.
     * @param field metadata property/field
     * @param regex regular expression
     * @param caseSensitive whether regular expression should be case sensitive
     */
    public void addRestriction(
            String field, String regex, boolean caseSensitive) {
        restrictions.add(new Restriction(field, regex, caseSensitive));
    }

    /**
     * Class to invoke by subclasses to find out if this handler should be
     * rejected or not based on the metadata restriction provided.
     * @param reference document reference
     * @param metadata document metadata.
     * @param parsed if the document was parsed (i.e. imported) already
     * @return <code>true</code> if this handler is applicable to the document
     * @throws ImporterHandlerException problem evaluating if applicable
     */
    protected final boolean isApplicable(
            String reference, ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        if (restrictions.isEmpty()) {
            return true;
        }
        for (Restriction restriction : restrictions) {
            if (restriction.applies(metadata)) {
                return true;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(getClass() + " handler does not apply to: " + reference);
        }
        return false;
    }

    @Override
    public final void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("restrictTo");
        for (HierarchicalConfiguration node : nodes) {
            addRestriction(
                    node.getString("[@field]"), 
                    node.getString("", null),
                    node.getBoolean("[@caseSensitive]", false));
        }
        loadHandlerFromXML(xml);
    }
    /**
     * Loads configuration settings specific to the implementing class.
     * @param xml xml configuration
     * @throws IOException could not load from XML
     */
    protected abstract void loadHandlerFromXML(XMLConfiguration xml)
            throws IOException;
    
    
    @Override
    public final void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            writer.writeStartElement(xmltag);
            writer.writeAttribute("class", getClass().getCanonicalName());

            saveHandlerToXML(writer);

            for (Restriction restriction : restrictions) {
                writer.writeStartElement("restrictTo");
                if (restriction.field != null) {
                    writer.writeAttribute("field", restriction.field);
                }
                writer.writeAttribute("caseSensitive", 
                        Boolean.toString(restriction.caseSensitive));
                if (restriction.regex != null) {
                    writer.writeCharacters(restriction.regex);
                }
                writer.writeEndElement();
            }
            
            writer.writeEndElement();
            writer.flush();
            writer.close();
            
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }
    
    /**
     * Saves configuration settings specific to the implementing class.
     * The parent tag along with the "class" attribute are already written.
     * Implementors must not close the writer.
     * 
     * @param writer the xml writer
     * @throws XMLStreamException could not save to XML
     */
    protected abstract void saveHandlerToXML(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((restrictions == null) ? 0 : restrictions.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractImporterHandler)) {
            return false;
        }
        AbstractImporterHandler other = (AbstractImporterHandler) obj;
        if (restrictions == null) {
            if (other.restrictions != null) {
                return false;
            }
        } else if (!restrictions.equals(other.restrictions)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("restrictions", restrictions);
        return builder.toString();
    }

    private class Restriction {
        private final String field;
        private final String regex;
        private final Pattern pattern;
        private final boolean caseSensitive;
        public Restriction(String field, String regex, boolean caseSensitive) {
            super();
            this.field = field;
            this.regex = regex;
            this.caseSensitive = caseSensitive;
            if (regex != null) {
                if (caseSensitive) {
                    this.pattern = Pattern.compile(regex);
                } else {
                    this.pattern = Pattern.compile(
                            regex, Pattern.CASE_INSENSITIVE);
                }
            } else {
                this.pattern = Pattern.compile(".*");
            }            
        }
        public boolean applies(ImporterMetadata metadata) {
            if (StringUtils.isBlank(regex)) {
                return true;
            }
            Collection<String> values =  metadata.getStrings(field);
            for (String value : values) {
                String safeVal = StringUtils.trimToEmpty(value);
                if (pattern.matcher(safeVal).matches()) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (caseSensitive ? 1231 : 1237);
            result = prime * result + ((field == null) ? 0 : field.hashCode());
            result = prime * result + ((regex == null) ? 0 : regex.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Restriction)) {
                return false;
            }
            Restriction other = (Restriction) obj;
            if (caseSensitive != other.caseSensitive) {
                return false;
            }
            if (field == null) {
                if (other.field != null) {
                    return false;
                }
            } else if (!field.equals(other.field)) {
                return false;
            }
            if (regex == null) {
                if (other.regex != null) {
                    return false;
                }
            } else if (!regex.equals(other.regex)) {
                return false;
            }
            return true;
        }
        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(this);
            builder.append("field", field);
            builder.append("regex", regex);
            builder.append("caseSensitive", caseSensitive);
            return builder.toString();
        }
    }
}
