/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.importer.parser.impl;

import org.apache.tika.parser.html.HtmlParser;

/**
 * HTML parser based on Apache Tika {@link HtmlParser}.
 * @author Pascal Essiembre
 * @deprecated since 2.1.0.  Now handled by FallbackParser by default.
 */
@Deprecated
public class HTMLParser extends AbstractTikaParser {

    public HTMLParser() {
        super(new HtmlParser());
    }

}
