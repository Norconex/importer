/* Copyright 2010-2014 Norconex Inc.
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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.XMLConfiguration;

import com.norconex.commons.lang.config.ConfigurationException;


/**
 * Convenience base class for implementing filters offering the include/exclude
 * "onmatch" option.  Default behavior on match is to include.
 * @author Pascal Essiembre
 */
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
     * attribute from an XML file when {@link XMLConfiguration} is used.
     * @param xml XML configuration
     */
    protected final void loadFromXML(XMLConfiguration xml) {
        OnMatch configOnMatch = OnMatch.INCLUDE;
        String onMatchStr = xml.getString(
                "[@onMatch]", OnMatch.INCLUDE.toString()).toUpperCase();
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
     * to an XML file when {@link XMLConfiguration} is used.
     * @param writer XML stream writer
     * @throws XMLStreamException problem saving extra content types
     */
    protected void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute("onMatch", onMatch.toString().toLowerCase()); 
    }
	
    @Override
    public String toString() {
        return "AbstractOnMatchFilter [onMatch=" + onMatch + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((onMatch == null) ? 0 : onMatch.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractOnMatchFilter other = (AbstractOnMatchFilter) obj;
        if (onMatch != other.onMatch) {
            return false;
        }
        return true;
    }
}
