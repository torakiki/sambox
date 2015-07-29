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

import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.IOException;

import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.IndirectCOSObjectReference;

/**
 * {@link COSWriter} that writes COS objects adding compression to the {@link COSStream}s before writing them.
 * 
 * @author Andrea Vacondio
 */
class CompressedStreamsCOSWriter implements COSWriter
{
    private COSWriter wrapped;

    public CompressedStreamsCOSWriter(COSWriter wrapped)
    {
        requireNotNullArg(wrapped, "Cannot use a null COSWriter");
        this.wrapped = wrapped;
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        value.addCompression();
        wrapped.visit(value);
    }

    @Override
    public void visit(COSDocument value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSArray value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSBoolean value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSDictionary value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSFloat value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSInteger value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSName value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSNull value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(COSString value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void visit(IndirectCOSObjectReference value) throws IOException
    {
        wrapped.visit(value);
    }

    @Override
    public void close() throws IOException
    {
        wrapped.close();
    }

    @Override
    public BufferedCountingChannelWriter writer()
    {
        return this.wrapped.writer();
    }
}
