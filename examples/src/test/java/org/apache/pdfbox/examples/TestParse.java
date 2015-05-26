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
package org.apache.pdfbox.examples;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.input.PDFParser;
import org.apache.pdfbox.input.source.SeekableSources;
import org.apache.pdfbox.output.WriteOption;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class TestParse
{
    private static final String FILEPATH = "/home/torakiki/Scrivania/test_incremental_xref/Responsive_sambox3.pdf";
    private static final String ENC_FILEPATH = "/home/torakiki/repos/sejda/sejda-core/src/test/resources/pdf/enc_usr_own_same_pwd.pdf";

    @Test
    public void testParse() throws IOException
    {
        File file = new File(FILEPATH);
        try (PDDocument document = PDFParser.parse(SeekableSources.seekableSourceFrom(file)))
        {
            document.writeTo(
                    "/home/torakiki/Scrivania/test_incremental_xref/Responsive_sambox5.pdf",
                    WriteOption.XREF_STREAM, WriteOption.SYNC_BODY_WRITE);
        }

    }

    @Test
    public void testParseEnc() throws IOException
    {
        File file = new File(ENC_FILEPATH);
        try (PDDocument document = PDFParser
                .parse(SeekableSources.seekableSourceFrom(file), "test"))
        {
            document.writeTo("/home/torakiki/Scrivania/test_incremental_xref/enc_sambox.pdf");
        }
    }

    @Test(expected = InvalidPasswordException.class)
    public void testWrongParseEnc() throws IOException
    {
        File file = new File(ENC_FILEPATH);
        try (PDDocument document = PDFParser
                .parse(SeekableSources.seekableSourceFrom(file), "test"))
        {
            document.writeTo("/home/torakiki/Scrivania/test_incremental_xref/enc_sambox.pdf");
        }
    }

}
