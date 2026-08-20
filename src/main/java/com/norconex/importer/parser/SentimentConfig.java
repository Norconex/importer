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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * <p>
 * Sentiment analysis configuration for Tika's
 * {@code SentimentAnalysisParser}. This feature is intentionally
 * <b>disabled by default</b> because the parser downloads a model from the
 * network when it initializes.
 * </p>
 * <p>
 * To enable it, set {@link #setEnabled(boolean) enabled} to {@code true}
 * and optionally override the default model path with
 * {@link #setModelPath(String) modelPath}.
 * </p>
 *
 * @since 3.2.0
 */
public class SentimentConfig {

    /** Default Tika sentiment model URL. */
    public static final String DEFAULT_MODEL_PATH = "https://raw.githubusercontent.com/USCDataScience/"
            + "SentimentAnalysisParser/master/sentiment-models/"
            + "src/main/resources/edu/usc/irds/sentiment/"
            + "en-netflix-sentiment.bin";

    private boolean enabled = false;
    private String modelPath = DEFAULT_MODEL_PATH;

    /**
     * Returns whether sentiment analysis is enabled.
     * 
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether sentiment analysis is enabled.
     * 
     * @param enabled {@code true} to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the model path or URL used to initialize the sentiment model.
     * 
     * @return model path or URL
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * Sets the model path or URL used to initialize the sentiment model.
     * 
     * @param modelPath model path or URL
     */
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public boolean isEmpty() {
        return !enabled && StringUtils.isBlank(modelPath);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof SentimentConfig)) {
            return false;
        }
        SentimentConfig castOther = (SentimentConfig) other;
        return new EqualsBuilder()
                .append(enabled, castOther.enabled)
                .append(modelPath, castOther.modelPath)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(enabled)
                .append(modelPath)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("enabled", enabled)
                .append("modelPath", modelPath)
                .toString();
    }
}
