/* Copyright 2021 Norconex Inc.
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
package com.norconex.importer.handler.condition.impl;

import javax.script.Bindings;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.handler.condition.AbstractStringCondition;
import com.norconex.importer.parser.ParseState;

/**
 * <p>
 * A condition formulated using a scripting language.
 * The default script engine is <code>JavaScript</code>.
 * </p>
 * <p>
 * Refer to {@link ScriptRunner} for more information on using a scripting
 * language with Norconex Importer.
 * </p>
 * <h3>How to create a condition with scripting:</h3>
 * <p>
 * The following are variables made available to your script for each
 * document:
 * </p>
 * <ul>
 *   <li><b>reference:</b> Document unique reference as a string.</li>
 *   <li><b>content:</b> Document content, as a string
 *       (of <code>maxReadSize</code> length).</li>
 *   <li><b>metadata:</b> Document metadata as a {@link Properties}
 *       object.</li>
 *   <li><b>parsed:</b> Whether the document was already parsed, as a
 *       boolean.</li>
 *   <li><b>sectionIndex:</b> Content section index (integer) if it had to be
 *       split because it was too large.</li>
 * </ul>
 * <p>
 * The expected <b>return value</b> from your script is a boolean indicating
 * whether the document was matched or not.
 * </p>
 *
 * {@nx.xml.usage
 * <condition class="com.norconex.importer.handler.condition.impl.ScriptCondition"
 *   {@nx.include com.norconex.importer.handler.condition.AbstractStringCondition#attributes}
 *       engineName="(script engine name)">
 *   <script>(your script)</script>
 * </condition>
 * }
 *
 * {@nx.xml.example
 * <!-- Javascript: -->
 * <condition class="ScriptCondition">
 *   <script><![CDATA[
 *     var isAppleDoc = metadata.getString('fruit') == 'apple'
 *             || content.indexOf('Apple') > -1;
 *     // Return value:
 *     isAppleDoc;
 *   ]]></script>
 * </condition>
 *
 * <!-- Lua: -->
 * <condition class="ScriptCondition" engineName="lua">
 *   <script><![CDATA[
 *     local isAppleDoc = metadata:getString('fruit') == 'apple'
 *             and content:find('Apple') ~= nil;
 *     return isAppleDoc;
 *   ]]></script>
 * </condition>
 * }
 *
 * @author Pascal Essiembre
 * @since 3.0.0
 * @see ScriptRunner
 */
@SuppressWarnings("javadoc")
public class ScriptCondition extends AbstractStringCondition {

    private static final Logger LOG =
            LoggerFactory.getLogger(ScriptCondition.class);

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
    protected boolean testDocument(HandlerDoc doc, String input,
            ParseState parseState, int sectionIndex)
            throws ImporterHandlerException {
        Bindings b = scriptRunner.createBindings();
        b.put("reference", doc.getReference());
        b.put("content", input);
        b.put("metadata", doc.getMetadata());
        b.put("parsed", parseState);
        b.put("sectionIndex", sectionIndex);
        Object obj = scriptRunner.eval(b);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Returned object from ScriptCondition: {}", obj);
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
    protected void loadStringConditionFromXML(XML xml) {
        setEngineName(xml.getString("@engineName", getEngineName()));
        setScript(xml.getString("script", getScript()));
    }
    @Override
    protected void saveStringConditionToXML(XML xml) {
        xml.setAttribute("engineName", getEngineName());
        xml.addElement("script", getScript());
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
