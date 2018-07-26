/* Copyright 2015-2018 Norconex Inc.
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
package com.norconex.importer.handler.filter.impl;

import javax.script.Bindings;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.handler.filter.AbstractStringFilter;

/**
 * <p>
 * Filter incoming documents using a scripting language.
 * The default script engine is <code>JavaScript</code>.
 * </p><p>
 * Refer to {@link ScriptRunner} for more information on using a scripting
 * language with Norconex Importer.
 * </p>
 * <h3>How to filter documents with scripting:</h3>
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
 * The expected <b>return value</b> from your script is a boolean indicating
 * whether the document was matched or not.
 * </p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.ScriptFilter"
 *          engineName="(script engine name)"
 *          onMatch="[include|exclude]"
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
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <h5>JavaScript:</h5>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.ScriptFilter"&gt;
 *    &lt;script&gt;&lt;![CDATA[
 *      var isAppleDoc = metadata.getString('fruit') == 'apple'
 *              || content.indexOf('Apple') &gt; -1;
 *      /&#42;return&#42;/ isAppleDoc;
 *    ]]&gt;&lt;/script&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h5>Lua:</h5>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.filter.impl.ScriptFilter"
 *      engineName="lua"&gt;
 *    &lt;script&gt;&lt;![CDATA[
 *      local isAppleDoc = metadata:getString('fruit') == 'apple'
 *              and content:find('Apple') ~= nil;
 *      return isAppleDoc;
 *    ]]&gt;&lt;/script&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.4.0
 * @see ScriptRunner
 */
public class ScriptFilter extends AbstractStringFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptFilter.class);

    private final ScriptRunner<Object> scriptRunner = new ScriptRunner<>();

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
    protected boolean isStringContentMatching(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) throws ImporterHandlerException {

        Bindings b = scriptRunner.createBindings();
        b.put("reference", reference);
        b.put("content", content.toString());
        b.put("metadata", metadata);
        b.put("parsed", parsed);
        b.put("sectionIndex", sectionIndex);
        Object obj = scriptRunner.eval(b);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Returned object from ScriptFilter: {}", obj);
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean) {
            return (boolean) obj;
        }
        return Boolean.valueOf(obj.toString());
    }

    @Override
    protected void saveStringFilterToXML(XML xml) {
        xml.setAttribute("engineName", getEngineName());
        xml.addElement("script", getScript());
    }

    @Override
    protected void loadStringFilterFromXML(XML xml) {
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
