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
import static org.apache.pdfbox.util.RequireUtils.requireNotNullArg;

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
import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.util.IOUtils;

/**
 * Component capable of writing COS objects using the given {@link BufferedDestinationWriter}.
 * 
 * @author Andrea Vacondio
 */
class COSWriter implements COSVisitor
{
    protected static final byte SPACE = 0x20;
    private static final byte[] CRLF = { '\r', '\n' };
    private static final byte SOLIDUS = 0x2F;
    private static final byte REVERSE_SOLIDUS = 0x5C;
    private static final byte NUMBER_SIGN = 0x23;
    private static final byte LESS_THEN = 0x3C;
    private static final byte GREATER_THEN = 0x3E;
    private static final byte LEFT_PARENTHESIS = 0x28;
    private static final byte RIGHT_PARENTHESIS = 0x29;
    private static final byte LEFT_SQUARE_BRACKET = 0x5B;
    private static final byte RIGHT_SQUARE_BRACKET = 0x5D;
    private static final byte[] STREAM = "stream".getBytes(Charsets.US_ASCII);
    private static final byte[] ENDSTREAM = "endstream".getBytes(Charsets.US_ASCII);
    private BufferedDestinationWriter writer;

    public COSWriter(BufferedDestinationWriter writer)
    {
        requireNotNullArg(writer, "Cannot write to a null writer");
        this.writer = writer;
    }

    @Override
    public void visit(COSArray value) throws IOException
    {
        writer.write(LEFT_SQUARE_BRACKET);
        for (Iterator<COSBase> i = value.iterator(); i.hasNext();)
        {
            COSBase current = i.next();
            Optional.ofNullable(current).orElse(COSNull.NULL).accept(this);
            if (i.hasNext())
            {
                writer.write(SPACE);
            }
        }
        writer.write(RIGHT_SQUARE_BRACKET);
        writer.writeEOL();
    }

    @Override
    public void visit(COSBoolean value) throws IOException
    {
        writer.write(value.toString());
    }

    @Override
    public void visit(COSDictionary dictionary) throws IOException
    {
        writer.write(LESS_THEN);
        writer.write(LESS_THEN);
        writer.writeEOL();
        for (Map.Entry<COSName, COSBase> entry : dictionary.entrySet())
        {
            COSBase value = entry.getValue();
            if (value != null)
            {
                entry.getKey().accept(this);
                writer.write(SPACE);
                entry.getValue().accept(this);
                writer.writeEOL();
            }
        }
        writer.write(GREATER_THEN);
        writer.write(GREATER_THEN);
        writer.writeEOL();
    }

    @Override
    public void visit(COSFloat value) throws IOException
    {
        writer.write(value.toString());
    }

    @Override
    public void visit(COSInteger value) throws IOException
    {
        writer.write(value.toString());
    }

    @Override
    public void visit(COSName value) throws IOException
    {
        writer.write(SOLIDUS);
        byte[] bytes = value.getName().getBytes(Charsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++)
        {
            int current = bytes[i] & 0xFF;
            if (isLetter(current) || isDigit(current))
            {
                writer.write(bytes[i]);
            }
            else
            {
                writer.write(NUMBER_SIGN);
                writer.write(String.format("%02X", current).getBytes(Charsets.US_ASCII));
            }
        }
    }

    @Override
    public void visit(COSNull value) throws IOException
    {
        writer.write("null".getBytes(Charsets.US_ASCII));
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        value.setLong(COSName.LENGTH, value.getFilteredLength());
        visit((COSDictionary) value);
        writer.write(STREAM);
        writer.write(CRLF);
        writer.write(value.getFilteredStream());
        IOUtils.close(value);
        writer.write(CRLF);
        writer.write(ENDSTREAM);
        writer.writeEOL();
    }

    @Override
    public void visit(COSString value) throws IOException
    {
        if (value.isForceHexForm())
        {
            writer.write(LESS_THEN);
            writer.write(value.toHexString());
            writer.write(GREATER_THEN);
        }
        else
        {
            writer.write(LEFT_PARENTHESIS);
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
                    writer.write(REVERSE_SOLIDUS);
                    //$FALL-THROUGH$
                default:
                    writer.write(b);
                }
            }
            writer.write(RIGHT_PARENTHESIS);
        }
    }

    @Override
    public void visit(IndirectCOSObjectReference value) throws IOException
    {
        writer.write(value.toString());
    }

    @Override
    public void visit(COSDocument value)
    {
        // nothing to do
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
    }

    BufferedDestinationWriter writer()
    {
        return this.writer;
    }
}
