/* Copyright 2015 Norconex Inc.
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

import javax.script.Bindings;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.handler.tagger.AbstractStringTagger;

/**
 * <p>
 * Tag incoming documents using a scripting language.
 * The default script engine is <code>JavaScript</code>. 
 * </p><p>
 * Refer to {@link ScriptRunner} for more information on using a scripting
 * language with Norconex Importer.
 * </p>
 * <h3>How to tag documents with scripting:</h3>
 * <p>
 * The following are variables made available to your script for each
 * document:
 * </p>
 * <ul>
 *   <li><b>reference:</b> Document unique reference as a string.</li>
 *   <li><b>content:</b> Document content, as a string 
 *       (of <code>maxReadSize</code> length).</li>
 *   <li><b>metadata:</b> Document metadata as a {@link ImporterMetadata}
 *       object.</li>
 *   <li><b>parsed:</b> Whether the document was already parsed, as a 
 *       boolean.</li>
 *   <li><b>sectionIndex:</b> Content section index if it had to be split, 
 *       as an integer.</li>
 * </ul>
 * <p>
 * There are no expected return value from your script. Returning
 * one has no effect.
 * </p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.ScriptTagger"
 *          engineName="(script engine name)"
 *          maxReadSize="(max content characters to read at once)" &gt;
 *      &lt;script&gt;(your script)&lt;/script&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * <h3>XML example:</h3>
 * <p>The following example simply adds new metadata field indicating which
 * fruit is a document about.</p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.ScriptTagger"&gt;
 *    &lt;script&gt;&lt;![CDATA[
 *        metadata.addString('fruit', 'apple');
 *    ]]&gt;&lt;/script&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.4.0
 * @see ScriptRunner
 */
public class ScriptTagger extends AbstractStringTagger {

    private final ScriptRunner<Void> scriptRunner = new ScriptRunner<>();
    
    public String getEngineName() {
        return scriptRunner.getEngineName();
    }
    public void setEngineName(String engineName) {
        scriptRunner.setEngineName(engineName);
    }

    public String getScript() {
        return scriptRunner.getScript();
    }
    public void setScript(String script) {
        scriptRunner.setScript(script);
    }

    @Override
    protected void tagStringContent(String reference, StringBuilder content,
            ImporterMetadata metadata, boolean parsed, int sectionIndex)
            throws ImporterHandlerException {
        Bindings b = scriptRunner.createBindings();
        b.put("reference", reference);
        b.put("content", content.toString());
        b.put("metadata", metadata);
        b.put("parsed", parsed);
        b.put("sectionIndex", sectionIndex);
        scriptRunner.eval(b);
    }

    @Override
    protected void saveStringTaggerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("engineName", getEngineName());
        writer.writeElementString("script", getScript());
    }

    @Override
    protected void loadStringTaggerFromXML(XMLConfiguration xml)
            throws IOException {
        setEngineName(xml.getString("[@engineName]", getEngineName()));
        setScript(xml.getString("script"));
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ScriptTagger)) {
            return false;
        }
        ScriptTagger castOther = (ScriptTagger) other;
        return new EqualsBuilder()
                .append(scriptRunner, castOther.scriptRunner)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(scriptRunner)
                .toHashCode();
    }
    private transient String toString;
    @Override
    public String toString() {
        if (toString == null) {
            toString = new ToStringBuilder(this)
                    .append("scriptRunner", scriptRunner)
                    .toString();
        }
        return toString;
    }
}
