/* Copyright 2015-2020 Norconex Inc.
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

import javax.script.Bindings;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.handler.tagger.AbstractStringTagger;
import com.norconex.importer.parser.ParseState;

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
 *   <li><b>metadata:</b> Document metadata as an {@link Properties}
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
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.ScriptTagger"
 *     engineName="(script engine name)"
 *     {@nx.include com.norconex.importer.handler.tagger.AbstractStringTagger#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <script>(your script)</script>
 *
 * </handler>
 * }
 *
 * <h4>Usage example:</h4>
 * <p>The following examples add new metadata field indicating which
 * fruit is a document about.</p>
 * <h5>JavaScript:</h5>
 * {@nx.xml
 * <handler class="com.norconex.importer.handler.tagger.impl.ScriptTagger">
 *   <script><![CDATA[
 *       metadata.add('fruit', 'apple');
 *   ]]></script>
 * </handler>
 * }
 * <h5>Lua:</h5>
 * {@nx.xml
 * <handler class="com.norconex.importer.handler.tagger.impl.ScriptTagger"
 *     engineName="lua">
 *   <script><![CDATA[
 *       metadata:addString('fruit', {'apple'});
 *   ]]></script>
 * </handler>
 * }
 *
 * @author Pascal Essiembre
 * @since 2.4.0
 * @see ScriptRunner
 */
@SuppressWarnings("javadoc")
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
    protected void tagStringContent(HandlerDoc doc, StringBuilder content,
            ParseState parseState, int sectionIndex)
                    throws ImporterHandlerException {
        Bindings b = scriptRunner.createBindings();
        b.put("reference", doc.getReference());
        b.put("content", content.toString());
        b.put("metadata", doc.getMetadata());
        b.put("parsed", parseState);
        b.put("sectionIndex", sectionIndex);
        scriptRunner.eval(b);
    }

    @Override
    protected void saveStringTaggerToXML(XML xml) {
        xml.setAttribute("engineName", getEngineName());
        xml.addElement("script", getScript());
    }

    @Override
    protected void loadStringTaggerFromXML(XML xml) {
        setEngineName(xml.getString("@engineName", getEngineName()));
        setScript(xml.getString("script", getScript()));
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
