/* Copyright 2010-2018 Norconex Inc.
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
package com.norconex.importer.handler.filter;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.xml.XML;


/**
 * Convenience base class for implementing filters offering the include/exclude
 * "onmatch" option.  Default behavior on match is to include.
 * @author Pascal Essiembre
 * @deprecated Use composition with OnMatch instead
 */
@Deprecated
public abstract class AbstractOnMatchFilter implements IOnMatchFilter {

	private OnMatch onMatch = OnMatch.INCLUDE;

	@Override
    public OnMatch getOnMatch() {
		return onMatch;
	}

	public final void setOnMatch(OnMatch onMatch) {
		if (onMatch == null) {
			throw new IllegalArgumentException(
					"OnMatch argument cannot be null.");
		}
		this.onMatch = onMatch;
	}


    /**
     * Convenience method for subclasses to load the "onMatch"
     * attribute from an XML file when {@link XML} is used.
     * @param xml XML configuration
     */
    protected final void loadOnMatchFromXML(XML xml) {
        OnMatch configOnMatch = OnMatch.INCLUDE;
        String onMatchStr = xml.getString(
                "@onMatch", OnMatch.INCLUDE.toString()).toUpperCase();
        try {
            configOnMatch = OnMatch.valueOf(onMatchStr);
        } catch (IllegalArgumentException e)  {
            throw new ConfigurationException("Configuration error: "
                    + "Invalid \"onMatch\" attribute value: \"" + onMatchStr
                    + "\".  Must be one of \"include\" or \"exclude\".", e);
        }
        this.onMatch = configOnMatch;
    }

    /**
     * Convenience method for subclasses to save the "onMatch" attribute
     * to an XML file when {@link XML} is used.
     * @param xml the XML
     */
    protected void saveOnMatchToXML(XML xml) {
        xml.setAttribute("onMatch", onMatch.toString().toLowerCase());
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
        return new ReflectionToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
