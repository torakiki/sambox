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
package org.apache.pdfbox.cos;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.pdfbox.util.Charsets;

/**
 * This class represents a null PDF object.
 *
 * @author Ben Litchfield
 */
public final class COSNull extends COSBase
{
    /**
     * The one null object in the system.
     */
    public static final COSNull NULL = new COSNull();

    private COSNull()
    {
        //limit creation to one instance.
    }

    @Override
    public void accept(COSVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * Writes the {@link COSNull} to the given {@link OutputStream}
     *
     * @param output The stream to write to.
     * @throws IOException If there is an error writing to the stream.
     */
    public void writeTo(OutputStream output) throws IOException
    {
        output.write("null".getBytes(Charsets.ISO_8859_1));
    }
}
