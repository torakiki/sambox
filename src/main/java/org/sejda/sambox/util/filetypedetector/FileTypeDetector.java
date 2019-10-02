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
package org.sejda.sambox.util.filetypedetector;

import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Drew Noakes
 *
 * code taken from https://github.com/drewnoakes/metadata-extractor
 * Examines the a file's first bytes and estimates the file's type.
 */
public final class FileTypeDetector
{
    private static final ByteTrie<FileType> ROOT;
    private static final HashMap<String, FileType> FTYP_MAP;

    static
    {
        ROOT = new ByteTrie<>();
        ROOT.setDefaultValue(FileType.UNKNOWN);

        // https://en.wikipedia.org/wiki/List_of_file_signatures

        ROOT.addPath(FileType.JPEG, new byte[] { (byte) 0xff, (byte) 0xd8 });
        ROOT.addPath(FileType.TIFF, "II".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { 0x2a, 0x00 });
        ROOT.addPath(FileType.TIFF, "MM".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { 0x00, 0x2a });
        ROOT.addPath(FileType.PSD, "8BPS".getBytes(StandardCharsets.ISO_8859_1));
        ROOT.addPath(FileType.PNG, new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A,
                0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52 });
        // TODO technically there are other very rare magic numbers for OS/2 BMP files...
        ROOT.addPath(FileType.BMP, "BM".getBytes(StandardCharsets.ISO_8859_1));
        ROOT.addPath(FileType.GIF, "GIF87a".getBytes(StandardCharsets.ISO_8859_1));
        ROOT.addPath(FileType.GIF, "GIF89a".getBytes(StandardCharsets.ISO_8859_1));
        ROOT.addPath(FileType.ICO, new byte[] { 0x00, 0x00, 0x01, 0x00 });
        // multiple PCX versions, explicitly listed
        ROOT.addPath(FileType.PCX, new byte[] { 0x0A, 0x00, 0x01 });
        ROOT.addPath(FileType.PCX, new byte[] { 0x0A, 0x02, 0x01 });
        ROOT.addPath(FileType.PCX, new byte[] { 0x0A, 0x03, 0x01 });
        ROOT.addPath(FileType.PCX, new byte[] { 0x0A, 0x05, 0x01 });
        ROOT.addPath(FileType.RIFF, "RIFF".getBytes(StandardCharsets.ISO_8859_1));

        // https://github.com/drewnoakes/metadata-extractor/issues/217
        // root.addPath(FileType.ARW, "II".getBytes(Charsets.ISO_8859_1), new byte[]{0x2a, 0x00, 0x08, 0x00})
        ROOT.addPath(FileType.CRW, "II".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { 0x1a, 0x00, 0x00, 0x00 },
                "HEAPCCDR".getBytes(StandardCharsets.ISO_8859_1));
        ROOT.addPath(FileType.CR2, "II".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { 0x2a, 0x00, 0x10, 0x00, 0x00, 0x00, 0x43, 0x52 });
        ROOT.addPath(FileType.NEF, "MM".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { 0x00, 0x2a, 0x00, 0x00, 0x00, (byte) 0x80, 0x00 });
        ROOT.addPath(FileType.ORF, "IIRO".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { (byte) 0x08, 0x00 });
        ROOT.addPath(FileType.ORF, "IIRS".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { (byte) 0x08, 0x00 });
        ROOT.addPath(FileType.RAF, "FUJIFILMCCD-RAW".getBytes(StandardCharsets.ISO_8859_1));
        ROOT.addPath(FileType.RW2, "II".getBytes(StandardCharsets.ISO_8859_1),
                new byte[] { 0x55, 0x00 });

        FTYP_MAP = new HashMap<>();

        // http://www.ftyps.com
        // HEIF
        FTYP_MAP.put("ftypmif1", FileType.HEIF);
        FTYP_MAP.put("ftypmsf1", FileType.HEIF);
        FTYP_MAP.put("ftypheic", FileType.HEIF);
        FTYP_MAP.put("ftypheix", FileType.HEIF);
        FTYP_MAP.put("ftyphevc", FileType.HEIF);
        FTYP_MAP.put("ftyphevx", FileType.HEIF);
    }

    private FileTypeDetector()
    {
    }

    /**
     * Examines the a file's first bytes and estimates the file's type.
     * 
     * @param file to examine.
     * @return the file type or null if it wasn't possible to determine by reading the first bytes
     * @throws IOException if an IO error occurs.
     */
    public static FileType detectFileType(File file) throws IOException
    {
        return detectFileType(SeekableSources.seekableSourceFrom(file));
    }

    public static FileType detectFileType(SeekableSource source) throws IOException
    {
        byte[] firstBytes = new byte[ROOT.getMaxDepth()];

        try (InputStream fin = source.asNewInputStream())
        {
            fin.read(firstBytes);
        }
        FileType fileType = ROOT.find(firstBytes);

        if (fileType == FileType.UNKNOWN)
        {
            String eightCC = new String(firstBytes, 4, 8);
            // Test at offset 4 for Base Media Format (i.e. QuickTime, MP4, etc...) identifier "ftyp" plus four identifying characters
            FileType t = FTYP_MAP.get(eightCC);
            if (t != null)
            {
                return t;
            }
        }
        else if (fileType == FileType.RIFF)
        {
            String fourCC = new String(firstBytes, 8, 4);
            if (fourCC.equals("WEBP"))
            {
                return FileType.WEBP;
            }
        }

        return fileType;
    }
}
