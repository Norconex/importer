/* Copyright 2016 Norconex Inc.
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
package com.norconex.importer.parser;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Configuration settings influencing how documents are parsed by various 
 * parsers.  These settings are not applicable to all parsers and some parsers
 * may decide not to support some of these settings (for not being able to
 * or else).
 * @author Pascal Essiembre
 * @since 2.6.0
 */
public class ParseHints {

    private final OCRConfig ocrConfig = new OCRConfig();
    private final EmbeddedConfig embeddedConfig = new EmbeddedConfig();
    private final GrobidConfig grobidConfig = new GrobidConfig();

    public OCRConfig getOcrConfig() {
        return ocrConfig;
    }
    public EmbeddedConfig getEmbeddedConfig() {
        return embeddedConfig;
    }
    public GrobidConfig getGrobidConfig() {
        return grobidConfig;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ParseHints)) {
            return false;
        }
        ParseHints castOther = (ParseHints) other;
        return new EqualsBuilder()
                .append(ocrConfig, castOther.ocrConfig)
                .append(embeddedConfig, castOther.embeddedConfig)
                .append(grobidConfig, castOther.grobidConfig)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(ocrConfig)
                .append(embeddedConfig)
                .append(grobidConfig)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("ocrConfig", ocrConfig)
                .append("embeddedConfig", embeddedConfig)
                .append("grobidConfig", grobidConfig)
                .toString();
    }
}
