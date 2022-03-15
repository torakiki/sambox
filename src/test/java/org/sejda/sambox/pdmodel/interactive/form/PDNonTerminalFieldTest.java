/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.pdmodel.interactive.form;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Andrea Vacondio
 */
public class PDNonTerminalFieldTest
{
    @Test
    public void remove() throws IOException
    {
        try (PDDocument document = new PDDocument())
        {
            PDAcroForm form = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(form);
            PDNonTerminalField b = new PDNonTerminalField(form);
            b.setPartialName("B");
            PDNonTerminalField d = new PDNonTerminalField(form);
            d.setPartialName("D");
            PDNonTerminalField g = new PDNonTerminalField(form);
            g.setPartialName("G");
            PDNonTerminalField i = new PDNonTerminalField(form);
            i.setPartialName("I");
            PDTextField a = new PDTextField(form);
            a.setPartialName("A");
            PDTextField c = new PDTextField(form);
            c.setPartialName("C");
            PDTextField h = new PDTextField(form);
            h.setPartialName("H");
            PDTextField e = new PDTextField(form);
            e.setPartialName("E");
            PDNonTerminalField f = new PDNonTerminalField(form);
            f.setPartialName("F");
            b.addChild(a);
            b.addChild(d);
            d.addChild(c);
            d.addChild(e);
            g.addChild(i);
            i.addChild(h);
            f.addChild(b);
            f.addChild(g);
            form.addFields(Arrays.asList(f));
            assertNotNull(form.getField("F.G.I.H"));
            assertNull(i.removeChild(PDFieldFactory.createField(form, new COSDictionary(), null)));
            assertNotNull(i.removeChild(PDFieldFactory.createField(form, h.getCOSObject(), null)));
            assertNull(form.getField("F.G.I.H"));
        }
    }

    @Test
    void setPartialName()
    {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    new PDNonTerminalField(null).setPartialName("this.that");
                });

        assertThat(thrown.getMessage(),
                containsString("A field partial name shall not contain a period character"));
    }

}
