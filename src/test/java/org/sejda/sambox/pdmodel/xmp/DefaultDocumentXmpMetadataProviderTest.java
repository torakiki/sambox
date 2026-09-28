/*
 * Created on 24/09/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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
package org.sejda.sambox.pdmodel.xmp;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.type.ArrayProperty;
import org.apache.xmpbox.type.Cardinality;
import org.apache.xmpbox.xml.DomXmpParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.common.PDMetadata;

/**
 * Tests for {@link DefaultDocumentXmpMetadataProvider} when the existing XMP has Dublin Core array
 * properties written as simple text values, which the lenient parser accepts.
 */
public class DefaultDocumentXmpMetadataProviderTest
{
    private PDDocument document;

    @BeforeEach
    public void setUp() throws IOException
    {
        document = new PDDocument();
        document.getDocumentCatalog().setMetadata(new PDMetadata(
                requireNonNull(getClass().getResourceAsStream("text_typed_dc_properties.xml"))));
    }

    @AfterEach
    public void tearDown() throws IOException
    {
        document.close();
    }

    @Test
    public void authorIsAddedWhenExistingCreatorIsText() throws Exception
    {
        document.getDocumentInformation().setAuthor("Info Author");
        assertThat(writtenDublinCore().getCreators(), contains("Chuck Norris", "Info Author"));
    }

    @Test
    public void authorIsNotDuplicatedWhenExistingCreatorIsTextWithSameValue() throws Exception
    {
        document.getDocumentInformation().setAuthor("Chuck Norris");
        assertThat(writtenDublinCore().getCreators(), contains("Chuck Norris"));
    }

    @Test
    public void titleIsUpdatedWhenExistingTitleIsText() throws Exception
    {
        document.getDocumentInformation().setTitle("Info Title");
        assertEquals("Info Title", writtenDublinCore().getTitle());
    }

    @Test
    public void subjectIsWrittenToDescriptionWhenExistingDescriptionIsText() throws Exception
    {
        document.getDocumentInformation().setSubject("Info Subject");
        assertEquals("Info Subject", writtenDublinCore().getDescription());
    }

    @Test
    public void existingTextValuesAreKeptAsArraysWhenInfoHasNoValues() throws Exception
    {
        var dcSchema = writtenDublinCore();
        assertArrayProperty(dcSchema, DublinCoreSchema.CREATOR, Cardinality.Seq);
        assertArrayProperty(dcSchema, DublinCoreSchema.TITLE, Cardinality.Alt);
        assertArrayProperty(dcSchema, DublinCoreSchema.DESCRIPTION, Cardinality.Alt);
        assertThat(dcSchema.getCreators(), contains("Chuck Norris"));
        assertEquals("This is the title", dcSchema.getTitle());
        assertEquals("This is the description", dcSchema.getDescription());
    }

    private static void assertArrayProperty(DublinCoreSchema dcSchema, String name,
            Cardinality expected)
    {
        var property = assertInstanceOf(ArrayProperty.class, dcSchema.getAbstractProperty(name));
        assertEquals(expected, property.getArrayType());
    }

    private DublinCoreSchema writtenDublinCore() throws Exception
    {
        var metadata = new DefaultDocumentXmpMetadataProvider().xmpMetadataFor(document);
        assertNotNull(metadata);
        var parser = new DomXmpParser();
        parser.setStrictParsing(false);
        return parser.parse(metadata.createInputStream()).getDublinCoreSchema();
    }
}
