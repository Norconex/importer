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
package com.norconex.importer.handler.transformer.impl;

import java.util.Objects;

import javax.script.Bindings;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;
import com.norconex.importer.parser.ParseState;

/**
 * <p>
 * Transform incoming documents using a scripting language.
 * The default script engine is <code>JavaScript</code>.
 * </p><p>
 * Refer to {@link ScriptRunner} for more information on using a scripting
 * language with Norconex Importer.
 * </p>
 * <h3>How to transform documents with scripting:</h3>
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
 * The expected <b>return value</b> from your script is a string holding
 * the modified content.
 * </p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.transformer.impl.ScriptTransformer"
 *     engineName="(script engine name)"
 *     {@nx.include com.norconex.importer.handler.transformer.AbstractStringTransformer#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <script>(your script)</script>
 *
 * </handler>
 * }
 *
 * <h4>Usage example:</h4>
 * <p>The following example replaces all occurences of "Alice" with "Roger"
 * in a document content.</p>
 * <h5>JavaScript:</h5>
 * {@nx.xml
 * <handler class="ScriptTransformer">
 *   <script><![CDATA[
 *       modifiedContent = content.replace(/Alice/g, 'Roger');
 *       /&#42;return&#42;/ modifiedContent;
 *   ]]></script>
 * </handler>
 * }
 * <h5>Lua:</h5>
 * {@nx.xml
 * <handler class="ScriptTransformer" engineName="lua">
 *   <script><![CDATA[
 *       modifiedContent = content:gsub('Alice', 'Roger');
 *       return modifiedContent;
 *   ]]></script>
 * </handler>
 * }
 *
 * @author Pascal Essiembre
 * @since 2.4.0
 * @see ScriptRunner
 */
@SuppressWarnings("javadoc")
public class ScriptTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private final ScriptRunner<String> scriptRunner = new ScriptRunner<>();

    public String getEngineName() {
        return scriptRunner.getEngineName();
    }
    public void setEngineName(final String engineName) {
        scriptRunner.setEngineName(engineName);
    }

    public String getScript() {
        return scriptRunner.getScript();
    }
    public void setScript(final String script) {
        scriptRunner.setScript(script);
    }

    @Override
    protected void transformStringContent(HandlerDoc doc,
            final StringBuilder content, final ParseState parseState,
            final int sectionIndex) throws ImporterHandlerException {

        String originalContent = content.toString();
        Bindings b = scriptRunner.createBindings();
        b.put("reference", doc.getReference());
        b.put("content", originalContent);
        b.put("metadata", doc.getMetadata());
        b.put("parsed", parseState);
        b.put("sectionIndex", sectionIndex);
        String modifiedContent = scriptRunner.eval(b);
        if (!Objects.equals(originalContent, modifiedContent)) {
            content.setLength(0);
            content.append(modifiedContent);
        }
    }

    @Override
    protected void saveStringTransformerToXML(
            final XML xml) {
        xml.setAttribute("engineName", getEngineName());
        xml.addElement("script", getScript());
    }

    @Override
    protected void loadStringTransformerFromXML(final XML xml) {
        setEngineName(xml.getString("@engineName", getEngineName()));
        setScript(xml.getString("script"));
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
