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

import java.util.ArrayList;
import java.util.List;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.importer.doc.ImporterMetadata;

/**
 * Commonly encountered restrictions that can be applied to subclass instances
 * of {@link AbstractImporterHandler}. Each method return a newly created
 * list that can safely be modified without impacting subsequent calls.
 * 
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public final class CommonRestrictions {

    private CommonRestrictions() {
    }
    
    /**
     * Default content-types defining a DOM document. That is, documents that
     * are HTML, XHTML, or XML-based. The <code>document.contentType</code>
     * field has to contain one of these for the restriction to apply:
     * <pre>
     * text/html, application/xhtml+xml, vnd.wap.xhtml+xml, x-asp,
     * application/xml, text/xml, application/atom+xml, application/xslt+xml,
     * image/svg+xml, application/mathml+xml, application/rss+xml
     * </pre>
     * @return list of restrictions
     */
    public static List<PropertyMatcher> domContentTypes() {
        return build(ImporterMetadata.DOC_CONTENT_TYPE,
                ContentType.HTML.toString(),
                "application/xhtml\\+xml",
                "vnd\\.wap.xhtml\\+xml",
                "x-asp",
                "application/xml",
                "text/xml",
                "application/atom\\+xml",
                "application/xslt\\+xml",
                "image/svg\\+xml",
                "application/mathml\\+xml",
                "application/rss\\+xml");
    }
  
    private static List<PropertyMatcher> build(
            String field, String... regexes) {
        List<PropertyMatcher> list = new ArrayList<>();
        for (String regex : regexes) {
            list.add(new PropertyMatcher(field, regex, false));
        }
        return list;
    }
}
