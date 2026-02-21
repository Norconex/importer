/* Copyright 2024 Norconex Inc.
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
 * <p>
 * Grobid REST service configuration. When enabled, the importer will use
 * the <a href="https://grobid.readthedocs.io/">Grobid</a> REST service
 * (via the Tika {@code GrobidRESTParser} and {@code JournalParser}) to
 * extract metadata and structured text from scientific documents such as PDF
 * journal articles.
 * </p>
 * <p>
 * Grobid parsing is <b>disabled by default</b>. To enable it, set
 * {@link #setEnabled(boolean) enabled} to {@code true} and make sure a
 * Grobid service is running at the configured
 * {@link #setServiceUrl(String) serviceUrl}
 * (default: {@value #DEFAULT_SERVICE_URL}).
 * </p>
 *
 * @since 3.2.0
 */
public class GrobidConfig {

    /** Default Grobid REST service URL. */
    public static final String DEFAULT_SERVICE_URL = "http://localhost:8070";

    private boolean enabled = false;
    private String serviceUrl = DEFAULT_SERVICE_URL;

    /**
     * Returns whether Grobid parsing is enabled.
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    /**
     * Sets whether Grobid parsing is enabled.
     * @param enabled {@code true} to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the Grobid REST service base URL.
     * @return service URL
     */
    public String getServiceUrl() {
        return serviceUrl;
    }
    /**
     * Sets the Grobid REST service base URL.
     * @param serviceUrl service URL (e.g. {@code http://myhost:8070})
     */
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof GrobidConfig)) {
            return false;
        }
        GrobidConfig castOther = (GrobidConfig) other;
        return new EqualsBuilder()
                .append(enabled, castOther.enabled)
                .append(serviceUrl, castOther.serviceUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(enabled)
                .append(serviceUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("enabled", enabled)
                .append("serviceUrl", serviceUrl)
                .toString();
    }
}
