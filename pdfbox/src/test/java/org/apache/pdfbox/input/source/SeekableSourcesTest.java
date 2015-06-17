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
package org.apache.pdfbox.input.source;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.pdfbox.PDFBox;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Andrea Vacondio
 *
 */
public class SeekableSourcesTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void nullSeekableSourceFrom() throws IOException
    {
        SeekableSources.seekableSourceFrom(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullInMemorySeekableSourceFromBytes()
    {
        SeekableSources.inMemorySeekableSourceFrom((byte[]) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullInMemorySeekableSourceFromStream() throws IOException
    {
        SeekableSources.inMemorySeekableSourceFrom((InputStream) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullOnTempFileSeekableSourceFrom() throws IOException
    {
        SeekableSources.onTempFileSeekableSourceFrom(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullInputStreamFrom()
    {
        SeekableSources.inputStreamFrom(null);
    }

    @Test
    public void seekableSourceFrom() throws IOException
    {
        assertNotNull(SeekableSources.seekableSourceFrom(temp.newFile()));
    }

    @Test
    public void aboveThresholdSeekableSourceFrom() throws IOException
    {
        try
        {
            System.setProperty(PDFBox.MAPPED_SIZE_THRESHOLD_PROPERTY, "10");
            Path tempFile = Files.createTempFile("SAMBox", null);
            Files.copy(getClass().getResourceAsStream("/input/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            assertNotNull(SeekableSources.seekableSourceFrom(tempFile.toFile()));
        }
        finally
        {
            System.getProperties().remove(PDFBox.MAPPED_SIZE_THRESHOLD_PROPERTY);
        }
    }

    @Test
    public void inMemorySeekableSourceFromBytes()
    {
        assertNotNull(SeekableSources.inMemorySeekableSourceFrom(new byte[] { -1 }));
    }

    @Test
    public void inMemorySeekableSourceFromStream() throws IOException
    {
        assertNotNull(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/allah2.pdf")));
    }

    @Test
    public void inputStreamFrom()
    {
        assertNotNull(SeekableSources.inputStreamFrom(SeekableSources
                .inMemorySeekableSourceFrom(new byte[] { -1 })));
    }

    @Test
    public void onTempFileSeekableSourceFrom() throws IOException
    {
        assertNotNull(SeekableSources.onTempFileSeekableSourceFrom(new ByteArrayInputStream(
                new byte[] { -1 })));
    }

}
