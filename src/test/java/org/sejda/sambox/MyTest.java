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
package org.sejda.sambox;

import static java.util.Objects.nonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.sejda.io.BufferedSeekableSource;
import org.sejda.io.FileChannelSeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.output.WriteOption;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDDocumentNameDestinationDictionary;
import org.sejda.sambox.pdmodel.PDDocumentNameDictionary;
import org.sejda.sambox.pdmodel.PDJavascriptNameTreeNode;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.interactive.action.PDActionJavaScript;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.sejda.sambox.pdmodel.interactive.form.PDAcroForm;
import org.sejda.sambox.pdmodel.interactive.form.PDField;
import org.sejda.sambox.pdmodel.interactive.form.PDNonTerminalField;
import org.sejda.sambox.pdmodel.interactive.form.PDTextField;
import org.sejda.sambox.text.PDFTextStripper;

/**
 * @author Andrea Vacondio
 *
 */
@Ignore
public class MyTest
{

    @Test
    public void myTest() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(
                "/home/torakiki/Scaricati/Marketing_Management_-_Millenium_Edition (1).pdf"))))
        {
            PDDocumentNameDestinationDictionary dests = new PDDocumentNameDestinationDictionary(
                    new COSDictionary());
            PDPageFitDestination destination = new PDPageFitDestination();
            destination.setPage(doc.getPage(0));
            dests.getCOSObject().setItem(COSName.getPDFName("MyDest"), destination);
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(new COSDictionary());

            PDJavascriptNameTreeNode js = new PDJavascriptNameTreeNode();
            Map<String, PDActionJavaScript> jsnames = new HashMap<>();
            jsnames.put("name", new PDActionJavaScript());
            js.setNames(jsnames);
            names.setJavascript(js);
            doc.getDocumentCatalog().setNames(names);
            doc.getDocumentCatalog().getCOSObject().setItem(COSName.DESTS, dests.getCOSObject());
            doc.writeTo("/home/torakiki/Scrivania/test_incremental_xref/test_xref_w_dests.pdf",
                    WriteOption.OBJECT_STREAMS);
        }
    }

    @Test
    public void testExtract() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources
                .seekableSourceFrom(new File("/home/torakiki/Scaricati/obannual35_2015.pdf"))))
        {
            PDFTextStripper textStripper = new PDFTextStripper();
            try (OutputStreamWriter outputWriter = new OutputStreamWriter(
                    new FileOutputStream("/home/torakiki/Scaricati/obannual35_2015.txt")))
            {
                textStripper.writeText(document, outputWriter);
            }
        }
    }

    @Test
    public void testSimple() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources
                .seekableSourceFrom((new File("/home/torakiki/Scaricati/PDFBOX-3446.pdf")))))
        {
            document.writeTo("/home/torakiki/Scaricati/PDFBOX-3446_cmp.pdf",
                    WriteOption.OBJECT_STREAMS);
        }

    }

    @Test
    public void testSimple2() throws IOException
    {
        try (PDDocument document = PDFParser.parse(new BufferedSeekableSource(
                new FileChannelSeekableSource(new File("/home/torakiki/Scaricati/a_sambox.pdf")))))
        {
            PDAcroForm form = document.getDocumentCatalog().getAcroForm();
            for (PDField field : form.getFields())
            {
                List<PDAnnotationWidget> widgets = field.getWidgets();
                System.out.println(
                        "Field Name: " + field.getPartialName() + " (" + widgets.size() + ")");
                for (PDAnnotationWidget annot : widgets)
                {
                    PDAppearanceDictionary ap = annot.getAppearance();
                    COSDictionary n = Optional.ofNullable(ap)
                            .map(PDAppearanceDictionary::getCOSObject)
                            .map(d -> d.getDictionaryObject("N", COSDictionary.class))
                            .orElse(new COSDictionary());
                    if (nonNull(n))
                    {
                        Set<COSName> keys = n.keySet();
                        ArrayList keyList = new ArrayList(keys.size());
                        for (COSName cosKey : keys)
                        {
                            keyList.add(cosKey.getName());
                        }
                        System.out.println(String.join("|", keyList));
                    }

                }
            }

        }

    }

    @Test
    public void testHalloWorld() throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDAcroForm form = new PDAcroForm(doc);
            PDNonTerminalField nonTer = new PDNonTerminalField(form);
            nonTer.setPartialName("node");
            PDTextField field = new PDTextField(form);
            field.setPartialName("leaf");
            nonTer.addChild(field);
            form.setFields(Arrays.asList(nonTer));
            PDAnnotationWidget widget = new PDAnnotationWidget();
            widget.setAnnotationFlags(4);
            widget.setRectangle(new PDRectangle(34, 600, 290, 60));
            field.addWidgetIfMissing(widget);
            page.setAnnotations(Arrays.asList(widget));
            doc.getDocumentCatalog().setAcroForm(form);
            doc.writeTo("/home/torakiki/Scaricati/simple_form_with_hierarchy.pdf");
        }
    }
}
