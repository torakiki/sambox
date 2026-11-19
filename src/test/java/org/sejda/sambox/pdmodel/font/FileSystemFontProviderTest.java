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
package org.sejda.sambox.pdmodel.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * The test fonts are synthetic, two glyphs each: cff2.otf has CFF2 outlines (no 'CFF ' table),
 * cff2-and-ttf.ttc is a collection with the same CFF2 font followed by a TrueType one.
 */
public class FileSystemFontProviderTest
{
    private static final String CFF2_FONT = "SamboxTestCFF2-Regular";
    private static final String TTF_FONT = "SamboxTestTTF-Regular";

    @Test
    public void cff2FontIsIgnored() throws Exception
    {
        List<String> names = scan("cff2.otf");

        // still recorded, so that the file lands in the disk cache and is not scanned every time
        assertEquals(1, names.size());
        assertFalse(names.contains(CFF2_FONT));
    }

    @Test
    public void cff2FontDoesNotHideTheRestOfTheCollection() throws Exception
    {
        List<String> names = scan("cff2-and-ttf.ttc");

        assertEquals(2, names.size());
        assertFalse(names.contains(CFF2_FONT));
        assertTrue(names.contains(TTF_FONT));
    }

    private List<String> scan(String fontResource) throws IOException, URISyntaxException
    {
        File font = new File(getClass().getResource(fontResource).toURI());
        File cache = File.createTempFile("sambox", ".cache");
        cache.deleteOnExit();

        FileSystemFontProvider victim = new FileSystemFontProvider()
        {
            @Override
            protected List<File> findFontFiles()
            {
                return List.of(font);
            }

            @Override
            protected File getDiskCacheFile()
            {
                return cache;
            }
        };
        return victim.getFontInfo().stream().map(FontInfo::getPostScriptName)
                .collect(Collectors.toList());
    }
}
