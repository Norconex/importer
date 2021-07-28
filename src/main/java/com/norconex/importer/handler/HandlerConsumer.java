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
import static com.norconex.importer.ImporterEvent.IMPORTER_HANDLER_END;
import static com.norconex.importer.ImporterEvent.IMPORTER_HANDLER_ERROR;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.function.FunctionUtil;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.IOUtil;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.flow.IXMLFlowConsumerAdapter;
import com.norconex.commons.lang.xml.flow.XMLFlow;
import com.norconex.importer.ImporterEvent;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.filter.IDocumentFilter;
import com.norconex.importer.handler.filter.IOnMatchFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.RejectFilter;
import com.norconex.importer.handler.splitter.IDocumentSplitter;
import com.norconex.importer.handler.tagger.IDocumentTagger;
import com.norconex.importer.handler.transformer.IDocumentTransformer;

/**
 * Consumer wrapping an {@link IImporterHandler} instance for use in an
 * {@link XMLFlow}.
 * @author Pascal Essiembre
 * @since 3.0.0
 */
public class HandlerConsumer
        implements IXMLFlowConsumerAdapter<HandlerContext> {

    private static final Logger LOG =
            LoggerFactory.getLogger(HandlerConsumer.class);

    private IImporterHandler handler;

    public HandlerConsumer() {
        super();
    }
    public HandlerConsumer(IImporterHandler handler) {
        super();
        this.handler = handler;
    }

    public IImporterHandler getHandler() {
        return handler;
    }
    public void setHandler(IImporterHandler handler) {
        this.handler = handler;
    }

    public static Consumer<HandlerContext> fromHandlers(
            IImporterHandler... importerHandlers) {
        return fromHandlers(importerHandlers == null
                ? Collections.emptyList() : Arrays.asList(importerHandlers));
    }
    public static Consumer<HandlerContext> fromHandlers(
            List<IImporterHandler> importerHandlers) {
        return FunctionUtil.allConsumers(Optional.ofNullable(importerHandlers)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(HandlerConsumer::new)
            .collect(Collectors.toList()));
    }

    @Override
    public void accept(HandlerContext ctx) {
        if (handler == null || ctx.isRejected()) {
            return;
        }

        fireEvent(ctx, IMPORTER_HANDLER_BEGIN);
        try {
            if (handler instanceof IDocumentTagger) {
                tagDocument(ctx, (IDocumentTagger) handler);
            } else if (handler instanceof IDocumentTransformer) {
                transformDocument(ctx, (IDocumentTransformer) handler);
            } else if (handler instanceof IDocumentSplitter) {
                splitDocument(ctx, (IDocumentSplitter) handler);
            } else if (handler instanceof IDocumentFilter) {
                acceptDocument(ctx, (IDocumentFilter) handler);
            } else {
                //TODO instead check if implementing right consumer
                // and invoke if so?
                LOG.error("Unsupported Import Handler: {}", handler);
            }
        } catch (ImporterHandlerException e) {
            fireEvent(ctx, IMPORTER_HANDLER_ERROR, e);
            ExceptionUtils.wrapAndThrow(e);
        } catch (Exception e) {
            fireEvent(ctx, IMPORTER_HANDLER_ERROR, e);
            ExceptionUtils.wrapAndThrow(new ImporterHandlerException(
                    "Importer failure for handler: " + handler, e));
        }
        fireEvent(ctx, IMPORTER_HANDLER_END);
    }

    private void tagDocument(HandlerContext ctx, IDocumentTagger tagger)
            throws ImporterHandlerException {
        tagger.tagDocument(
                new HandlerDoc(ctx.getDoc()),
                ctx.getDoc().getInputStream(),
                ctx.getParseState());
    }

    private void acceptDocument(
            HandlerContext ctx, IDocumentFilter filter)
                    throws ImporterHandlerException {
        boolean accepted = filter.acceptDocument(
                new HandlerDoc(ctx.getDoc()),
                ctx.getDoc().getInputStream(),
                ctx.getParseState());
        if (isMatchIncludeFilter(filter)) {
            ctx.getIncludeResolver().setHasIncludes(true);
            if (accepted) {
                ctx.getIncludeResolver().setAtLeastOneIncludeMatch(true);
            }
            return;
        }
        // Deal with exclude and non-OnMatch filters
        if (!accepted){
            ctx.setRejectedBy(filter);
            LOG.debug("Document import rejected. Filter: {}", filter);
        }
    }

    private void transformDocument(
            HandlerContext ctx, IDocumentTransformer transformer)
                    throws ImporterHandlerException, IOException {
        CachedInputStream in = ctx.getDoc().getInputStream();
        try (CachedOutputStream out =
                ctx.getDoc().getStreamFactory().newOuputStream()) {
            transformer.transformDocument(
                    new HandlerDoc(ctx.getDoc()), in, out, ctx.getParseState());
            CachedInputStream newInputStream = null;
            if (out.isCacheEmpty()) {
                LOG.debug("Transformer \"{}\" returned no content for: {}.",
                        transformer.getClass(), ctx.getDoc().getReference());
                IOUtil.closeQuietly(out);
                newInputStream = in;
            } else {
                in.dispose();
                newInputStream = out.getInputStream();
                IOUtil.closeQuietly(out);
            }
            ctx.getDoc().setInputStream(newInputStream);
        }
    }

    private void splitDocument(
            HandlerContext ctx, IDocumentSplitter splitter)
                    throws ImporterHandlerException, IOException {
        List<Doc> childDocs = null;
        CachedInputStream in = ctx.getDoc().getInputStream();
        try (CachedOutputStream out =
                ctx.getDoc().getStreamFactory().newOuputStream()) {
            childDocs = splitter.splitDocument(
                    new HandlerDoc(ctx.getDoc()), in, out, ctx.getParseState());
            // If writing was performed, get new content
            if (!out.isCacheEmpty()) {
                ctx.getDoc().setInputStream(out.getInputStream());
                in.dispose();
            }
        }
        if (childDocs != null) {
            for (int i = 0; i < childDocs.size(); i++) {
                Properties meta = childDocs.get(i).getMetadata();
                meta.add(DocMetadata.EMBEDDED_INDEX, i);
                meta.add(DocMetadata.EMBEDDED_PARENT_REFERENCES,
                        ctx.getDoc().getReference());
            }
            ctx.getChildDocs().addAll(childDocs);
        }
    }

    private boolean isMatchIncludeFilter(IDocumentFilter filter) {
        return filter instanceof IOnMatchFilter
                && OnMatch.INCLUDE == ((IOnMatchFilter) filter).getOnMatch();
    }

    private void fireEvent(HandlerContext ctx, String eventName) {
        fireEvent(ctx, eventName, null);
    }
    private void fireEvent(
            HandlerContext ctx, String eventName, Exception e) {
        ctx.getEventManager().fire(
                new ImporterEvent.Builder(eventName, ctx.getDoc())
                    .subject(handler)
                    .parseState(ctx.getParseState())
                    .exception(e)
                    .build());
    }

    @Override
    public void loadFromXML(XML xml) {
        if (xml.getName().equals("reject")) {
            this.handler = RejectFilter.INSTANCE;
        } else {
            this.handler = xml.toObjectImpl(IImporterHandler.class);
        }
        if (this.handler == null) {
            LOG.warn("Importer handler from the following XML resolved to null "
                    + "and will have no effect: {}", xml);
        }
    }
    @Override
    public void saveToXML(XML xml) {
        if (handler instanceof RejectFilter) {
            xml.rename("reject");
        } else {
            xml.rename("handler");
            xml.setAttribute("class", handler.getClass().getName());
            if (handler instanceof IXMLConfigurable) {
                ((IXMLConfigurable) handler).saveToXML(xml);
            }
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
