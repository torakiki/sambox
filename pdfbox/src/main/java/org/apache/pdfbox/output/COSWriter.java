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

import static org.apache.pdfbox.util.CharUtils.isDigit;
import static org.apache.pdfbox.util.CharUtils.isLetter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.COSVisitor;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;

/**
 * @author Andrea Vacondio
 *
 */
class COSWriter extends DestinationWriter implements COSVisitor
{
    private static final byte[] STREAM = "stream".getBytes(Charsets.US_ASCII);
    private static final byte[] ENDSTREAM = "endstream".getBytes(Charsets.US_ASCII);

    public COSWriter(CountingWritableByteChannel channel)
    {
        super(channel);
    }

    @Override
    public void visit(COSArray value) throws IOException
    {
        write(LEFT_SQUARE_BRACKET);
        for (Iterator<COSBase> i = value.iterator(); i.hasNext();)
        {
            COSBase current = i.next();
            Optional.ofNullable(current).orElse(COSNull.NULL).accept(this);
            if (i.hasNext())
            {
                write(SPACE);
            }
        }
        write(RIGHT_SQUARE_BRACKET);
        writeEOL();
    }

    @Override
    public void visit(COSBoolean value) throws IOException
    {
        write(value.toString());
    }

    @Override
    public void visit(COSDictionary dictionary) throws IOException
    {
        write(LESS_THEN);
        write(LESS_THEN);
        writeEOL();
        for (Map.Entry<COSName, COSBase> entry : dictionary.entrySet())
        {
            COSBase value = entry.getValue();
            if (value != null)
            {
                entry.getKey().accept(this);
                write(SPACE);
                entry.getValue().accept(this);
                writeEOL();
            }
        }
        write(GREATER_THEN);
        write(GREATER_THEN);
        writeEOL();
    }

    @Override
    public void visit(COSFloat value) throws IOException
    {
        write(value.toString());
    }

    @Override
    public void visit(COSInteger value) throws IOException
    {
        write(value.toString());
    }

    @Override
    public void visit(COSName value) throws IOException
    {
        write(SOLIDUS);
        byte[] bytes = value.getName().getBytes(Charsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++)
        {
            int current = bytes[i] & 0xFF;
            if (isLetter(current) || isDigit(current))
            {
                write(bytes[i]);
            }
            else
            {
                write(NUMBER_SIGN);
                write(String.format("%02X", current).getBytes(Charsets.US_ASCII));
            }
        }
    }

    @Override
    public void visit(COSNull value) throws IOException
    {
        write("null".getBytes(Charsets.US_ASCII));
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        value.setLong(COSName.LENGTH, value.getFilteredLength());
        visit((COSDictionary) value);
        write(STREAM);
        write(CRLF);
        write(value.getFilteredStream());
        IOUtils.close(value);
        write(CRLF);
        write(ENDSTREAM);
        write(CRLF);
    }

    @Override
    public void visit(COSString value) throws IOException
    {
        if (value.isForceHexForm())
        {
            write(LESS_THEN);
            write(value.toHexString());
            write(GREATER_THEN);
        }
        else
        {
            write(LEFT_PARENTHESIS);
            for (byte b : value.getBytes())
            {
                switch (b)
                {
                case '\n':
                case '\r':
                case '\t':
                case '\b':
                case '\f':
                case '(':
                case ')':
                case '\\':
                    write(REVERSE_SOLIDUS);
                    //$FALL-THROUGH$
                default:
                    write(b);
                }
            }
            write(RIGHT_PARENTHESIS);
        }
    }

    @Override
    public void visit(IndirectCOSObjectReference value) throws IOException
    {
        write(value.xrefEntry().key().toString());
    }

    @Override
    public void visit(COSDocument value)
    {
        // nothing to do
    }

}
