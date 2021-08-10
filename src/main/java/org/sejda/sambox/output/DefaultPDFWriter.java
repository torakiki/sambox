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
package org.sejda.sambox.output;

import static java.util.Objects.nonNull;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.sambox.util.SpecVersionUtils.PDF_HEADER;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.DirectCOSObject;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.xref.XrefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default PDF writer that writes part of the pdf document using the given {@link IndirectObjectsWriter}.
 * 
 * @author Andrea Vacondio
 */
class DefaultPDFWriter implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPDFWriter.class);

    byte COMMENT = '%';
    byte[] GARBAGE = new byte[] { (byte) 0xA7, (byte) 0xE3, (byte) 0xF1, (byte) 0xF1 };
    private IndirectObjectsWriter writer;

    public DefaultPDFWriter(IndirectObjectsWriter writer)
    {
        requireNotNullArg(writer, "Cannot write to a null COSWriter");
        this.writer = writer;
    }

    public void writeHeader(String version) throws IOException
    {
        LOG.debug("Writing header " + version);
        writer().write(PDF_HEADER);
        writer().write(version);
        writer().writeEOL();
        writer().write(COMMENT);
        writer().write(GARBAGE);
        writer().writeEOL();
    }

    /**
     * writes the xref table
     * 
     * @return the startxref value
     * @throws IOException
     */
    public long writeXrefTable() throws IOException
    {
        long startxref = writer().offset();
        LOG.debug("Writing xref table at offset " + startxref);
        if (nonNull(writer.context().addWritten(XrefEntry.DEFAULT_FREE_ENTRY)))
        {
            LOG.warn("Reserved object number 0 has been overwritten with the expected free entry");
        }
        writer().write("xref");
        writer().writeEOL();
        for (List<Long> continuos : writer.context().getWrittenContiguousGroups())
        {
            writer().write(continuos.get(0).toString() + " " + continuos.size());
            writer().writeEOL();
            for (long key : continuos)
            {
                writer().write(Optional.ofNullable(writer.context().getWritten(key))
                        .orElse(XrefEntry.DEFAULT_FREE_ENTRY).toXrefTableEntry());
            }
        }
        return startxref;
    }

    /**
     * Writes the given trailer setting startxref to the given offsets
     * 
     * @param trailer
     * @param startxref
     * @throws IOException
     */
    public void writeTrailer(COSDictionary trailer, long startxref) throws IOException
    {
        writeTrailer(trailer, startxref, -1);
    }

    /**
     * Writes the given trailer setting the /Prev entry and startxref to the given offsets
     * 
     * @param trailer
     * @param startxref
     * @param prev prev offset, written only if != -1
     * @throws IOException
     */
    public void writeTrailer(COSDictionary trailer, long startxref, long prev) throws IOException
    {
        LOG.trace("Writing trailer");
        sanitizeTrailer(trailer, prev);
        trailer.setItem(COSName.SIZE, DirectCOSObject
                .asDirectObject(COSInteger.get(writer.context().highestObjectNumber() + 1)));
        writer.write("trailer".getBytes(StandardCharsets.US_ASCII));
        writer.writeEOL();
        trailer.getCOSObject().accept(writer.writer());
        writeXrefFooter(startxref);
    }

    /**
     * Writes an xref stream using the given trailer
     * 
     * @param trailer
     * @throws IOException
     */
    public void writeXrefStream(COSDictionary trailer) throws IOException
    {
        if (nonNull(writer.context().addWritten(XrefEntry.DEFAULT_FREE_ENTRY)))
        {
            LOG.warn("Reserved object number 0 has been overwritten with the expected free entry");
        }
        writeXrefStream(trailer, -1);
    }

    /**
     * Writes an xref stream using the given trailer and setting the /Prev entry to the given offset
     * 
     * @param trailer
     * @param prev
     * @throws IOException
     */
    public void writeXrefStream(COSDictionary trailer, long prev) throws IOException
    {
        long startxref = writer().offset();
        LOG.debug("Writing xref stream at offset " + startxref);
        sanitizeTrailer(trailer, prev);
        XrefEntry entry = XrefEntry.inUseEntry(writer.context().highestObjectNumber() + 1,
                startxref, 0);
        writer.context().addWritten(entry);
        writer.writeObject(new IndirectCOSObjectReference(entry.getObjectNumber(),
                entry.getGenerationNumber(), new XrefStream(trailer, writer.context())));
        writeXrefFooter(startxref);
    }

    private static void sanitizeTrailer(COSDictionary trailer, long prev)
    {
        trailer.removeItem(COSName.PREV);
        trailer.removeItem(COSName.XREF_STM);
        trailer.removeItem(COSName.DOC_CHECKSUM);
        trailer.removeItem(COSName.DECODE_PARMS);
        trailer.removeItem(COSName.FILTER);
        trailer.removeItem(COSName.F_DECODE_PARMS);
        trailer.removeItem(COSName.F_FILTER);
        trailer.removeItem(COSName.F);
        trailer.removeItem(COSName.LENGTH);
        trailer.removeItem(COSName.W);
        trailer.removeItem(COSName.DL);
        trailer.removeItem(COSName.TYPE);
        trailer.removeItem(COSName.INDEX);
        if (prev != -1)
        {
            trailer.setLong(COSName.PREV, prev);
        }
    }

    private void writeXrefFooter(long startxref) throws IOException
    {
        writer.write("startxref".getBytes(StandardCharsets.US_ASCII));
        writer.writeEOL();
        writer.write(Long.toString(startxref));
        writer.writeEOL();
        writer.write("%%EOF".getBytes(StandardCharsets.US_ASCII));
        writer.writeEOL();
    }

    IndirectObjectsWriter writer()
    {
        return writer;
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
    }
}
