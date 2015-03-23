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
package org.apache.pdfbox.pdfparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.xref.CompressedXrefEntry;
import org.apache.pdfbox.xref.Xref;
import org.apache.pdfbox.xref.XrefEntry;

/**
 * This will parse a PDF 1.5 (or better) Xref stream and
 * extract the xref information from the stream.
 *
 *  @author Justin LeFebvre
 */
public class PDFXrefStreamParser extends BaseParser
{
    private COSStream stream;
    private Xref xref;

    /**
     * Constructor.
     *
     * @param strm The stream to parse.
     * @param doc The document for the current parsing.
     * @param xref
     *
     * @throws IOException If there is an error initializing the stream.
     */
    public PDFXrefStreamParser(COSStream strm, COSDocument doc, Xref xref)
            throws IOException
    {
        super(strm.getUnfilteredStream());
        document = doc;
        stream = strm;
        this.xref = xref;
    }

    /**
     * Parses through the unfiltered stream and populates the xrefTable HashMap.
     * @throws IOException If there is an error while parsing the stream.
     */
    public void parse() throws IOException
    {
        try
        {
            // TODO Refactor all this is just to get object numbers
            COSArray xrefFormat = (COSArray)stream.getDictionaryObject(COSName.W);
            COSArray indexArray = (COSArray)stream.getDictionaryObject(COSName.INDEX);
            /*
             * If Index doesn't exist, we will use the default values.
             */
            if(indexArray == null)
            {
                indexArray = new COSArray();
                indexArray.add(COSInteger.ZERO);
                indexArray.add(stream.getDictionaryObject(COSName.SIZE));
            }

            ArrayList<Long> objNums = new ArrayList<Long>();

            /*
             * Populates objNums with all object numbers available
             */
            Iterator<COSBase> indexIter = indexArray.iterator();
            while(indexIter.hasNext())
            {
                long objID = ((COSInteger)indexIter.next()).longValue();
                int size = ((COSInteger)indexIter.next()).intValue();
                for(int i = 0; i < size; i++)
                {
                    objNums.add(objID + i);
                }
            }
            Iterator<Long> objIter = objNums.iterator();
            // TODO END Refactor all this is just to get object numbers
            /*
             * Calculating the size of the line in bytes
             */
            int w0 = xrefFormat.getInt(0);
            int w1 = xrefFormat.getInt(1);
            int w2 = xrefFormat.getInt(2);
            int lineSize = w0 + w1 + w2;

            while(pdfSource.available() > 0 && objIter.hasNext())
            {
                byte[] currLine = new byte[lineSize];
                pdfSource.read(currLine);

                // Need to remember the current objID
                Long objID = objIter.next();

                int type = 0;
                /*
                 * Grabs the number of bytes specified for the first column in
                 * the W array and stores it.
                 */
                for(int i = 0; i < w0; i++)
                {
                    type += (currLine[i] & 0x00ff) << ((w0 - i - 1)* 8);
                }
                int field1 = 0;
                for (int i = 0; i < w1; i++)
                {
                    field1 += (currLine[i + w0] & 0x00ff) << ((w1 - i - 1) * 8);
                }
                int field2 = 0;
                for (int i = 0; i < w2; i++)
                {
                    field2 += (currLine[i + w0 + w1] & 0x00ff) << ((w2 - i - 1) * 8);
                }
                /*
                 * 3 different types of entries.
                 */
                switch(type)
                {
                    case 0:
                    // xref.add(XrefEntry.freeEntry(objID, field1, field2));
                        break;
                    case 1:
                    xref.add(XrefEntry.inUseEntry(objID, field1, field2));
                        break;
                    case 2:
                    xref.add(CompressedXrefEntry.compressedEntry(objID, field1));
                        break;
                    default:
                        break;
                }
            }
        }
        finally
        {
            pdfSource.close();
        }
    }
}
