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
package com.norconex.importer.handler;

import static com.norconex.importer.ImporterEvent.IMPORTER_HANDLER_BEGIN;
import static com.norconex.importer.ImporterEvent.IMPORTER_HANDLER_CONDITION_FALSE;
import static com.norconex.importer.ImporterEvent.IMPORTER_HANDLER_CONDITION_TRUE;
import static com.norconex.importer.ImporterEvent.IMPORTER_HANDLER_END;
import static com.norconex.importer.ImporterEvent.IMPORTER_HANDLER_ERROR;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.flow.IXMLFlowPredicateAdapter;
import com.norconex.commons.lang.xml.flow.XMLFlow;
import com.norconex.importer.ImporterEvent;
import com.norconex.importer.handler.condition.IImporterCondition;
import com.norconex.importer.handler.condition.impl.TextCondition;

/**
 * Predicate wrapping an {@link IImporterCondition} instance for use in an
 * {@link XMLFlow}.
 * @author Pascal Essiembre
 * @since 3.0.0
 */
public class HandlerPredicate
        implements IXMLFlowPredicateAdapter<HandlerContext> {

    private IImporterCondition condition;

    public HandlerPredicate() {
        super();
    }
    public HandlerPredicate(IImporterCondition condition) {
        super();
        this.condition = condition;
    }

    public IImporterCondition getCondition() {
        return condition;
    }

    @Override
    public boolean test(HandlerContext ctx) {
        if (condition == null || ctx.isRejected()) {
            return false;
        }

        fireEvent(ctx, IMPORTER_HANDLER_BEGIN);
        try {
            boolean result = condition.testDocument(
                    new HandlerDoc(ctx.getDoc()),
                    ctx.getDoc().getInputStream(),
                    ctx.getParseState());
            fireEvent(ctx, result
                    ? IMPORTER_HANDLER_CONDITION_TRUE
                    : IMPORTER_HANDLER_CONDITION_FALSE);
            return result;
        } catch (ImporterHandlerException e) {
            fireEvent(ctx, IMPORTER_HANDLER_ERROR, e);
            ExceptionUtils.wrapAndThrow(e);
        } catch (Exception e) {
            fireEvent(ctx, IMPORTER_HANDLER_ERROR, e);
            ExceptionUtils.wrapAndThrow(new ImporterHandlerException(
                    "Importer failure for handler condition: " + condition, e));
        }
        fireEvent(ctx, IMPORTER_HANDLER_END);
        return false;
    }

    private void fireEvent(HandlerContext ctx, String eventName) {
        fireEvent(ctx, eventName, null);
    }
    private void fireEvent(
            HandlerContext ctx, String eventName, Exception e) {
        ctx.getEventManager().fire(
                new ImporterEvent.Builder(eventName, ctx.getDoc())
                    .subject(condition)
                    .parseState(ctx.getParseState())
                    .exception(e)
                    .build());
    }

    @Override
    public void loadFromXML(XML xml) {
        this.condition = xml.toObjectImpl(IImporterCondition.class);
        if (this.condition == null) {
            this.condition = new TextCondition();
            xml.populate(condition);
        }
    }
    @Override
    public void saveToXML(XML xml) {
        xml.rename("condition");
        xml.setAttribute("class", condition.getClass().getName());
        if (condition instanceof IXMLConfigurable) {
            ((IXMLConfigurable) condition).saveToXML(xml);
        }
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
