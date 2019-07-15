/* Copyright 2014-2019 Norconex Inc.
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
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Given a separator, split a field string into multiple segments
 * representing each node of a hierarchical branch. This is useful
 * when faceting, to find out how many documents fall under each
 * node of a hierarchy. For example, take this hierarchical string:</p>
 * <pre>
 *   /vegetable/potato/sweet
 * </pre>
 * <p>We specify a slash (/) separator and it will produce the folowing entries
 * in the specified document metadata field:</p>
 *
 * <pre>
 *   /vegetable
 *   /vegetable/potato
 *   /vegetable/potato/sweet
 * </pre>
 * <p>
 * If no target field is specified (<code>toField</code>) the
 * source field (<code>fromField</code>) will be used to store the resulting
 * values. The same applies to the source and target hierarchy separators
 * (<code>fromSeparator</code> and <code>toSeparator</code>).
 * </p>
 * <p>
 * <b>Since 2.10.0</b>, you can "keepEmptySegments", as well as specify
 * whether the "fromSeparator" is a regular expression. When using regular
 * expression without a "toSeparator", the text matching the expression is
 * kept as is and thus can be different for each segment.
 * </p>
 * <p>
 * Can be used both as a pre-parse or post-parse handler.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.HierarchyTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *
 *      &lt;hierarchy fromField="(from field)"
 *              toField="(optional to field)"
 *              fromSeparator="(original separator)"
 *              toSeparator="(optional new separator)"
 *              overwrite="[false|true]"
 *              regex="[false|true]"
 *              keepEmptySegments="[false|true]" /&gt;
 *      &lt;!-- multiple hierarchy tags allowed --&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <p>
 * The following will expand a slash-separated vegetable hierarchy found in a
 * "vegetable" field into a "vegetableHierarchy" field.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.HierarchyTagger"&gt;
 *      &lt;hierarchy fromField="vegetable" toField="vegetableHierarchy"
 *                 fromSeparator="/"/&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class HierarchyTagger extends AbstractDocumentTagger {

    private final List<HierarchyDetails> list = new ArrayList<>();

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {

        for (HierarchyDetails details : list) {
            breakSegments(metadata, details);
        }
    }

    private void breakSegments(
            ImporterMetadata metadata, HierarchyDetails details) {

        String toField = details.fromField;
        if (StringUtils.isNotBlank(details.toField)) {
            toField = details.toField;
        }

        Pattern delim;
        if (details.regex) {
            delim = Pattern.compile(details.fromSeparator);
        } else {
            delim = Pattern.compile(Pattern.quote(details.fromSeparator));
        }

        List<String> paths = new ArrayList<>();
        for (String value : metadata.getStrings(details.fromField)) {
            if (value == null) {
                continue;
            }

            List<Object> segments = new ArrayList<>();
            int prevMatch = 0;
            Matcher m = delim.matcher(value);
            while (m.find()) {
                int delimStart = m.start();
                if (prevMatch != delimStart) {
                    segments.add(value.substring(prevMatch, delimStart));
                }
                prevMatch = m.end();

                String sep = m.group();
                if (StringUtils.isNotEmpty(details.toSeparator)) {
                    sep = details.toSeparator;
                }
                segments.add(new Separator(sep));
            }
            if (value.length() > prevMatch) {
                segments.add(value.substring(prevMatch));
            }

            // if not keeping empty segments, keep last of a series
            // (iterating in reverse to help do so)
            boolean prevIsSep = false;
            if (!details.keepEmptySegments) {
                ListIterator<Object> iter =
                        segments.listIterator(segments.size());
                while(iter.hasPrevious()) {
                    Object seg = iter.previous();
                    if (seg instanceof Separator) {
                        if (prevIsSep) {
                            iter.remove();
                        }
                        prevIsSep = true;
                    } else {
                        prevIsSep = false;
                    }
                }
            }

            prevIsSep = false;
            StringBuilder b = new StringBuilder();
            for (Object seg : segments) {
                if (seg instanceof Separator) {
                    if (prevIsSep) {
                        paths.add(b.toString());
                    }
                    b.append(seg);
                    prevIsSep = true;
                } else {
                    b.append(seg);
                    prevIsSep = false;
                    paths.add(b.toString());
                }
            }
        }

        String[] nodesArray = paths.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        if (details.overwrite) {
            metadata.set(toField, nodesArray);
        } else {
            metadata.add(toField, nodesArray);
        }
    }

    /**
     * Adds hierarchy instructions.
     * @param fromField source field name
     * @param toField target optional target field name
     * @param fromSeparator source separator
     * @param toSeparator optional target separator
     * @param overwrite whether to overwrite target field if it exists
     * @deprecated
     *    Since 2.10.0, use {@link #addHierarcyDetails(HierarchyDetails)}
     *    instead.
     */
    @Deprecated
    public void addHierarcyDetails(
            String fromField, String toField,
            String fromSeparator, String toSeparator, boolean overwrite) {
        if (StringUtils.isAnyBlank(fromField, fromSeparator)) {
            return;
        }
        HierarchyDetails hd = new HierarchyDetails(
                fromField, toField, fromSeparator, toSeparator);
        hd.setOverwrite(overwrite);
        addHierarcyDetails(hd);
    }

    /**
     * Adds hierarchy instructions.
     * @param details hierarchy details
     */
    public void addHierarcyDetails(HierarchyDetails details) {
        if (details == null || StringUtils.isAnyBlank(
                details.fromField, details.fromSeparator)) {
            return;
        }
        list.add(details);
    }

    public List<HierarchyDetails> getHierarchyDetails() {
        return list;
    }


    @Override
    protected void loadHandlerFromXML(XML xml) {
        for (XML node : xml.getXMLList("hierarchy")) {
            HierarchyDetails hd = new HierarchyDetails(
                    node.getString("@fromField", null),
                    node.getString("@toField", null),
                    node.getString("@fromSeparator", null),
                    node.getString("@toSeparator", null));
            hd.setOverwrite(node.getBoolean("@overwrite", false));
            hd.setKeepEmptySegments(
                    node.getBoolean("@keepEmptySegments", false));
            hd.setRegex(node.getBoolean("@regex", false));
            addHierarcyDetails(hd);
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (HierarchyDetails hd : list) {
            xml.addElement("hierarchy")
                    .setAttribute("fromField", hd.fromField)
                    .setAttribute("toField", hd.toField)
                    .setAttribute("fromSeparator", hd.fromSeparator)
                    .setAttribute("toSeparator", hd.toSeparator)
                    .setAttribute("overwrite", hd.overwrite)
                    .setAttribute("keepEmptySegments", hd.keepEmptySegments)
                    .setAttribute("regex", hd.regex);
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

    private static class Separator {
        private final String sep;
        public Separator(String sep) {
            super();
            this.sep = sep;
        }
        @Override
        public String toString() {
            return sep;
        }
    }

    public static class HierarchyDetails {
        private String fromField;
        private String toField;
        private String fromSeparator;
        private String toSeparator;
        private boolean overwrite;
        private boolean keepEmptySegments;
        private boolean regex;

        public HierarchyDetails() {
            super();
        }
        public HierarchyDetails(String fromField, String toField,
                String fromSeparator, String toSeparator) {
            this.fromField = fromField;
            this.toField = toField;
            this.fromSeparator = fromSeparator;
            this.toSeparator = toSeparator;
        }

        public String getFromField() {
            return fromField;
        }
        public void setFromField(String fromField) {
            this.fromField = fromField;
        }
        public String getToField() {
            return toField;
        }
        public void setToField(String toField) {
            this.toField = toField;
        }
        public String getFromSeparator() {
            return fromSeparator;
        }
        public void setFromSeparator(String fromSeparator) {
            this.fromSeparator = fromSeparator;
        }
        public String getToSeparator() {
            return toSeparator;
        }
        public void setToSeparator(String toSeparator) {
            this.toSeparator = toSeparator;
        }
        public boolean isOverwrite() {
            return overwrite;
        }
        public void setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
        }
        public boolean isKeepEmptySegments() {
            return keepEmptySegments;
        }
        public void setKeepEmptySegments(boolean keepEmptySegments) {
            this.keepEmptySegments = keepEmptySegments;
        }
        public boolean isRegex() {
            return regex;
        }
        public void setRegex(boolean regex) {
            this.regex = regex;
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

}
