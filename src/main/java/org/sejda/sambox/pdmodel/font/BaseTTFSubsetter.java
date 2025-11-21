package org.sejda.sambox.pdmodel.font;
/*
 * Copyright 2022 Sober Lemur S.r.l.
 * Copyright 2022 Sejda BV
 *
 * Created 10/08/22
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static java.util.Objects.nonNull;
import static org.sejda.sambox.pdmodel.font.FontUtils.isSubsettingPermitted;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.fontbox.ttf.CmapTable;
import org.apache.fontbox.ttf.GlyphTable;
import org.apache.fontbox.ttf.HeaderTable;
import org.apache.fontbox.ttf.HorizontalHeaderTable;
import org.apache.fontbox.ttf.HorizontalMetricsTable;
import org.apache.fontbox.ttf.MaximumProfileTable;
import org.apache.fontbox.ttf.NameRecord;
import org.apache.fontbox.ttf.NamingTable;
import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.PostScriptTable;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.commons.util.RequireUtils;
import org.sejda.io.SeekableSource;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeletal implementation for a TTF subsetter. To be used to subset embedded TrueType font and Type
 * 0 fonts with CIDFontType2 descendant This is based on the
 * {@link org.apache.fontbox.ttf.TTFSubsetter} in FontBox.
 *
 * @author Andrea Vacondio
 */
public abstract class BaseTTFSubsetter implements Function<String, COSStream>
{

    private static final Logger LOG = LoggerFactory.getLogger(BaseTTFSubsetter.class);

    private static final byte[] PAD_BUF = new byte[] { 0, 0, 0 };

    private final SeekableSource fontSource;
    private final SortedSet<Integer> glyphIds = new TreeSet<>();
    private final SortedMap<Integer, Integer> codeToGIDLookup = new TreeMap<>();
    private final COSStream fontFileStream;
    private final COSDictionary existingFont;
    private TrueTypeFont font;
    private int numberOfGlyphs;

    /**
     * @param existingFont   the font dictionary that will be updated
     * @param fontFileStream the corresponding not subsetted FontFile2
     * @param font           the existing and already parsed font. If null the fontFileStream will
     *                       be parsed to get the ttf font when needed
     */
    public BaseTTFSubsetter(COSDictionary existingFont, COSStream fontFileStream, TrueTypeFont font)
            throws IOException
    {
        this.existingFont = Objects.requireNonNull(existingFont);
        this.fontFileStream = Objects.requireNonNull(fontFileStream);
        this.fontSource = fontFileStream.getUnfilteredSource();
        this.font = font;
        this.glyphIds.add(0);
    }

    public BaseTTFSubsetter(COSDictionary existingFont, COSStream fontFileStream) throws IOException
    {
        this(existingFont, fontFileStream, null);
    }

    /**
     * Adds all the given mappings code -> GID to this subsetter
     *
     * @param codesToGID
     */
    public void putAll(Map<Integer, Integer> codesToGID)
    {
        this.codeToGIDLookup.putAll(codesToGID);
    }

    /**
     * Creates a subset of the font file containing only the glyphs added to this subsetter. If
     * anything happens null is returned
     *
     * @param subsetFontName the new subset font name
     * @return the subsetted font stream or null if anything went wrong
     */
    public COSStream apply(String subsetFontName)
    {
        String existingFontName = existingFont.getNameAsString(COSName.BASE_FONT);
        LOG.debug("Subsetting {}", existingFontName);
        //TTFParser#parse already closes the InputStream so no need to close it
        try (TrueTypeFont font = getFont())
        {
            if (isSubsettingPermitted(font))
            {
                RequireUtils.requireState(!codeToGIDLookup.isEmpty(),
                        "Unable to subset, no glyph was specified");
                this.glyphIds.addAll(codeToGIDLookup.values());
                addCompositeGlyphs();
                return doSubset(subsetFontName);
            }
        }
        catch (Exception e)
        {
            LOG.error("Unable to subset font " + existingFontName, e);
        }
        return null;
    }

    public TrueTypeFont getFont() throws IOException
    {
        if (Objects.isNull(font))
        {
            font = new TTFParser(true, true).parse(fontFileStream.getUnfilteredStream());
        }
        return font;
    }

    public SortedSet<Integer> getGlyphIds()
    {
        return Collections.unmodifiableSortedSet(glyphIds);
    }

    public abstract COSStream doSubset(String subsetFontName) throws IOException;

    /**
     * Initialize the subsetter before performing a subset
     */
    public void setNumberOfGlyphs(int numberOfGlyphs)
    {
        this.numberOfGlyphs = numberOfGlyphs;
    }

    protected void updateChecksum(DataOutputStream out, Map<String, byte[]> tables)
            throws IOException
    {
        long checksum = writeFileHeader(out, tables.size());
        long offset = 12L + 16L * tables.size();
        for (Map.Entry<String, byte[]> entry : tables.entrySet())
        {
            checksum += writeTableHeader(out, entry.getKey(), offset, entry.getValue());
            offset += (entry.getValue().length + 3L) / 4 * 4;
        }
        checksum = 0xB1B0AFBAL - (checksum & 0xffffffffL);
        byte[] head = tables.get("head");
        // update checksumAdjustment in 'head' table
        head[8] = (byte) (checksum >>> 24);
        head[9] = (byte) (checksum >>> 16);
        head[10] = (byte) (checksum >>> 8);
        head[11] = (byte) checksum;
    }

    private void addCompositeGlyphs() throws IOException
    {
        GlyphTable glyphTable = getFont().getGlyph();
        long[] offsets = getFont().getIndexToLocation().getOffsets();
        Set<Integer> toProcess = new HashSet<>(glyphIds);
        while (!toProcess.isEmpty())
        {
            Set<Integer> composite = new HashSet<>();
            //there is no seek
            for (Integer glyphId : toProcess)
            {
                if (glyphId < offsets.length)
                {
                    long offset = offsets[glyphId];
                    long length = offsets[glyphId + 1] - offset;
                    if (length >= 0)
                    {
                        fontSource.position(offset + glyphTable.getOffset());
                        ByteBuffer glyphData = ByteBuffer.allocate((int) length);
                        fontSource.read(glyphData);
                        glyphData.flip();
                        if (isComposite(glyphData))
                        {
                            int off = 2 * 5;
                            int flags;
                            do
                            {
                                flags = (glyphData.get(off) & 0xff) << 8
                                        | glyphData.get(off + 1) & 0xff;
                                off += 2;
                                int ogid = (glyphData.get(off) & 0xff) << 8
                                        | glyphData.get(off + 1) & 0xff;
                                composite.add(ogid);
                                off += 2;
                                // ARG_1_AND_2_ARE_WORDS
                                if ((flags & 1) != 0)
                                {
                                    off += 2 * 2;
                                }
                                else
                                {
                                    off += 2;
                                }
                                // WE_HAVE_A_TWO_BY_TWO
                                if ((flags & 1 << 7) != 0)
                                {
                                    off += 2 * 4;
                                }
                                // WE_HAVE_AN_X_AND_Y_SCALE
                                else if ((flags & 1 << 6) != 0)
                                {
                                    off += 2 * 2;
                                }
                                // WE_HAVE_A_SCALE
                                else if ((flags & 1 << 3) != 0)
                                {
                                    off += 2;
                                }
                            } while ((flags & 1 << 5) != 0); // MORE_COMPONENTS

                        }
                    }
                }
                else
                {
                    LOG.warn("Ignored composite glyph element {}", glyphId);
                }
            }
            glyphIds.addAll(composite);
            toProcess.clear();
            toProcess.addAll(composite);
        }
    }

    private boolean isComposite(ByteBuffer glyphData)
    {
        // negative numberOfContours means a composite glyph
        return glyphData.limit() >= 2 && glyphData.getShort() == -1;
    }

    protected byte[] buildHeadTable() throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream(54))
        {
            try (DataOutputStream out = new DataOutputStream(bos))
            {
                HeaderTable h = getFont().getHeader();
                writeFixed(out, h.getVersion());
                writeFixed(out, h.getFontRevision());
                writeUint32(out, 0); // h.getCheckSumAdjustment()
                writeUint32(out, h.getMagicNumber());
                writeUint16(out, h.getFlags());
                writeUint16(out, h.getUnitsPerEm());
                writeLongDateTime(out, h.getCreated());
                writeLongDateTime(out, h.getModified());
                writeSInt16(out, h.getXMin());
                writeSInt16(out, h.getYMin());
                writeSInt16(out, h.getXMax());
                writeSInt16(out, h.getYMax());
                writeUint16(out, h.getMacStyle());
                writeUint16(out, h.getLowestRecPPEM());
                writeSInt16(out, h.getFontDirectionHint());
                // force long format of 'loca' table
                writeSInt16(out, (short) 1); // h.getIndexToLocFormat()
                writeSInt16(out, h.getGlyphDataFormat());
            }
            return bos.toByteArray();
        }
    }

    protected byte[] buildHheaTable() throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            try (DataOutputStream out = new DataOutputStream(bos))
            {

                HorizontalHeaderTable h = getFont().getHorizontalHeader();
                writeFixed(out, h.getVersion());
                writeSInt16(out, h.getAscender());
                writeSInt16(out, h.getDescender());
                writeSInt16(out, h.getLineGap());
                writeUint16(out, h.getAdvanceWidthMax());
                //TODO verify if these need recalculation, what other PDF tools do?
                writeSInt16(out, h.getMinLeftSideBearing());
                writeSInt16(out, h.getMinRightSideBearing());
                writeSInt16(out, h.getXMaxExtent());
                writeSInt16(out, h.getCaretSlopeRise());
                writeSInt16(out, h.getCaretSlopeRun());
                writeSInt16(out, h.getReserved1()); // caretOffset
                writeSInt16(out, h.getReserved2());
                writeSInt16(out, h.getReserved3());
                writeSInt16(out, h.getReserved4());
                writeSInt16(out, h.getReserved5());
                writeSInt16(out, h.getMetricDataFormat());
                writeUint16(out, Math.min(h.getNumberOfHMetrics(), numberOfGlyphs + 1));
            }
            return bos.toByteArray();
        }
    }

    protected byte[] buildMaxpTable() throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            try (DataOutputStream out = new DataOutputStream(bos))
            {

                MaximumProfileTable p = getFont().getMaximumProfile();
                writeFixed(out, p.getVersion());
                writeUint16(out, numberOfGlyphs + 1);
                if (p.getVersion() >= 1.0f)
                {
                    writeUint16(out, p.getMaxPoints());
                    writeUint16(out, p.getMaxContours());
                    writeUint16(out, p.getMaxCompositePoints());
                    writeUint16(out, p.getMaxCompositeContours());
                    writeUint16(out, p.getMaxZones());
                    writeUint16(out, p.getMaxTwilightPoints());
                    writeUint16(out, p.getMaxStorage());
                    writeUint16(out, p.getMaxFunctionDefs());
                    writeUint16(out, p.getMaxInstructionDefs());
                    writeUint16(out, p.getMaxStackElements());
                    writeUint16(out, p.getMaxSizeOfInstructions());
                    writeUint16(out, p.getMaxComponentElements());
                    writeUint16(out, p.getMaxComponentDepth());
                }
            }
            return bos.toByteArray();
        }
    }

    protected byte[] buildNameTable(String subsetFontName) throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            try (DataOutputStream out = new DataOutputStream(bos))
            {
                NamingTable name = getFont().getNaming();
                if (nonNull(name))
                {
                    Map<Integer, byte[]> records = name.getNameRecords().stream()
                            .filter(this::shouldCopyNameRecord).collect(
                                    Collectors.toMap(NameRecord::getNameId,
                                            (record) -> record.getString()
                                                    .getBytes(StandardCharsets.UTF_16BE)));
                    records.put(0, "This is a subeset for internal PDF use".getBytes(
                            StandardCharsets.UTF_16BE));
                    records.put(1, subsetFontName.getBytes(StandardCharsets.UTF_16BE));
                    records.remove(3);
                    records.put(4, subsetFontName.getBytes(StandardCharsets.UTF_16BE));
                    records.put(5, "Version 1.0".getBytes(StandardCharsets.UTF_16BE));
                    records.put(6, subsetFontName.getBytes(StandardCharsets.UTF_16BE));

                    writeUint16(out, 0);
                    writeUint16(out, records.size());
                    writeUint16(out, 2 * 3 + 2 * 6 * records.size());
                    int offset = 0;
                    for (Map.Entry<Integer, byte[]> nameRecord : records.entrySet())
                    {
                        writeUint16(out, NameRecord.PLATFORM_WINDOWS);
                        writeUint16(out, NameRecord.ENCODING_WINDOWS_UNICODE_BMP);
                        writeUint16(out, NameRecord.LANGUAGE_WINDOWS_EN_US);
                        writeUint16(out, nameRecord.getKey());
                        writeUint16(out, nameRecord.getValue().length);
                        writeUint16(out, offset);
                        offset += nameRecord.getValue().length;
                    }

                    for (byte[] bytes : records.values())
                    {
                        out.write(bytes);
                    }
                }
            }
            return bos.toByteArray();
        }
    }

    boolean shouldCopyNameRecord(NameRecord nr)
    {
        return nr.getPlatformId() == NameRecord.PLATFORM_WINDOWS
                && nr.getPlatformEncodingId() == NameRecord.ENCODING_WINDOWS_UNICODE_BMP
                && nr.getLanguageId() == NameRecord.LANGUAGE_WINDOWS_EN_US && nr.getNameId() >= 0
                && nr.getNameId() < 7;
    }

    protected byte[] buildGlyfTable(long[] newOffsets) throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            GlyphTable glyphTable = getFont().getGlyph();
            long[] offsets = getFont().getIndexToLocation().getOffsets();
            long newOffset = 0;  // new offset for the glyph in the subset font
            // for each glyph in the subset + 1 last additional entry:
            // "In the particular case of the last glyph(s), loca[n] will be equal the length of the glyph data ('glyf') table"
            for (int glyphId = 0; glyphId <= numberOfGlyphs + 1; glyphId++)
            {
                newOffsets[glyphId] = newOffset;
                if (glyphIds.contains(glyphId))
                {
                    if (glyphId < offsets.length)
                    {
                        long offset = offsets[glyphId];
                        long length = offsets[glyphId + 1] - offset;
                        if (length >= 0)
                        {
                            fontSource.position(offset + glyphTable.getOffset());
                            ByteBuffer glyphData = ByteBuffer.allocate((int) length);
                            fontSource.read(glyphData);
                            glyphData.flip();
                            bos.write(glyphData.array());
                            // offset to start next glyph
                            newOffset += glyphData.limit();
                        }
                        else
                        {
                            LOG.warn("Ignored glyph {}, cannot find a valid offset", glyphId);
                        }
                    }
                }
            }
            return bos.toByteArray();
        }
    }

    protected byte[] buildLocaTable(long[] newOffsets) throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            try (DataOutputStream out = new DataOutputStream(bos))
            {
                for (long offset : newOffsets)
                {
                    writeUint32(out, offset);
                }
            }
            return bos.toByteArray();
        }
    }

    //TODO this is straight from fontbox, it needs to be reviewed
    protected byte[] buildHmtxTable() throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {

            HorizontalHeaderTable h = getFont().getHorizontalHeader();
            HorizontalMetricsTable hm = getFont().getHorizontalMetrics();

            // more info: https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6hmtx.html
            int lastgid = h.getNumberOfHMetrics() - 1;
            fontSource.position(hm.getOffset());
            for (int glyphId = 0; glyphId <= numberOfGlyphs; glyphId++)
            {
                // offset in original file
                long offset;
                if (glyphId <= lastgid)
                {
                    // copy width and lsb
                    offset = glyphId * 4L;
                    copyBytes(bos, offset + hm.getOffset(), 4);
                }
                else
                {
                    // copy lsb only, as we are beyond numOfHMetrics
                    offset =
                            h.getNumberOfHMetrics() * 4L + (glyphId - h.getNumberOfHMetrics()) * 2L;
                    copyBytes(bos, offset + hm.getOffset(), 2);
                }
            }
            return bos.toByteArray();
        }
    }

    protected byte[] buildPostTable() throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            PostScriptTable post = getFont().getPostScript();
            if (nonNull(post))
            {
                try (DataOutputStream out = new DataOutputStream(bos))
                {
                    writeFixed(out, 3.0);
                    writeFixed(out, post.getItalicAngle());
                    writeSInt16(out, post.getUnderlinePosition());
                    writeSInt16(out, post.getUnderlineThickness());
                    writeUint32(out, post.getIsFixedPitch());
                    writeUint32(out, post.getMinMemType42());
                    writeUint32(out, post.getMaxMemType42());
                    writeUint32(out, post.getMinMemType1());
                    writeUint32(out, post.getMaxMemType1());
                }
            }
            return bos.toByteArray();
        }
    }

    protected byte[] buildOS2Table() throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            try (DataOutputStream out = new DataOutputStream(bos))
            {
                OS2WindowsMetricsTable os2 = getFont().getOS2Windows();
                if (nonNull(os2))
                {

                    writeUint16(out, os2.getVersion());
                    writeSInt16(out, os2.getAverageCharWidth());
                    writeUint16(out, os2.getWeightClass());
                    writeUint16(out, os2.getWidthClass());

                    writeSInt16(out, os2.getFsType());

                    writeSInt16(out, os2.getSubscriptXSize());
                    writeSInt16(out, os2.getSubscriptYSize());
                    writeSInt16(out, os2.getSubscriptXOffset());
                    writeSInt16(out, os2.getSubscriptYOffset());

                    writeSInt16(out, os2.getSuperscriptXSize());
                    writeSInt16(out, os2.getSuperscriptYSize());
                    writeSInt16(out, os2.getSuperscriptXOffset());
                    writeSInt16(out, os2.getSuperscriptYOffset());

                    writeSInt16(out, os2.getStrikeoutSize());
                    writeSInt16(out, os2.getStrikeoutPosition());
                    writeSInt16(out, (short) os2.getFamilyClass());
                    out.write(os2.getPanose());

                    writeUint32(out, 0);
                    writeUint32(out, 0);
                    writeUint32(out, 0);
                    writeUint32(out, 0);

                    out.write(os2.getAchVendId().getBytes(StandardCharsets.US_ASCII));

                    writeUint16(out, os2.getFsSelection());
                    writeUint16(out, codeToGIDLookup.firstKey());
                    writeUint16(out, codeToGIDLookup.lastKey());
                    writeUint16(out, os2.getTypoAscender());
                    writeUint16(out, os2.getTypoDescender());
                    writeUint16(out, os2.getTypoLineGap());
                    writeUint16(out, os2.getWinAscent());
                    writeUint16(out, os2.getWinDescent());
                }
            }
            return bos.toByteArray();
        }
    }

    protected byte[] buildCMapTable() throws IOException
    {
        try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream())
        {
            try (DataOutputStream out = new DataOutputStream(bos))
            {

                // cmap header
                writeUint16(out, 0); // version
                writeUint16(out, 1); // numberSubtables

                // encoding record
                writeUint16(out, CmapTable.PLATFORM_WINDOWS); // platformID
                writeUint16(out, CmapTable.ENCODING_WIN_UNICODE_BMP); // platformSpecificID
                writeUint32(out, 12); // offset 4 * 2 + 4

                // build Format 4 subtable (Unicode BMP)
                Iterator<Map.Entry<Integer, Integer>> it = codeToGIDLookup.entrySet().iterator();
                Map.Entry<Integer, Integer> lastChar = it.next();
                Map.Entry<Integer, Integer> prevChar = lastChar;
                int lastGid = lastChar.getValue();

                // +1 because .notdef is missing in uniToGID
                int[] startCode = new int[codeToGIDLookup.size() + 1];
                int[] endCode = new int[startCode.length];
                int[] idDelta = new int[startCode.length];
                int segCount = 0;
                while (it.hasNext())
                {
                    Map.Entry<Integer, Integer> curChar2Gid = it.next();
                    int curGid = curChar2Gid.getValue();

                    // todo: need format Format 12 for non-BMP
                    if (curChar2Gid.getKey() > 0xFFFF)
                    {
                        throw new UnsupportedOperationException("non-BMP Unicode character");
                    }

                    if (curChar2Gid.getKey() != prevChar.getKey() + 1
                            || curGid - lastGid != curChar2Gid.getKey() - lastChar.getKey())
                    {
                        if (lastGid != 0)
                        {
                            // don't emit ranges, which map to GID 0, the
                            // undef glyph is emitted a the very last segment
                            startCode[segCount] = lastChar.getKey();
                            endCode[segCount] = prevChar.getKey();
                            idDelta[segCount] = lastGid - lastChar.getKey();
                            segCount++;
                        }
                        else if (!lastChar.getKey().equals(prevChar.getKey()))
                        {
                            // shorten ranges which start with GID 0 by one
                            startCode[segCount] = lastChar.getKey() + 1;
                            endCode[segCount] = prevChar.getKey();
                            idDelta[segCount] = lastGid - lastChar.getKey();
                            segCount++;
                        }
                        lastGid = curGid;
                        lastChar = curChar2Gid;
                    }
                    prevChar = curChar2Gid;
                }

                // trailing segment
                startCode[segCount] = lastChar.getKey();
                endCode[segCount] = prevChar.getKey();
                idDelta[segCount] = lastGid - lastChar.getKey();
                segCount++;

                // GID 0
                startCode[segCount] = 0xffff;
                endCode[segCount] = 0xffff;
                idDelta[segCount] = 1;
                segCount++;

                // write format 4 subtable
                int searchRange = 2 * (int) Math.pow(2, log2(segCount));
                writeUint16(out, 4); // format
                writeUint16(out, 8 * 2 + segCount * 4 * 2); // length
                writeUint16(out, 0); // language
                writeUint16(out, segCount * 2); // segCountX2
                writeUint16(out, searchRange); // searchRange
                writeUint16(out, log2(searchRange / 2)); // entrySelector
                writeUint16(out, 2 * segCount - searchRange); // rangeShift

                // endCode[segCount]
                for (int i = 0; i < segCount; i++)
                {
                    writeUint16(out, endCode[i]);
                }

                // reservedPad
                writeUint16(out, 0);

                // startCode[segCount]
                for (int i = 0; i < segCount; i++)
                {
                    writeUint16(out, startCode[i]);
                }

                // idDelta[segCount]
                for (int i = 0; i < segCount; i++)
                {
                    writeUint16(out, idDelta[i]);
                }

                for (int i = 0; i < segCount; i++)
                {
                    writeUint16(out, 0);
                }

            }
            return bos.toByteArray();
        }
    }

    private void copyBytes(OutputStream os, long offset, int count) throws IOException
    {
        ByteBuffer data = ByteBuffer.allocate(count);
        fontSource.position(offset);
        fontSource.read(data);
        data.flip();
        os.write(data.array());
    }

    private void writeFixed(DataOutputStream out, double f) throws IOException
    {
        double ip = Math.floor(f);
        double fp = (f - ip) * 65536.0;
        out.writeShort((int) ip);
        out.writeShort((int) fp);
    }

    private void writeUint32(DataOutputStream out, long l) throws IOException
    {
        out.writeInt((int) l);
    }

    private void writeUint16(DataOutputStream out, int i) throws IOException
    {
        out.writeShort(i);
    }

    private void writeSInt16(DataOutputStream out, short i) throws IOException
    {
        out.writeShort(i);
    }

    private void writeLongDateTime(DataOutputStream out, Calendar calendar) throws IOException
    {
        // inverse operation of TTFDataStream.readInternationalDate()
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(1904, 0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long millisFor1904 = cal.getTimeInMillis();
        long secondsSince1904 = (calendar.getTimeInMillis() - millisFor1904) / 1000L;
        out.writeLong(secondsSince1904);
    }

    /**
     * @param out     The data output stream.
     * @param nTables The number of table.
     * @return The file offset of the first TTF table to write.
     * @throws IOException Upon errors.
     */
    private long writeFileHeader(DataOutputStream out, int nTables) throws IOException
    {
        out.writeInt(0x00010000);
        out.writeShort(nTables);

        int mask = Integer.highestOneBit(nTables);
        int searchRange = mask * 16;
        out.writeShort(searchRange);

        int entrySelector = log2(mask);

        out.writeShort(entrySelector);

        // numTables * 16 - searchRange
        int last = 16 * nTables - searchRange;
        out.writeShort(last);

        return 0x00010000L + toUInt32(nTables, searchRange) + toUInt32(entrySelector, last);
    }

    private long writeTableHeader(DataOutputStream out, String tag, long offset, byte[] bytes)
            throws IOException
    {
        long checksum = 0;
        for (int nup = 0, n = bytes.length; nup < n; nup++)
        {
            checksum += (bytes[nup] & 0xffL) << 24 - nup % 4 * 8;
        }
        checksum &= 0xffffffffL;

        byte[] tagbytes = tag.getBytes(StandardCharsets.US_ASCII);

        out.write(tagbytes, 0, 4);
        out.writeInt((int) checksum);
        out.writeInt((int) offset);
        out.writeInt(bytes.length);

        // account for the checksum twice, once for the header field, once for the content itself
        return toUInt32(tagbytes) + checksum + checksum + offset + bytes.length;
    }

    public void writeTableBody(OutputStream os, byte[] bytes) throws IOException
    {
        int n = bytes.length;
        os.write(bytes);
        if (n % 4 != 0)
        {
            os.write(PAD_BUF, 0, 4 - n % 4);
        }
    }

    private long toUInt32(int high, int low)
    {
        return (high & 0xffffL) << 16 | low & 0xffffL;
    }

    private long toUInt32(byte[] bytes)
    {
        return (bytes[0] & 0xffL) << 24 | (bytes[1] & 0xffL) << 16 | (bytes[2] & 0xffL) << 8
                | bytes[3] & 0xffL;
    }

    private int log2(int num)
    {
        return (int) Math.floor(Math.log(num) / Math.log(2));
    }
}
