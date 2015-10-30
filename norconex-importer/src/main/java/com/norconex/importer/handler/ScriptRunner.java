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
package com.norconex.importer.handler;

import java.util.List;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.importer.handler.filter.impl.ScriptFilter;
import com.norconex.importer.handler.tagger.impl.ScriptTagger;
import com.norconex.importer.handler.transformer.impl.ScriptTransformer;

/**
 * <p>
 * Runs scripts written in a programming language supported by the provided
 * script engine. Java SE 6 and higher includes the 
 * <a href="https://jcp.org/en/jsr/detail?id=223">JSR 223</a> API, which 
 * allows to "plug" any script engines to support your favorite scripting
 * language.
 * </p><p>
 * The "JavaScript" script engine should already be present as part of 
 * your Java installation and is the default script engine used by this class
 * when none is specified. The Oracle implementation of Java includes
 * a JavaScript engine based on Mozilla Rhino and you can find extensive 
 * documentation on it on the 
 * <a href="https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino">
 * Mozilla Rhino</a> site.
 * </p><p>
 * Several third-party script engines already exist to support additional
 * languages such as Groovy, JRuby, Scala, Fantom, Jython, etc. Refer to 
 * appropriate third-party documentation about these languages to find
 * out how to use them.
 * </p><p>
 * <b>Note:</b> While using a scripting language can be very convenient, it 
 * requires extra knowledge and should only be considered by 
 * experimented or adventurous users. <b>Use at your own risk.</b>
 * </p>
 * 
 * @author Pascal Essiembre
 * @param <T> The evaluation response type.
 * @since 2.4.0
 * @see ScriptFilter
 * @see ScriptTagger
 * @see ScriptTransformer
 */
public class ScriptRunner<T> {

    private static final Logger LOG = LogManager.getLogger(ScriptRunner.class);
    
    public static final String DEFAULT_SCRIPT_ENGINE = "JavaScript";
    
    private ScriptEngine engine;
    private CompiledScript compiledScript;
    
    private String engineName = DEFAULT_SCRIPT_ENGINE;
    private String script;
    
    public ScriptRunner() {
        super();
    }
    public ScriptRunner(String engineName) {
        super();
        this.engineName = engineName;
    }

    public String getEngineName() {
        return engineName;
    }
    public void setEngineName(String engineName) {
        this.engineName = engineName;
        this.engine = null;
    }

    public String getScript() {
        return script;
    }
    public void setScript(String script) {
        this.script = script;
        this.engine = null;
    }

    public Bindings createBindings() throws ImporterHandlerException {
        ensureScriptEngine();
        return engine.createBindings();
    }
    
    @SuppressWarnings("unchecked")
    public T eval(Bindings bindings) throws ImporterHandlerException {
        try {
            ensureScriptEngine();
            T value = null;
            if (compiledScript != null) {
                value = (T) compiledScript.eval(bindings);
            } else {
                value = (T) engine.eval(script, bindings);
            }
            return value;
        } catch (ScriptException e) {
            throw new ImporterHandlerException("Script execution error.", e);
        }
    }
 
    private synchronized void ensureScriptEngine()
            throws ImporterHandlerException {
        if (engine != null) {
            return;
        }
        String engineName = this.engineName;
        if (StringUtils.isBlank(engineName)) {
            engineName = DEFAULT_SCRIPT_ENGINE;
        }
        engine = new ScriptEngineManager().getEngineByName(engineName);
        
        if (engine == null) {
            StringBuilder b = new StringBuilder();
            ScriptEngineManager mgr = new ScriptEngineManager();
            List<ScriptEngineFactory> factories = mgr.getEngineFactories();
            for (ScriptEngineFactory factory: factories) {
                String engName = factory.getEngineName();
                String engVersion = factory.getEngineVersion();
                String langName = factory.getLanguageName();
                String langVersion = factory.getLanguageVersion();
                b.append("\n\tScript Engine: ");
                b.append(engName);
                b.append(" (" + engVersion + ")\n");
                b.append("\t      Aliases: ");
                b.append(StringUtils.join(factory.getNames(), ", "));
                b.append("\n\t     Language: ");
                b.append(langName);
                b.append(" (" + langVersion + ")");
            }
            LOG.error("Invalid Script Engine \"" + engineName 
                    + "\". Detected Script Engines are:\n" + b.toString());
            throw new ImporterHandlerException(
                    "No Script Engine found in your JVM matching the name \""
                    + engineName + "\".");
        }
        
        if (engine instanceof Compilable) {
            Compilable compileEngine = (Compilable) engine;
            try {
                compiledScript = compileEngine.compile(script);
            } catch (ScriptException e) {
                throw new ImporterHandlerException(
                        "Could not compile script.", e);
            }
        }
    }


    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ScriptRunner)) {
            return false;
        }
        ScriptRunner<?> castOther = (ScriptRunner<?>) other;
        return new EqualsBuilder()
                .append(engineName, castOther.engineName)
                .append(script, castOther.script)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(engineName)
                .append(script)
                .toHashCode();
    }
    private transient String toString;
    @Override
    public String toString() {
        if (toString == null) {
            toString = new ToStringBuilder(
                    this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("engineName", engineName)
                    .append("script", script)
                    .toString();
        }
        return toString;
    }
}