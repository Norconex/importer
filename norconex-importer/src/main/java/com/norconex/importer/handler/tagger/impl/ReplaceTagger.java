/* Copyright 2010-2016 Norconex Inc.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;


/**
 * <p>Replaces an existing metadata value with another one. The "toField" 
 * argument is optional. The same field will be used for the replacement if no
 * "toField" is specified. If there are no matches and a "toField" is 
 * specified, no value will be added to the "toField".  To have the original
 * value copied in the "toField" when there are no matches, first copy the 
 * original value using {@link CopyTagger} to your target field then use this 
 * class on that new field without a "toField". 
 * </p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.ReplaceTagger"&gt;
 *      &lt;replace fromField="sourceFieldName" toField="targetFieldName"
 *               caseSensitive="[false|true]"
 *               regex="[false|true]"&gt;
 *          &lt;fromValue&gt;Source Value&lt;/fromValue&gt;
 *          &lt;toValue&gt;Target Value&lt;/toValue&gt;
 *      &lt;/replace&gt;
 *      &lt;!-- multiple replace tags allowed --&gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see Pattern
 */
public class ReplaceTagger extends AbstractDocumentTagger {

    private final List<Replacement> replacements = new ArrayList<>();
    
    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        for (Replacement repl : replacements) {
            if (metadata.containsKey(repl.getFromField())) {
                String[] metaValues = metadata.getStrings(repl.getFromField())
                        .toArray(ArrayUtils.EMPTY_STRING_ARRAY);
                for (int i = 0; i < metaValues.length; i++) {
                    String metaValue = metaValues[i];
                    String newValue = null;
                    if (repl.isRegex()) {
                        newValue = regexReplace(metaValue, 
                                repl.getFromValue(), repl.getToValue(),
                                repl.isCaseSensitive());
                    } else {
                        newValue = regularReplace(metaValue, 
                                repl.getFromValue(), repl.getToValue(),
                                repl.isCaseSensitive());
                    }
                    if (newValue != null) {
                        if (StringUtils.isNotBlank(repl.getToField())) {
                            metadata.addString(repl.getToField(), newValue);
                        } else {
                            metaValues[i] = newValue;
                            metadata.setString(repl.getFromField(), metaValues);
                        }
                    }
                }
            }
        }
    }
    
    // if no matches, return null
    private String regexReplace(String metaValue, String fromValue, 
            String toValue, boolean caseSensitive) {
        int flags = Pattern.DOTALL;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        Pattern p = Pattern.compile(fromValue, flags);
        Matcher m = p.matcher(metaValue);
        if (m.find()) {
            return m.replaceFirst(toValue);
        }
        return null;
    }
    // if no matches, return null
    private String regularReplace(String metaValue, String fromValue, 
            String toValue, boolean caseSensitive) {
        String mv = metaValue;
        String fv = fromValue;
        if (!caseSensitive) {
            mv = StringUtils.lowerCase(mv);
            fv = StringUtils.lowerCase(fv);
        }
        if (Objects.equals(mv, fv)) {
            return toValue;
        }
        return null;
    }
        
    public List<Replacement> getReplacements() {
        return Collections.unmodifiableList(replacements);
    }

    public void removeReplacement(String fromField) {
        List<Replacement> toRemove = new ArrayList<>();
        for (Replacement replacement : replacements) {
            if (Objects.equals(replacement.getFromField(), fromField)) {
                toRemove.add(replacement);
            }
        }
        synchronized (replacements) {
            replacements.removeAll(toRemove);
        }
    }
    
    /**
     * Adds a replacement.
     * @param fromValue value to replace
     * @param toValue replacement value
     * @param fromField field holding the value to replace
     * @deprecated Since 2.2.0, use {@link #addReplacement(Replacement)} 
     *             instead.
     */
    @Deprecated
    public void addReplacement(
            String fromValue, String toValue, String fromField) {
        addReplacement(fromValue, toValue, fromField, null, false);
    }
    /**
     * Adds a replacement.
     * @param fromValue value to replace
     * @param toValue replacement value
     * @param fromField field holding the value to replace
     * @param regex <code>true</code> if a regular expression
     * @deprecated Since 2.2.0, use {@link #addReplacement(Replacement)} 
     *             instead.
     */
    @Deprecated
    public void addReplacement(
            String fromValue, String toValue, String fromField, boolean regex) {
        addReplacement(fromValue, toValue, fromField, null, regex);
    }
    /**
     * Adds a replacement.
     * @param fromValue value to replace
     * @param toValue replacement value
     * @param fromField field holding the value to replace
     * @param toField field to store the replaced value
     * @deprecated Since 2.2.0, use {@link #addReplacement(Replacement)} 
     *             instead.
     */
    @Deprecated
    public void addReplacement(String fromValue, String toValue, 
            String fromField, String toField) {
        addReplacement(fromValue, toValue, fromField, toField, false);
    }
    /**
     * Adds a replacement.
     * @param fromValue value to replace
     * @param toValue replacement value
     * @param fromField field holding the value to replace
     * @param toField field to store the replaced value
     * @param regex <code>true</code> if a regular expression
     * @deprecated Since 2.2.0, use {@link #addReplacement(Replacement)} 
     *             instead.
     */
    @Deprecated
    public void addReplacement(String fromValue, String toValue, 
            String fromField, String toField, boolean regex) {
        Replacement r = new Replacement(fromField, fromValue, toField, toValue);
        r.setRegex(regex);
        addReplacement(r);
    }

    /**
     * Adds a replacement.
     * @param replacement the replacement
     * @since 2.2.0
     */
    public void addReplacement(Replacement replacement) {
        if (replacement != null) {
            replacements.add(replacement);
        }
    }

    
    public static class Replacement {
        private String fromField;
        private String fromValue;
        private String toField;
        private String toValue;
        private boolean regex;
        private boolean caseSensitive;
        public Replacement() {
            super();
        }
        public Replacement(
                String fromField, String fromValue, 
                String toField, String toValue) {
            super();
            this.fromField = fromField;
            this.fromValue = fromValue;
            this.toField = toField;
            this.toValue = toValue;
        }
        public String getFromField() {
            return fromField;
        }
        public String getFromValue() {
            return fromValue;
        }
        public String getToField() {
            return toField;
        }
        public String getToValue() {
            return toValue;
        }
        public boolean isRegex() {
            return regex;
        }
        /**
         * Whether the replacement should be case sensitive or not.
         * @return the caseSensitive
         * @since 2.2.0
         */
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
        /**
         * Sets the field with the value to replace.
         * @param fromField field with the value to replace
         * @since 2.2.0
         */
        public void setFromField(String fromField) {
            this.fromField = fromField;
        }
        /**
         * Sets the value to replace.
         * @param fromValue the value to replace
         * @since 2.2.0
         */
        public void setFromValue(String fromValue) {
            this.fromValue = fromValue;
        }
        /**
         * Sets the field to store the replaced value.
         * @param toField field to store the replaced value
         * @since 2.2.0
         */
        public void setToField(String toField) {
            this.toField = toField;
        }
        /**
         * Sets the replacement value.
         * @param toValue the replacement value
         * @since 2.2.0
         */
        public void setToValue(String toValue) {
            this.toValue = toValue;
        }
        /**
         * Sets whether the <code>fromValue</code> is a regular expression.
         * @param regex <code>true</code> if <code>fromValue</code> is a 
         *              regular expression
         * @since 2.2.0
         */
        public void setRegex(boolean regex) {
            this.regex = regex;
        }
        /**
         * Sets whether to do a case sensitive replacement or not.
         * <b>Since 2.2.0</b>, replacements are not case sensitive by default.
         * @param caseSensitive <code>true</code> if doing a case sensitive
         *                      replacement
         * @since 2.2.0
         */
        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }
        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("fromField", fromField)
                    .append("fromValue", fromValue)
                    .append("toField", toField)
                    .append("toValue", toValue)
                    .append("regex", regex)
                    .append("caseSensitive", caseSensitive)
                    .toString();
        }
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof Replacement))
                return false;
            Replacement castOther = (Replacement) other;
            return new EqualsBuilder()
                    .append(fromField, castOther.fromField)
                    .append(fromValue, castOther.fromValue)
                    .append(toField, castOther.toField)
                    .append(toValue, castOther.toValue)
                    .append(regex, castOther.regex)
                    .append(caseSensitive, castOther.caseSensitive)
                    .isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                        .append(fromField)
                        .append(fromValue)
                        .append(toField)
                        .append(toValue)
                        .append(regex)
                        .append(caseSensitive)
                        .toHashCode();
        }
    }
    
    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        List<HierarchicalConfiguration> nodes = xml.configurationsAt("replace");
        for (HierarchicalConfiguration node : nodes) {
            Replacement r = new Replacement();
            r.setFromValue(node.getString("fromValue"));
            r.setToValue(node.getString("toValue"));
            r.setFromField(node.getString("[@fromField]"));
            r.setToField(node.getString("[@toField]", null));
            r.setRegex(node.getBoolean("[@regex]", false));
            r.setCaseSensitive(node.getBoolean("[@caseSensitive]", false));
            addReplacement(r);
        }
    }
    
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        for (Replacement replacement : replacements) {
            writer.writeStartElement("replace");
            writer.writeAttribute("fromField", replacement.getFromField());
            if (replacement.getToField() != null) {
                writer.writeAttribute("toField", replacement.getToField());
            }
            writer.writeAttribute("regex", 
                    Boolean.toString(replacement.isRegex()));
            writer.writeAttribute("caseSensitive", 
                    Boolean.toString(replacement.isCaseSensitive()));
            writer.writeStartElement("fromValue");
            writer.writeCharacters(replacement.getFromValue());
            writer.writeEndElement();
            writer.writeStartElement("toValue");
            writer.writeCharacters(replacement.getToValue());
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ReplaceTagger)) {
            return false;
        }
        ReplaceTagger castOther = (ReplaceTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(replacements, castOther.replacements)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(replacements)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("replacements", replacements)
                .toString();
    }
}
