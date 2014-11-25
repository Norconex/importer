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
package com.norconex.importer.handler.tagger;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.MemoryUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.util.BufferUtil;

/**
 * <p>Base class to facilitate creating taggers based on text content, loading
 * text into {@link StringBuilder} for memory processing, also giving more 
 * options (like fancy regex).  This class checks for free memory every 10KB of 
 * text read.  If enough memory, it keeps going for another 10KB or until
 * all the content is read, or the buffer size reaches half the available 
 * memory.  In either case, it passes the buffered content so far for 
 * tagging (all of it for small enough content, or in several
 * chunks for large content).
 * </p>
 * <p>
 * Implementors should be conscious about memory when dealing with the string
 * builder.
 * </p>
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * <pre>
 *  &lt;contentTypeRegex&gt;
 *      (regex to identify text content-types, overridding default)
 *  &lt;/contentTypeRegex&gt;
 *  &lt;restrictTo
 *          caseSensitive="[false|true]" &gt;
 *          field="(name of header/metadata name to match)"
 *      (regular expression of value to match)
 *  &lt;/restrictTo&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public abstract class AbstractStringTagger 
            extends AbstractCharStreamTagger {

    //TODO try to share more with AbstractStringTransformer
    //TODO maybe: Add to importer config something about max buffer memory.
    // That way, we can ensure to apply the memory check technique on content of 
    // the same size (threads should be taken into account), 
    // as opposed to have one big file take all the memory so other big files
    // are forced to do smaller chunks at a time.
    
    private static final Logger LOG = 
            LogManager.getLogger(AbstractStringTagger.class);

    private static final int READ_CHUNK_SIZE = 10 * (int) FileUtils.ONE_KB;
    private static final int STRING_TOTAL_MEMORY_DIVIDER = 4;
    
    @Override
    protected final void tagTextDocument(
            String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        
        try {
            // Initial size is half free memory, considering chars take 2 bytes
            StringBuilder b = new StringBuilder(
                    (int)(MemoryUtil.getFreeMemory() 
                            / STRING_TOTAL_MEMORY_DIVIDER));
            int i;
            while ((i = input.read()) != -1) {
                char ch = (char) i;
                b.append(ch);
                if (b.length() * 2 % READ_CHUNK_SIZE == 0
                        && isTakingTooMuchMemory(b)) {
                    tagStringContent(reference, b, metadata, parsed, true);
                    BufferUtil.flushBuffer(b, null, true);
                }
            }
            if (b.length() > 0) {
                tagStringContent(reference, b, metadata, parsed, false);
                BufferUtil.flushBuffer(b, null, false);
            }
            b.setLength(0);
            b = null;
        } catch (IOException e) {
            throw new ImporterHandlerException("Cannot tag text document.", e);
        }
        
    }
    
    
    protected abstract void tagStringContent(
           String reference, StringBuilder content, ImporterMetadata metadata,
           boolean parsed, boolean partialContent) 
                   throws ImporterHandlerException;
    
    // We ensure buffer size never goes beyond half available memory.
    private boolean isTakingTooMuchMemory(StringBuilder b) {
        int maxMem = (int) MemoryUtil.getFreeMemory() / 2;
        int bufMem = b.length() * 2;
        boolean busted = bufMem > maxMem;
        if (busted) {
            LOG.warn("Text document read via tagger is quite big for "
                + "remaining JVM memory.  It was split in text chunks and "
                + "tagging will be applied on each chunk.  This "
                + "may sometimes result in unexpected tagging. "
                + "To eliminate this risk, increase the JVM maximum heap "
                + "space to more than double the processed content size "
                + "by using the -xmx flag to your startup script "
                + "(e.g. -xmx1024m for 1 Gigabyte).  In addition, "
                + "reducing the number of threads may help (if applicable). "
                + "As an alternative, you can also implement a new solution " 
                + "using AbstractCharSteamTagger instead, which relies "
                + "on streams (taking very little fixed-size memory when "
                + "done right).");
        }
        return busted;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractStringTagger)) {
            return false;
        }
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE)
            .appendSuper(super.toString())
            .toString();
    }
    

}