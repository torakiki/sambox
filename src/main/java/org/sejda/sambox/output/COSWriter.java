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

import java.io.Closeable;
import java.io.IOException;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSVisitor;

/**
 * Component capable of writing COS objects using a {@link BufferedCountingChannelWriter}.
 * 
 * @author Andrea Vacondio
 */
interface COSWriter extends COSVisitor, Closeable
{
    /**
     * writes the separator after a {@link COSArray} or a {@link COSDictionary} or a {@link COSStream} are written.
     * 
     * @throws IOException
     */
    default void writeComplexObjectSeparator() throws IOException
    {
        writer().writeEOL();
    }

    /**
     * writes the separator after a {@link COSDictionary} value is written, before the next key/value is written.
     * 
     * @throws IOException
     */
    default void writeDictionaryItemsSeparator() throws IOException
    {
        writer().writeEOL();
    }

    @Override
    default void close() throws IOException
    {
        IOUtils.close(writer());
    }

    BufferedCountingChannelWriter writer();
}
