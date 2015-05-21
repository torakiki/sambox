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
package org.apache.pdfbox.output;

import static org.apache.pdfbox.util.SpecVersionUtils.PDF_HEADER;

import java.io.IOException;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.xref.XrefEntry;

/**
 * @author Andrea Vacondio
 *
 */
class PDFWriter extends COSWriter
{
    private static final Log LOG = LogFactory.getLog(PDFWriter.class);

    private static final byte[] COMMENT = { '%' };
    private static final byte[] GARBAGE = new byte[] { (byte) 0xA7, (byte) 0xE3, (byte) 0xF1,
            (byte) 0xF1 };
    private static final byte[] OBJ = "obj".getBytes(Charsets.US_ASCII);
    private static final byte[] ENDOBJ = "endobj".getBytes(Charsets.US_ASCII);

    private TreeMap<Long, XrefEntry> written = new TreeMap<>();

    PDFWriter(CountingWritableByteChannel channel)
    {
        super(channel);
    }

    void writeHeader(String version) throws IOException
    {
        LOG.debug("Writing header " + version);
        write(PDF_HEADER);
        write(version);
        writeEOL();
        write(COMMENT);
        write(GARBAGE);
        writeEOL();
    }

    void writerObject(IndirectCOSObjectReference object) throws IOException
    {
        if (written.get(object.xrefEntry().getObjectNumber()) == null)
        {
            doWriteObject(object);
            written.put(object.xrefEntry().getObjectNumber(), object.xrefEntry());
        }
    }

    private void doWriteObject(IndirectCOSObjectReference object) throws IOException
    {
        object.xrefEntry().setByteOffset(offset());
        write(Long.toString(object.xrefEntry().getObjectNumber()));
        write(SPACE);
        write(Integer.toString(object.xrefEntry().getGenerationNumber()));
        write(SPACE);
        write(OBJ);
        writeEOL();
        object.getCOSObject().accept(this);
        writeEOL();
        write(ENDOBJ);
        writeEOL();
        LOG.trace("Written object " + object.xrefEntry());
    }

    void writeBody(COSDocument document) throws IOException
    {
        LOG.debug("Writing body");
        try (AbstractPdfBodyWriter bodyWriter = new AsyncPdfBodyWriter(this))
        {
            document.accept(bodyWriter);
        }
    }

    /**
     * writes the xref table
     * 
     * @return the startxref value
     * @throws IOException
     */
    long writeXrefTable() throws IOException
    {
        long startxref = offset();
        LOG.debug("Writing xref table at offset " + startxref);
        if (written.put(0L, XrefEntry.DEFAULT_FREE_ENTRY) != null)
        {
            LOG.warn("Reserved object number 0 has been overwritten with the expected free entry");
        }
        write("xref");
        writeEOL();
        write("0 " + written.size());
        writeEOL();
        for (long key = 0; key <= written.lastKey(); key++)
        {
            write(Optional.ofNullable(written.get(key)).orElse(XrefEntry.DEFAULT_FREE_ENTRY)
                    .toXrefTableEntry());
        }
        return startxref;
    }

    void writeTrailer(COSDictionary trailer, long startxref) throws IOException
    {
        LOG.trace("Writing trailer");
        trailer.removeItem(COSName.PREV);
        trailer.removeItem(COSName.XREF_STM);
        trailer.removeItem(COSName.DOC_CHECKSUM);
        trailer.setLong(COSName.SIZE, written.lastKey() + 1);
        write("trailer".getBytes(Charsets.US_ASCII));
        writeEOL();
        visit(trailer);
        write("startxref".getBytes(Charsets.US_ASCII));
        writeEOL();
        write(Long.toString(startxref));
        writeEOL();
        write("%%EOF".getBytes(Charsets.US_ASCII));
        writeEOL();
    }

    void writeXrefStream(COSDictionary trailer) throws IOException
    {
        long startxref = offset();
        LOG.debug("Writing xref stream at offset " + startxref);
        XrefEntry entry = XrefEntry.inUseEntry(written.lastKey() + 1, startxref, 0);
        written.put(entry.getObjectNumber(), entry);
        doWriteObject(new IndirectCOSObjectReference(entry.getObjectNumber(),
                entry.getGenerationNumber(), new XrefStream(trailer, written)));
        write("startxref".getBytes(Charsets.US_ASCII));
        writeEOL();
        write(Long.toString(startxref));
        writeEOL();
        write("%%EOF".getBytes(Charsets.US_ASCII));
        writeEOL();
    }

    @Override
    public void close() throws IOException
    {
        written.clear();
        super.close();
    }
}
