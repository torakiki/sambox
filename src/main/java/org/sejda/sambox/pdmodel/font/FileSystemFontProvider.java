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

import static java.util.Objects.nonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.fontbox.FontBoxFont;
import org.apache.fontbox.cff.CFFCIDFont;
import org.apache.fontbox.cff.CFFFont;
import org.apache.fontbox.ttf.NamingTable;
import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.OpenTypeFont;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.type1.Type1Font;
import org.apache.fontbox.util.autodetect.FontFileFinder;
import org.sejda.commons.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FontProvider which searches for fonts on the local filesystem.
 *
 * @author John Hewson
 */
public class FileSystemFontProvider extends FontProvider
{
    private static final FontCache FONT_CACHE = new FontCache(); // todo: static cache isn't ideal
    private static final String FONT_CACHE_SEPARATOR = "|";

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemFontProvider.class);

    private final List<FSFontInfo> fontInfoList = new ArrayList<>();

    private static class FSFontInfo extends FontInfo
    {
        private final String postScriptName;
        private final FontFormat format;
        private final CIDSystemInfo cidSystemInfo;
        private final int usWeightClass;
        private final int sFamilyClass;
        private final int ulCodePageRange1;
        private final int ulCodePageRange2;
        private final int macStyle;
        private final PDPanoseClassification panose;
        private final File file;

        private FSFontInfo(File file, FontFormat format, String postScriptName,
                CIDSystemInfo cidSystemInfo, int usWeightClass, int sFamilyClass,
                int ulCodePageRange1, int ulCodePageRange2, int macStyle, byte[] panose)
        {
            this.file = file;
            this.format = format;
            this.postScriptName = postScriptName;
            this.cidSystemInfo = cidSystemInfo;
            this.usWeightClass = usWeightClass;
            this.sFamilyClass = sFamilyClass;
            this.ulCodePageRange1 = ulCodePageRange1;
            this.ulCodePageRange2 = ulCodePageRange2;
            this.macStyle = macStyle;
            this.panose = panose != null
                    && panose.length >= PDPanoseClassification.LENGTH ? new PDPanoseClassification(
                    panose) : null;
        }

        @Override
        public String getPostScriptName()
        {
            return postScriptName;
        }

        @Override
        public FontFormat getFormat()
        {
            return format;
        }

        @Override
        public CIDSystemInfo getCIDSystemInfo()
        {
            return cidSystemInfo;
        }

        @Override
        public synchronized FontBoxFont getFont()
        {
            FontBoxFont cached = FONT_CACHE.getFont(this);
            if (cached != null)
            {
                return cached;
            }
            FontBoxFont font;
            LOG.debug("Loading {} from {}", postScriptName, file);
            switch (format)
            {
            case PFB:
                font = getType1Font(postScriptName, file);
                break;
            case TTF:
                font = getTrueTypeFont(postScriptName, file);
                break;
            case OTF:
                font = getOTFFont(postScriptName, file);
                break;
            default:
                throw new RuntimeException("can't happen");
            }
            if (font != null)
            {
                FONT_CACHE.addFont(this, font);
            }
            return font;
        }

        @Override
        public int getFamilyClass()
        {
            return sFamilyClass;
        }

        @Override
        public int getWeightClass()
        {
            return usWeightClass;
        }

        @Override
        public int getCodePageRange1()
        {
            return ulCodePageRange1;
        }

        @Override
        public int getCodePageRange2()
        {
            return ulCodePageRange2;
        }

        @Override
        public int getMacStyle()
        {
            return macStyle;
        }

        @Override
        public PDPanoseClassification getPanose()
        {
            return panose;
        }

        @Override
        public String toString()
        {
            return super.toString() + " " + file;
        }

        private TrueTypeFont getTrueTypeFont(String postScriptName, File file)
        {
            try
            {
                TrueTypeFont ttf = readTrueTypeFont(postScriptName, file);
                LOG.debug("Loaded {} from {}", postScriptName, file);
                return ttf;
            }
            catch (NullPointerException | IOException e) // TTF parser is buggy
            {
                LOG.warn("Could not load font file: " + file, e);
            }
            return null;
        }

        private TrueTypeFont readTrueTypeFont(String postScriptName, File file) throws IOException
        {
            if (file.getName().toLowerCase().endsWith(".ttc"))
            {
                @SuppressWarnings("squid:S2095")
                // ttc not closed here because it is needed later when ttf is accessed,
                // e.g. rendering PDF with non-embedded font which is in ttc file in our font directory
                TrueTypeCollection ttc = new TrueTypeCollection(file);
                TrueTypeFont ttf = null;
                try
                {
                    ttf = ttc.getFontByName(postScriptName);
                }
                catch (IOException ex)
                {
                    ttc.close();
                    throw ex;
                }
                if (ttf == null)
                {
                    ttc.close();
                    throw new IOException("Font " + postScriptName + " not found in " + file);
                }
                return ttf;
            }
            TTFParser ttfParser = new TTFParser(false, true);
            return ttfParser.parse(file);
        }

        private static OpenTypeFont getOTFFont(String postScriptName, File file)
        {
            try
            {
                if (file.getName().toLowerCase().endsWith(".ttc"))
                {
                    @SuppressWarnings("squid:S2095")
                    // ttc not closed here because it is needed later when ttf is accessed,
                    // e.g. rendering PDF with non-embedded font which is in ttc file in our font directory
                    TrueTypeCollection ttc = new TrueTypeCollection(file);
                    TrueTypeFont ttf = null;
                    try
                    {
                        ttf = ttc.getFontByName(postScriptName);
                    }
                    catch (IOException ex)
                    {
                        LOG.error(ex.getMessage(), ex);
                        ttc.close();
                        return null;
                    }
                    if (ttf == null)
                    {
                        ttc.close();
                        throw new IOException("Font " + postScriptName + " not found in " + file);
                    }
                    return (OpenTypeFont) ttf;
                }
                OTFParser parser = new OTFParser(false, true);
                OpenTypeFont otf = parser.parse(file);
                LOG.debug("Loaded {} from {}", postScriptName, file);
                return otf;
            }
            catch (IOException e)
            {
                LOG.warn("Could not load font file: " + file, e);
            }
            return null;
        }

        private static Type1Font getType1Font(String postScriptName, File file)
        {
            try (InputStream input = new FileInputStream(file))
            {
                Type1Font type1 = Type1Font.createWithPFB(input);
                LOG.debug("Loaded {} from {}", postScriptName, file);
                return type1;
            }
            catch (IOException e)
            {
                LOG.warn("Could not load font file " + file, e);
            }
            return null;
        }

    }

    /**
     * Represents ignored fonts (i.e. bitmap fonts).
     */
    private static final class FSIgnored extends FSFontInfo
    {
        private FSIgnored(File file, FontFormat format, String postScriptName)
        {
            super(file, format, postScriptName, null, 0, 0, 0, 0, 0, null);
        }

    }
    
    protected List<File> findFontFiles()
    {
        return new FontFileFinder().find().stream().map(File::new).collect(Collectors.toList());
    }


    private void initialize()
    {
        try
        {
            LOG.trace("Will search the local system for fonts");

            // scan the local system for font files
            List<File> files = new ArrayList<>();
            List<File> fonts = findFontFiles();
            files.addAll(fonts);

            LOG.trace("Found {} fonts on the local system", files.size());

            if (!files.isEmpty())
            {
                // load cached FontInfo objects
                List<FSFontInfo> cachedInfos = loadDiskCache(files);
                if (cachedInfos != null && !cachedInfos.isEmpty())
                {
                    fontInfoList.addAll(cachedInfos);
                }
                else
                {
                    LOG.warn("Building on-disk font cache, this may take a while");
                    scanFonts(files);
                    var executor = Executors.newSingleThreadExecutor();
                    executor.execute(this::saveDiskCache);
                    executor.shutdown();
                    LOG.info("Finished building on-disk font cache, found {} fonts",
                            fontInfoList.size());
                }
            }
        }
        catch (AccessControlException e)
        {
            LOG.error("Error accessing the file system", e);
        }
    }

    private void scanFonts(List<File> files)
    {
        for (File file : files)
        {
            try
            {
                String filePath = file.getPath().toLowerCase();
                if (filePath.endsWith(".ttf") || filePath.endsWith(".otf"))
                {
                    addTrueTypeFont(file);
                }
                else if (filePath.toLowerCase().endsWith(".ttc") || filePath.toLowerCase()
                        .endsWith(".otc"))
                {
                    addTrueTypeCollection(file);
                }
                else if (filePath.toLowerCase().endsWith(".pfb"))
                {
                    addType1Font(file);
                }
            }
            catch (Exception e) // TTF parser is buggy
            {
                LOG.warn("Unable to load font file: " + file, e);
            }
        }
    }

    protected File getDiskCacheFile()
    {
        String path = System.getProperty("org.sambox.fontcache");
        if (isBadPath(path))
        {
            path = System.getProperty("user.home");
            if (isBadPath(path))
            {
                path = System.getProperty("java.io.tmpdir");
            }
        }
        return new File(path, ".sambox.cache");
    }

    private static boolean isBadPath(String path)
    {
        return path == null || !new File(path).isDirectory() || !new File(path).canWrite();
    }

    /**
     * Saves the font metadata cache to disk.
     */
    private void saveDiskCache()
    {
        try
        {
            File file = getDiskCacheFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
            {
                for (FSFontInfo fontInfo : fontInfoList)
                {
                    writer.write(fontInfo.postScriptName.trim().replace("|", "\\|"));
                    writer.write(FONT_CACHE_SEPARATOR);
                    writer.write(fontInfo.format.toString());
                    writer.write(FONT_CACHE_SEPARATOR);
                    if (fontInfo.cidSystemInfo != null)
                    {
                        writer.write(fontInfo.cidSystemInfo.getRegistry() + '-'
                                + fontInfo.cidSystemInfo.getOrdering() + '-'
                                + fontInfo.cidSystemInfo.getSupplement());
                    }
                    writer.write(FONT_CACHE_SEPARATOR);
                    if (fontInfo.usWeightClass > -1)
                    {
                        writer.write(Integer.toHexString(fontInfo.usWeightClass));
                    }
                    writer.write(FONT_CACHE_SEPARATOR);
                    if (fontInfo.sFamilyClass > -1)
                    {
                        writer.write(Integer.toHexString(fontInfo.sFamilyClass));
                    }
                    writer.write(FONT_CACHE_SEPARATOR);
                    writer.write(Integer.toHexString(fontInfo.ulCodePageRange1));
                    writer.write(FONT_CACHE_SEPARATOR);
                    writer.write(Integer.toHexString(fontInfo.ulCodePageRange2));
                    writer.write(FONT_CACHE_SEPARATOR);
                    if (fontInfo.macStyle > -1)
                    {
                        writer.write(Integer.toHexString(fontInfo.macStyle));
                    }
                    writer.write(FONT_CACHE_SEPARATOR);
                    if (fontInfo.panose != null)
                    {
                        byte[] bytes = fontInfo.panose.getBytes();
                        for (int i = 0; i < 10; i++)
                        {
                            String str = Integer.toHexString(bytes[i]);
                            if (str.length() == 1)
                            {
                                writer.write('0');
                            }
                            writer.write(str);
                        }
                    }
                    writer.write(FONT_CACHE_SEPARATOR);
                    writer.write(fontInfo.file.getAbsolutePath());
                    writer.newLine();
                }
            }
        }
        catch (IOException | SecurityException e)
        {
            LOG.error("Could not write to font cache", e);
        }
    }

    /**
     * Loads the font metadata cache from disk.
     */
    private List<FSFontInfo> loadDiskCache(List<File> files)
    {
        Set<String> pending = new HashSet<>();
        for (File file : files)
        {
            pending.add(file.getAbsolutePath());
        }

        List<FSFontInfo> results = new ArrayList<>();
        File file = getDiskCacheFile();
        boolean fileExists = false;

        try
        {
            fileExists = file.exists();
        }
        catch (SecurityException e)
        {
        }

        if (fileExists)
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    String[] parts = line.split("(?<!\\\\)\\|", 10);
                    if (parts.length < 10)
                    {
                        LOG.warn("Incorrect line '" + line + "' in font disk cache is skipped");
                        continue;
                    }

                    String postScriptName;
                    FontFormat format;
                    CIDSystemInfo cidSystemInfo = null;
                    int usWeightClass = -1;
                    int sFamilyClass = -1;
                    int ulCodePageRange1;
                    int ulCodePageRange2;
                    int macStyle = -1;
                    byte[] panose = null;
                    File fontFile;

                    postScriptName = parts[0].replace("\\|", "|");
                    format = FontFormat.valueOf(parts[1]);
                    if (parts[2].length() > 0)
                    {
                        String[] ros = parts[2].split("-");
                        cidSystemInfo = new CIDSystemInfo(ros[0], ros[1], Integer.parseInt(ros[2]));
                    }
                    if (parts[3].length() > 0)
                    {
                        usWeightClass = (int) Long.parseLong(parts[3], 16);
                    }
                    if (parts[4].length() > 0)
                    {
                        sFamilyClass = (int) Long.parseLong(parts[4], 16);
                    }
                    ulCodePageRange1 = (int) Long.parseLong(parts[5], 16);
                    ulCodePageRange2 = (int) Long.parseLong(parts[6], 16);
                    if (parts[7].length() > 0)
                    {
                        macStyle = (int) Long.parseLong(parts[7], 16);
                    }
                    if (parts[8].length() > 0)
                    {
                        panose = new byte[10];
                        for (int i = 0; i < 10; i++)
                        {
                            String str = parts[8].substring(i * 2, i * 2 + 2);
                            int b = Integer.parseInt(str, 16);
                            panose[i] = (byte) (b & 0xff);
                        }
                    }
                    fontFile = new File(parts[9]);

                    if (fontFile.exists())
                    {

                        FSFontInfo info = new FSFontInfo(fontFile, format, postScriptName,
                                cidSystemInfo, usWeightClass, sFamilyClass, ulCodePageRange1,
                                ulCodePageRange2, macStyle, panose);
                        results.add(info);
                    }
                    else
                    {
                        LOG.debug("Font file {} not found, skipped", fontFile.getAbsolutePath());
                    }
                    pending.remove(fontFile.getAbsolutePath());
                }
            }
            catch (IOException e)
            {
                LOG.warn("Error loading font cache, will be re-built", e);
                return null;
            }
        }

        if (!pending.isEmpty())
        {
            // re-build the entire cache if we encounter un-cached fonts (could be optimised)
            LOG.warn("New fonts found, font cache will be re-built");
            return null;
        }

        return results;
    }

    /**
     * Adds a TTC or OTC to the file cache. To reduce memory, the parsed font is not cached.
     *
     * @throws IOException
     */
    private void addTrueTypeCollection(final File ttcFile) throws IOException
    {
        try (TrueTypeCollection ttc = new TrueTypeCollection(ttcFile))
        {
            ttc.processAllFonts(ttf -> addTrueTypeFontImpl(ttf, ttcFile));
        }
    }

    /**
     * Adds an OTF or TTF font to the file cache. To reduce memory, the parsed font is not cached.
     *
     * @throws IOException
     */
    private void addTrueTypeFont(File ttfFile) throws IOException
    {
        if (ttfFile.getPath().toLowerCase().endsWith(".otf"))
        {
            OTFParser parser = new OTFParser(false, true);
            OpenTypeFont otf = parser.parse(ttfFile);
            addTrueTypeFontImpl(otf, ttfFile);
        }
        else
        {
            TTFParser parser = new TTFParser(false, true);
            TrueTypeFont ttf = parser.parse(ttfFile);
            addTrueTypeFontImpl(ttf, ttfFile);
        }
    }

    /**
     * Adds an OTF or TTF font to the file cache. To reduce memory, the parsed font is not cached.
     */
    private void addTrueTypeFontImpl(TrueTypeFont ttf, File file) throws IOException
    {
        try
        {
            // read PostScript name, if any
            if (ttf.getName() != null)
            {
                // ignore bitmap fonts
                if (ttf.getHeader() == null)
                {
                    fontInfoList.add(new FSIgnored(file, FontFormat.TTF, ttf.getName()));
                    return;
                }
                int macStyle = ttf.getHeader().getMacStyle();

                int sFamilyClass = -1;
                int usWeightClass = -1;
                int ulCodePageRange1 = 0;
                int ulCodePageRange2 = 0;
                byte[] panose = null;

                OS2WindowsMetricsTable os2WindowsMetricsTable = ttf.getOS2Windows();

                // Apple's AAT fonts don't have an OS/2 table
                if (nonNull(os2WindowsMetricsTable))
                {
                    sFamilyClass = os2WindowsMetricsTable.getFamilyClass();
                    usWeightClass = os2WindowsMetricsTable.getWeightClass();
                    ulCodePageRange1 = (int) os2WindowsMetricsTable.getCodePageRange1();
                    ulCodePageRange2 = (int) os2WindowsMetricsTable.getCodePageRange2();
                    panose = os2WindowsMetricsTable.getPanose();
                }

                String format;
                if (ttf instanceof OpenTypeFont && ((OpenTypeFont) ttf).isPostScript())
                {
                    format = "OTF";
                    CFFFont cff = ((OpenTypeFont) ttf).getCFF().getFont();
                    CIDSystemInfo ros = null;
                    if (cff instanceof CFFCIDFont cidFont)
                    {
                        String registry = cidFont.getRegistry();
                        String ordering = cidFont.getOrdering();
                        int supplement = cidFont.getSupplement();
                        ros = new CIDSystemInfo(registry, ordering, supplement);
                    }
                    fontInfoList.add(
                            new FSFontInfo(file, FontFormat.OTF, ttf.getName(), ros, usWeightClass,
                                    sFamilyClass, ulCodePageRange1, ulCodePageRange2, macStyle,
                                    panose));
                }
                else
                {
                    CIDSystemInfo ros = null;
                    if (ttf.getTableMap().containsKey("gcid"))
                    {
                        // Apple's AAT fonts have a "gcid" table with CID info
                        byte[] bytes = ttf.getTableBytes(ttf.getTableMap().get("gcid"));
                        String reg = new String(bytes, 10, 64, StandardCharsets.US_ASCII);
                        String registryName = reg.substring(0, reg.indexOf('\0'));
                        String ord = new String(bytes, 76, 64, StandardCharsets.US_ASCII);
                        String orderName = ord.substring(0, ord.indexOf('\0'));
                        int supplementVersion = bytes[140] << 8 & (bytes[141] & 0xFF);
                        ros = new CIDSystemInfo(registryName, orderName, supplementVersion);
                    }

                    format = "TTF";
                    fontInfoList.add(
                            new FSFontInfo(file, FontFormat.TTF, ttf.getName(), ros, usWeightClass,
                                    sFamilyClass, ulCodePageRange1, ulCodePageRange2, macStyle,
                                    panose));
                }

                if (LOG.isTraceEnabled())
                {
                    NamingTable name = ttf.getNaming();
                    if (name != null)
                    {
                        LOG.trace(format + ": '" + name.getPostScriptName() + "' / '"
                                + name.getFontFamily() + "' / '" + name.getFontSubFamily() + "'");
                    }
                }
            }
            else
            {
                fontInfoList.add(new FSIgnored(file, FontFormat.TTF, "*skipnoname*"));
                LOG.warn("Missing 'name' entry for PostScript name in font " + file);
            }
        }
        catch (IOException e)
        {
            fontInfoList.add(new FSIgnored(file, FontFormat.TTF, "*skipexception*"));
            LOG.warn("Could not load font file: " + file, e);
        }
        finally
        {
            IOUtils.close(ttf);
        }
    }

    /**
     * Adds a Type 1 font to the file cache. To reduce memory, the parsed font is not cached.
     *
     * @throws IOException
     */
    private void addType1Font(File pfbFile) throws IOException
    {
        try (InputStream input = new FileInputStream(pfbFile))
        {
            Type1Font type1 = Type1Font.createWithPFB(input);
            fontInfoList.add(
                    new FSFontInfo(pfbFile, FontFormat.PFB, type1.getName(), null, -1, -1, 0, 0, -1,
                            null));

            if (LOG.isTraceEnabled())
            {
                LOG.trace("PFB: '" + type1.getName() + "' / '" + type1.getFamilyName() + "' / '"
                        + type1.getWeight() + "'");
            }
        }
    }

    @Override
    public List<? extends FontInfo> getFontInfo()
    {
        initialize();
        return fontInfoList;
    }
}
