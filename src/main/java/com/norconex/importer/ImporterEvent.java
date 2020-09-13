/* Copyright 2020 Norconex Inc.
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
package com.norconex.importer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.event.Event;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.parser.ParseState;

/**
 * An Importer event.
 * @author Pascal Essiembre
 * @since 3.0.0
 */
public class ImporterEvent extends Event {

    private static final long serialVersionUID = 1L;

    public static final String IMPORTER_HANDLER_BEGIN =
            "IMPORTER_HANDLER_BEGIN";
    public static final String IMPORTER_HANDLER_END= "IMPORTER_HANDLER_END";
    public static final String IMPORTER_HANDLER_ERROR= "IMPORTER_HANDLER_ERROR";
    public static final String IMPORTER_PARSER_BEGIN = "IMPORTER_PARSER_BEGIN";
    public static final String IMPORTER_PARSER_END = "IMPORTER_PARSER_END";
    public static final String IMPORTER_PARSER_ERROR = "IMPORTER_PARSER_ERROR";


    private final ParseState parseState;
    private final transient Object subject;

    public static class Builder extends Event.Builder<Builder> {

        private ParseState parseState;
        private Object subject;

        public Builder(String name, Doc source) {
            super(name, source);
        }

        public Builder parseState(ParseState parseState) {
            this.parseState = parseState;
            return this;
        }
        public Builder subject(Object subject) {
            this.subject = subject;
            return this;
        }

        @Override
        public ImporterEvent build() {
            return new ImporterEvent(this);
        }
    }

    /**
     * New event.
     * @param b builder
     */
    protected ImporterEvent(Builder b) {
        super(b);
        this.parseState = b.parseState;
        this.subject = b.subject;
    }

    @Override
    public Doc getSource() {
        return (Doc) super.getSource();
    }

    public boolean isParsed() {
        return ParseState.isPost(parseState);
    }
    public ParseState getParseState() {
        return parseState;
    }
    public Object getSubject() {
        return subject;
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
        StringBuilder b = new StringBuilder();
        if (getSource() != null) {
            b.append(getSource().getReference());
        }
        if (b.length() > 0) {
            b.append(" ");
        }
        b.append("(parseState:").append(parseState).append(')');
        if (subject != null) {
            b.append(" - ");
            b.append(subject.toString());
        }
        return b.toString();
    }
}
