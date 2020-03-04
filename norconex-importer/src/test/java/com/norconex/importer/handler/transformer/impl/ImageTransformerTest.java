/* Copyright 2019-2020 Norconex Inc.
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

import java.awt.Dimension;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;

public class ImageTransformerTest {

    @Test
    public void testWriteRead() {
        ImageTransformer t = new ImageTransformer();
        t.setCropRectangle(new Rectangle(10, 15, 400, 250));
        t.setRotateDegrees(-90.0);
        t.setScaleDimension(new Dimension(800,  600));
        t.setScaleFactor(0.5);
        t.setScaleStretch(true);
        t.setTargetFormat("jpg");
        XML.assertWriteRead(t, "handler");
    }
}
