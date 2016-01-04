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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.encryption.StandardSecurity;
import org.sejda.sambox.encryption.StandardSecurityEncryption;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.output.WriteOption;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * @author Andrea Vacondio
 *
 */
@RunWith(Parameterized.class)
public class ShakeDownTest
{

    @Parameters
    public static Collection<Object[]> input()
    {
        return Arrays.asList(new String[][] { { "/sambox/simple_test.pdf", "" },
                { "/sambox/simple_test_objstm.pdf", "" },
                { "/sambox/encrypted_simple_test.pdf", "" },
                { "/sambox/encrypted_with_user.pdf", "test" } });
    }

    private List<StandardSecurity> securities = Arrays.asList(
            new StandardSecurity("owner", "user", StandardSecurityEncryption.ARC4_128, true),
            new StandardSecurity("owner", null, StandardSecurityEncryption.ARC4_128, true),
            new StandardSecurity("owner", "user", StandardSecurityEncryption.AES_128, true),
            new StandardSecurity("owner", null, StandardSecurityEncryption.ARC4_128, true));
    @Parameter
    public String inputFile;
    @Parameter(value = 1)
    public String pwd;

    @Test
    public void write() throws IOException
    {
        doTest();
        doTestEncrypted();
    }

    @Test
    public void writeObjectStream() throws IOException
    {
        doTest(WriteOption.OBJECT_STREAMS);
        doTestEncrypted(WriteOption.OBJECT_STREAMS);
    }

    @Test
    public void writeCompressed() throws IOException
    {
        doTest(WriteOption.COMPRESS_STREAMS);
        doTestEncrypted(WriteOption.COMPRESS_STREAMS);
    }

    @Test
    public void writeObjectStreamCompressed() throws IOException
    {
        doTest(WriteOption.OBJECT_STREAMS, WriteOption.COMPRESS_STREAMS);
        doTestEncrypted(WriteOption.OBJECT_STREAMS, WriteOption.COMPRESS_STREAMS);
    }

    @Test
    public void writeSync() throws IOException
    {
        doTest(WriteOption.SYNC_BODY_WRITE);
        doTestEncrypted(WriteOption.SYNC_BODY_WRITE);
    }

    @Test
    public void writeXrefStream() throws IOException
    {
        doTest(WriteOption.XREF_STREAM);
        doTestEncrypted(WriteOption.XREF_STREAM);
    }

    private void doTest(WriteOption... options) throws IOException
    {
        try (PDDocument current = PDFParser.parse(SeekableSources
                .inMemorySeekableSourceFrom(getClass().getResourceAsStream(inputFile)), pwd))
        {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream())
            {
                current.writeTo(out, options);
                try (PDDocument outDoc = PDFParser
                        .parse(SeekableSources.inMemorySeekableSourceFrom(out.toByteArray())))
                {
                    assertTrue(outDoc.getNumberOfPages() > 0);
                }
            }
        }
    }

    private void doTestEncrypted(WriteOption... options) throws IOException
    {
        for (StandardSecurity security : securities)
        {
            doTestEncrypted(security, options);
        }
    }

    private void doTestEncrypted(StandardSecurity security, WriteOption... options)
            throws IOException
    {
        try (PDDocument current = PDFParser.parse(SeekableSources
                .inMemorySeekableSourceFrom(getClass().getResourceAsStream(inputFile)), pwd))
        {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream())
            {
                current.writeTo(out, security, options);

                try (PDDocument outDoc = PDFParser.parse(
                        SeekableSources.inMemorySeekableSourceFrom(out.toByteArray()),
                        new String(security.userPassword)))
                {
                    assertTrue(outDoc.getNumberOfPages() > 0);
                }
            }
        }
    }
}
