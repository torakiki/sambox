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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
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

/**
 * @author Andrea Vacondio
 *
 */
class COSWriter extends DestinationWriter implements COSVisitor
{
    private static final Log LOG = LogFactory.getLog(COSWriter.class);

    public COSWriter(CountingWritableByteChannel channel)
    {
        super(channel);
    }

    @Override
    public void visit(COSArray value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(COSBoolean value) throws IOException
    {
        write(value.toString());
    }

    @Override
    public void visit(COSDictionary value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(COSFloat value)
    {
    }

    @Override
    public void visit(COSInteger value) throws IOException
    {
        write(Long.toString(value.longValue()));

    }

    @Override
    public void visit(COSName value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(COSNull value) throws IOException
    {
        write("null".getBytes(Charsets.ISO_8859_1));

    }

    @Override
    public void visit(COSStream value)
    {
        // alla fine
        // value.setItem(COSName.LENGTH, asDirect(lengthObject));

    }

    @Override
    public void visit(COSString value) throws IOException
    {
    }

    @Override
    public void visit(IndirectCOSObjectReference value) throws IOException
    {
        write(value.xrefEntry().key().toString());
    }

    @Override
    public void visit(COSDocument value) throws IOException
    {
        // TODO Auto-generated method stub

    }

}
