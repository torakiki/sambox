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

    public static final byte[] COMMENT = { '%' };
    public static final byte[] GARBAGE = new byte[] { (byte) 0x9d, (byte) 0xe3, (byte) 0xf1,
            (byte) 0xf1 };
    public static final byte[] OBJ = "obj".getBytes(Charsets.ISO_8859_1);
    public static final byte[] ENDOBJ = "endobj".getBytes(Charsets.ISO_8859_1);

    private TreeMap<Long, XrefEntry> written = new TreeMap<>();

    public PDFWriter(CountingWritableByteChannel channel)
    {
        super(channel);
    }

    public void writeHeader(float version) throws IOException
    {
        LOG.trace("Writing header");
        write("%PDF-" + Float.toString(version));
        writeEOL();
        write(COMMENT);
        write(GARBAGE);
        writeEOL();
    }

    public void writerObject(IndirectCOSObjectReference object) throws IOException
    {
        if (written.get(object.xrefEntry().getObjectNumber()) == null)
        {
            object.xrefEntry().setByteOffset(offset());
            write(Long.toString(object.xrefEntry().getObjectNumber()));
            writeSpace();
            write(Integer.toString(object.xrefEntry().getGenerationNumber()));
            writeSpace();
            write(OBJ);
            writeEOL();
            object.getCOSObject().accept(this);
            writeEOL();
            write(ENDOBJ);
            writeEOL();
            written.put(object.xrefEntry().getObjectNumber(), object.xrefEntry());
            LOG.trace("Written object " + object.xrefEntry());
        }
    }

    public void writeBody(COSDocument document) throws IOException
    {
        try (PdfBodyWriter bodyWriter = new PdfBodyWriter(this))
        {
            bodyWriter.visit(document);
        }
    }

    /**
     * writes the xref table
     * 
     * @return the startxref value
     * @throws IOException
     */
    public long writeXrefTable() throws IOException
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

    public void writeTrailer(COSDictionary trailer, long startxref) throws IOException
    {
        LOG.trace("Writing trailer");
        trailer.removeItem(COSName.PREV);
        trailer.removeItem(COSName.XREF_STM);
        trailer.removeItem(COSName.DOC_CHECKSUM);
        trailer.setLong(COSName.SIZE, written.lastKey() + 1);
        write("trailer");
        writeEOL();
        visit(trailer);
        write("startxref");
        writeEOL();
        write(Long.toString(startxref));
        writeEOL();
        write("%%EOF");
        writeEOL();
    }

    @Override
    public void close() throws IOException
    {
        written.clear();
        super.close();
    }
}
