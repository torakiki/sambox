/*
 * Created on 27/ago/2015
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sejda.sambox.output;

import static java.util.Optional.ofNullable;

import java.io.IOException;

import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSBase;

/**
 * {@link COSWriter} implementation that writes dictionary and array values as indirect references if they have been
 * added as indirect references to the context.
 * 
 * @author Andrea Vacondio
 *
 */
class IndirectReferencesAwareCOSWriter extends DefaultCOSWriter
{

    final PDFWriteContext context;

    IndirectReferencesAwareCOSWriter(CountingWritableByteChannel channel, PDFWriteContext context)
    {
        this(new BufferedCountingChannelWriter(channel), context);
    }

    IndirectReferencesAwareCOSWriter(BufferedCountingChannelWriter writer, PDFWriteContext context)
    {
        super(writer);
        this.context = context;
    }

    /**
     * writes the given dictionary or array value item
     * 
     * @throws IOException
     */
    @Override
    void writeValue(COSBase value) throws IOException
    {
        ofNullable((COSBase) context.getIndirectReferenceFor(value)).orElse(value).accept(this);
    }

}
