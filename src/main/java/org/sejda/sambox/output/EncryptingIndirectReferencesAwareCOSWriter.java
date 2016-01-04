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

import java.io.IOException;

import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;

/**
 * {@link IndirectReferencesAwareCOSWriter} implementation that will encrypt {@link COSString} and {@link COSStream} if
 * the {@link PDFWriteContext} has an encryptor.
 * 
 * @author Andrea Vacondio
 *
 */
class EncryptingIndirectReferencesAwareCOSWriter extends IndirectReferencesAwareCOSWriter
{

    EncryptingIndirectReferencesAwareCOSWriter(BufferedCountingChannelWriter writer,
            PDFWriteContext context)
    {
        super(writer, context);
    }

    EncryptingIndirectReferencesAwareCOSWriter(CountingWritableByteChannel channel,
            PDFWriteContext context)
    {
        super(channel, context);
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        if (context.encryptor.isPresent())
        {
            context.encryptor.get().visit(value);
        }
        super.visit(value);
    }

    @Override
    public void visit(COSString value) throws IOException
    {
        if (context.encryptor.isPresent())
        {
            context.encryptor.get().visit(value);
            value.setForceHexForm(true);
        }
        super.visit(value);
    }
}
