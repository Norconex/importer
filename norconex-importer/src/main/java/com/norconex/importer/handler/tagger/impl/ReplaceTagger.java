/* Copyright 2010-2020 Norconex Inc.
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.Regex;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Replaces an existing metadata value with another one. The "toField"
 * argument is optional.
 * </p>
 * <p>It is possible to only keep values that changed from a replacement and
 * discard others by setting "discardUnchanged" to <code>true</code>.
 * </p>
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 * Can be used both as a pre-parse or post-parse handler.
 * </p>
 * <p>
 * You can specify whether matches should be made
 * against the whole field value or not (default). You can also specify whether
 * replacement should be attempted on first match only (default) or all
 * occurrences. This last option is only applicable when whole value matching
 * is <code>false</code>.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.ReplaceTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;replace fromField="sourceFieldName"
 *               toField="targetFieldName"
 *               caseSensitive="[false|true]"
 *               regex="[false|true]"
 *               wholeMatch="[false|true]"
 *               replaceAll="[false|true]"
 *               onSet="[append|prepend|replace|optional]"
 *               discardUnchanged="[false|true]" &gt;
 *          &lt;fromValue&gt;Source Value&lt;/fromValue&gt;
 *          &lt;toValue&gt;Target Value&lt;/toValue&gt;
 *      &lt;/replace&gt;
 *      &lt;!-- multiple replace tags allowed --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <p>
 * The following example replaces occurrences of "apple" to "orange"
 * in the "fruit" field.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.ReplaceTagger"&gt;
 *      &lt;replace fromField="fruit" replaceAll="true"&gt;
 *          &lt;fromValue&gt;apple&lt;/fromValue&gt;
 *          &lt;toValue&gt;orange&lt;/toValue&gt;
 *      &lt;/replace&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class ReplaceTagger extends AbstractDocumentTagger {

    //TODO add "applyTo=field|value" to replace tag, and remove "fromField"
    // and "toField" and rename from|toValue to just from and to.

    private final List<Replacement> replacements = new ArrayList<>();

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        for (Replacement repl : replacements) {
            if (metadata.containsKey(repl.getFromField())) {
                replaceMeta(metadata, repl);
            }
        }
    }

    private void replaceMeta(ImporterMetadata metadata, Replacement repl) {
        List<String> metaValues = metadata.getStrings(repl.getFromField());
        List<String> newValues = new ArrayList<>(metaValues.size());

        for (String metaValue : metaValues) {
            String newValue = replaceValue(metaValue, repl);
            if (newValue != null && (!repl.isDiscardUnchanged()
                    || !Objects.equals(metaValue, newValue))) {
                newValues.add(newValue);
            }
        }

        if (StringUtils.isNotBlank(repl.toField)) {
            // set on target field
            PropertySetter.orDefault(repl.getOnSet()).apply(
                    metadata, repl.toField, newValues);
        } else {
            // overwrite source field
            PropertySetter.REPLACE.apply(metadata, repl.fromField, newValues);
        }
    }

    // if no matches, return null
    private String replaceValue(String metaValue, Replacement r) {
        String fromValue = r.getFromValue();
        if (!r.isRegex()) {
            fromValue = Pattern.quote(fromValue);
        }
        Pattern p = Regex.compileDotAll(fromValue, !r.isCaseSensitive());
        Matcher m = p.matcher(metaValue);

        if (r.isWholeMatch() && m.matches()
                || !r.isWholeMatch() && m.find()) {
            String toValue = StringUtils.defaultString(r.getToValue());
            if (r.isReplaceAll()) {
                return m.replaceAll(toValue);
            }
            return m.replaceFirst(toValue);
        }
        return metaValue;
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
        private boolean wholeMatch;
        private boolean replaceAll;
        private PropertySetter onSet;
        private boolean discardUnchanged;
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
        public boolean isWholeMatch() {
            return wholeMatch;
        }
        public boolean isReplaceAll() {
            return replaceAll;
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
        /**
         * Sets whether the specified "from" value should match the entire
         * field value or not (default is <code>false</code>).
         * @param wholeMatch <b>true</b> for whole match
         * @since 2.6.1
         */
        public void setWholeMatch(boolean wholeMatch) {
            this.wholeMatch = wholeMatch;
        }
        /**
         * Sets whether to replace all occurrences of the "from" value
         * with the "to" value. (default is <code>false</code>).
         * @param replaceAll <b>true</b> to replace all occurrences
         * @since 2.6.1
         */
        public void setReplaceAll(boolean replaceAll) {
            this.replaceAll = replaceAll;
        }
        /**
         * Gets the property setter to use when a value is set.
         * @return property setter
         * @since 3.0.0
         */
        public PropertySetter getOnSet() {
            return onSet;
        }
        /**
         * Sets the property setter to use when a value is set.
         * @param onSet property setter
         * @since 3.0.0
         */
        public void setOnSet(PropertySetter onSet) {
            this.onSet = onSet;
        }
        /**
         * Gets whether to discard values that did not change as a result
         * of the replacement attempt.
         * @return <code>true</code> if discarding unchanged values
         * @since 3.0.0
         */
        public boolean isDiscardUnchanged() {
            return discardUnchanged;
        }
        /**
         * Sets whether to discard values that did not change as a result
         * of the replacement attempt.
         * @param discardUnchanged <code>true</code> if discarding unchanged
         *        values
         * @since 3.0.0
         */
        public void setDiscardUnchanged(boolean discardUnchanged) {
            this.discardUnchanged = discardUnchanged;
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

    @Override
    protected void loadHandlerFromXML(XML xml) {
        for (XML node : xml.getXMLList("replace")) {
            Replacement r = new Replacement();
            r.setFromValue(node.getString("fromValue"));
            r.setToValue(node.getString("toValue"));
            r.setFromField(node.getString("@fromField"));
            r.setToField(node.getString("@toField", null));
            r.setRegex(node.getBoolean("@regex", false));
            r.setCaseSensitive(node.getBoolean("@caseSensitive", false));
            r.setWholeMatch(node.getBoolean("@wholeMatch", false));
            r.setReplaceAll(node.getBoolean("@replaceAll", false));
            r.setOnSet(PropertySetter.fromXML(node, null));
            r.setDiscardUnchanged(node.getBoolean("@discardUnchanged", false));
            addReplacement(r);
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (Replacement replacement : replacements) {
            XML rxml = xml.addElement("replace")
                    .setAttribute("fromField", replacement.getFromField())
                    .setAttribute("toField", replacement.getToField())
                    .setAttribute("regex", replacement.isRegex())
                    .setAttribute(
                            "caseSensitive", replacement.isCaseSensitive())
                    .setAttribute("wholeMatch", replacement.isWholeMatch())
                    .setAttribute("replaceAll", replacement.isReplaceAll());
            rxml.addElement("fromValue", replacement.getFromValue());
            rxml.addElement("toValue", replacement.getToValue());
            PropertySetter.toXML(rxml, replacement.getOnSet());
            rxml.setAttribute(
                    "discardUnchanged", replacement.isDiscardUnchanged());
        }
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
