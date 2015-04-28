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

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.COSVisitor;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;

/**
 * Visitor implementation that encrypts COS objects when necessary before forwarding calls to the decorated visitor.
 * 
 * @author Andrea Vacondio
 *
 */
class COSEncryptor implements COSVisitor
{

    private COSVisitor wrapped;

    COSEncryptor(COSVisitor wrapped)
    {
        this.wrapped = wrapped;
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
    public void visit(COSStream value) throws IOException
    {
        // encrypt
        wrapped.visit(value);
    }

    @Override
    public void visit(COSString value) throws IOException
    {
        // encrypt
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

}
