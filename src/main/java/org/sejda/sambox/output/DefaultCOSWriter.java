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

import static java.util.Objects.isNull;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.sambox.util.CharUtils.isDigit;
import static org.sejda.sambox.util.CharUtils.isLetter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.util.Hex;

/**
 * Default implementation of a {@link COSWriter} that writes COS objects to the given
 * {@link CountingWritableByteChannel} or {@link BufferedCountingChannelWriter}
 * 
 * @author Andrea Vacondio
 */
class DefaultCOSWriter implements COSWriter
{
    protected static final byte SPACE = 0x20;
    private static final byte[] CRLF = { '\r', '\n' };
    private static final byte SOLIDUS = 0x2F;
    private static final byte REVERSE_SOLIDUS = 0x5C;
    private static final byte NUMBER_SIGN = 0x23;
    protected static final byte PERCENT_SIGN = 0x25;
    private static final byte LESS_THEN = 0x3C;
    private static final byte GREATER_THEN = 0x3E;
    private static final byte LEFT_PARENTHESIS = 0x28;
    private static final byte RIGHT_PARENTHESIS = 0x29;
    private static final byte LEFT_SQUARE_BRACKET = 0x5B;
    private static final byte RIGHT_SQUARE_BRACKET = 0x5D;
    private static final byte[] STREAM = "stream".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ENDSTREAM = "endstream".getBytes(StandardCharsets.US_ASCII);
    private BufferedCountingChannelWriter writer;

    public DefaultCOSWriter(CountingWritableByteChannel channel)
    {
        requireNotNullArg(channel, "Cannot write to a null channel");
        this.writer = new BufferedCountingChannelWriter(channel);
    }

    public DefaultCOSWriter(BufferedCountingChannelWriter writer)
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
            writeValue(Optional.ofNullable(current).orElse(COSNull.NULL));
            if (i.hasNext())
            {
                writer.write(SPACE);
            }
        }
        writer.write(RIGHT_SQUARE_BRACKET);
        writeComplexObjectSeparator();
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
        writeDictionaryItemsSeparator();
        for (Map.Entry<COSName, COSBase> entry : dictionary.entrySet())
        {
            COSBase value = entry.getValue();
            if (value != null)
            {
                entry.getKey().accept(this);
                writer.write(SPACE);
                writeValue(entry.getValue());
                writeDictionaryItemsSeparator();
            }
        }
        writer.write(GREATER_THEN);
        writer.write(GREATER_THEN);
        writeComplexObjectSeparator();
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
        byte[] bytes = value.getName().getBytes(StandardCharsets.UTF_8);
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
                writer.write(Hex.getBytes(bytes[i]));
            }
        }
    }

    @Override
    public void visit(COSNull value) throws IOException
    {
        writer.write("null".getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        try
        {
            COSBase length = value.getItem(COSName.LENGTH);
            if (isNull(length))
            {
                value.setLong(COSName.LENGTH, value.getFilteredLength());
            }
            visit((COSDictionary) value);
            writer.write(STREAM);
            writer.write(CRLF);
            long streamStartingPosition = writer.offset();
            writer.write(value.getFilteredStream());
            if (length instanceof IndirectCOSObjectReference)
            {
                ((IndirectCOSObjectReference) length)
                        .setValue(new COSInteger(writer.offset() - streamStartingPosition));
            }
            writer.write(CRLF);
            writer.write(ENDSTREAM);
            writeComplexObjectSeparator();
        }
        finally
        {
            IOUtils.closeQuietly(value);
        }
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

    /**
     * writes the given dictionary or array value item
     * 
     * @throws IOException
     */
    void writeValue(COSBase value) throws IOException
    {
        value.accept(this);
    }

    @Override
    public BufferedCountingChannelWriter writer()
    {
        return this.writer;
    }
}
