/* Copyright 2015-2017 Norconex Inc.
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

import java.io.IOException;
import java.util.Objects;

import javax.script.Bindings;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

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
 *   <li><b>metadata:</b> Document metadata as a {@link ImporterMetadata}
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
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ScriptTransformer"
 *          engineName="(script engine name)"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max content characters to read at once)" &gt;
 *          
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *      
 *      &lt;script&gt;(your script)&lt;/script&gt;
 *      
 *  &lt;/transformer&gt;
 * </pre>
 * 
 * <h4>Usage example:</h4>
 * <p>The following example replaces all occurences of "Alice" with "Roger"
 * in a document content.</p>
 * <h5>JavaScript:</h5>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ScriptTransformer"&gt;
 *    &lt;script&gt;&lt;![CDATA[
 *        modifiedContent = content.replace(/Alice/g, 'Roger');
 *        /&#42;return&#42;/ modifiedContent;
 *    ]]&gt;&lt;/script&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * <h5>Lua:</h5>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ScriptTransformer"
 *      engineName="lua"&gt;
 *    &lt;script&gt;&lt;![CDATA[
 *        modifiedContent = content:gsub('Alice', 'Roger');
 *        return modifiedContent;
 *    ]]&gt;&lt;/script&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.4.0
 * @see ScriptRunner
 */
public class ScriptTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private final ScriptRunner<String> scriptRunner = new ScriptRunner<>();
    
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
    protected void transformStringContent(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) throws ImporterHandlerException {
        
        String originalContent = content.toString();
        Bindings b = scriptRunner.createBindings();
        b.put("reference", reference);
        b.put("content", originalContent);
        b.put("metadata", metadata);
        b.put("parsed", parsed);
        b.put("sectionIndex", sectionIndex);
        String modifiedContent = scriptRunner.eval(b);
        if (!Objects.equals(originalContent, modifiedContent)) {
            content.setLength(0);
            content.append(modifiedContent);
        }
    }

    @Override
    protected void saveStringTransformerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeString("engineName", getEngineName());
        writer.writeElementString("script", getScript());
    }

    @Override
    protected void loadStringTransformerFromXML(XMLConfiguration xml)
            throws IOException {
        setEngineName(xml.getString("[@engineName]", getEngineName()));
        setScript(xml.getString("script"));
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ScriptTransformer)) {
            return false;
        }
        ScriptTransformer castOther = (ScriptTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(scriptRunner, castOther.scriptRunner)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(scriptRunner)
                .toHashCode();
    }
    @Override
    public String toString() {
        return new ToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("scriptRunner", scriptRunner)
                .toString();
    } 
}
