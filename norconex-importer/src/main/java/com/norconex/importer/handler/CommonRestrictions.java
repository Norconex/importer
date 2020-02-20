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
package com.norconex.importer.handler;

import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.map.PropertyMatchers;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.importer.doc.DocMetadata;

/**
 * Commonly encountered restrictions that can be applied to subclass instances
 * of {@link AbstractImporterHandler}. Each method return a newly created
 * list that can safely be modified without impacting subsequent calls.
 *
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public final class CommonRestrictions {

    private static final TextMatcher RX_NOCASE_PROTO =
            new TextMatcher(Method.REGEX).setIgnoreCase(true);


    private CommonRestrictions() {
    }

    /**
     * Default content-types defining a DOM document. That is, documents that
     * are HTML, XHTML, or XML-based. The <code>document.contentType</code>
     * field has to contain one of these for the restriction to apply:
     * <pre>
     * text/html, application/xhtml+xml, vnd.wap.xhtml+xml, application/x-asp,
     * application/xml, text/xml, application/atom+xml, application/xslt+xml,
     * image/svg+xml, application/mathml+xml, application/rss+xml
     * </pre>
     * @return list of restrictions
     */
    public static PropertyMatchers domContentTypes() {
        return regexesIgnoreCase(DocMetadata.CONTENT_TYPE,
                "text/html",
                "application/xhtml\\+xml",
                "application/vnd\\.wap.xhtml\\+xml",
                "application/x-asp",
                "application/xml",
                "text/xml",
                "application/atom\\+xml",
                "application/xslt\\+xml",
                "image/svg\\+xml",
                "application/mathml\\+xml",
                "application/rss\\+xml");
    }

    /**
     * Default content-types defining an HTML or XHTML document.
     * The <code>document.contentType</code>
     * field has to contain one of these for the restriction to apply:
     * <pre>
     * text/html, application/xhtml+xml, application/vnd.wap.xhtml+xml
     * </pre>
     * @return list of restrictions
     * @since 2.8.0
     */
    public static PropertyMatchers htmlContentTypes() {
        return regexesIgnoreCase(DocMetadata.CONTENT_TYPE,
                "text/html",
                "application/xhtml\\+xml",
                "application/vnd\\.wap.xhtml\\+xml");
    }

    /**
     * Content types of standard image format supported by all ImageIO
     * implementations: JPEG, PNG, GIF, BMP, WBMP.
     * The <code>document.contentType</code>
     * field has to contain one of these for the restriction to apply:
     * <pre>
     * image/jpeg, image/png, image/vnd.wap.wbmp, image/bmp,
     * image/x-windows-bmp, image/gif
     * </pre>
     * @return list of restrictions
     * @since 3.0.0
     */
    public static PropertyMatchers imageIOStandardContentTypes() {
        return regexesIgnoreCase(DocMetadata.CONTENT_TYPE,
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/bmp",
                "image/vnd.wap.wbmp",
                "image/x-windows-bmp");
    }

    private static PropertyMatchers regexesIgnoreCase(
            String key, String... regexes) {
        PropertyMatchers matchers = new PropertyMatchers();
        for (String regex : regexes) {
            matchers.add(new PropertyMatcher(
                    TextMatcher.basic(key),
                    RX_NOCASE_PROTO.withPattern(regex)));
        }
        return matchers;
    }
}
