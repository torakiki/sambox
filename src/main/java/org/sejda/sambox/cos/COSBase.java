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
package org.sejda.sambox.cos;

import java.io.IOException;

/**
 * The base object that all objects in the PDF document will extend.
 *
 * @author Ben Litchfield
 */
public abstract class COSBase implements COSObjectable
{
    private String writerKey;

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos object that matches this Java object.
     */
    @Override
    public COSBase getCOSObject()
    {
        return this;
    }

    /**
     * Visitor pattern for the COS model objects
     * 
     * @param visitor
     * @throws IOException
     */
    public abstract void accept(COSVisitor visitor) throws IOException;

    /**
     * @return a key that a writer can use to identify this {@link COSBase} and find out if it has been already written.
     */
    public String writerKey()
    {
        return writerKey;
    }

    /**
     * Assign a writer key to this {@link COSBase} if it doesn't have one already. It can later be used by the writer to
     * find out if the {@link COSBase} was already written and with what object number.
     * 
     * @param writerKey
     */
    public void writerKeyIfAbsent(String writerKey)
    {
        if (!hasWriterKey() && writerKey != null)
        {
            this.writerKey = writerKey.trim();
        }
    }

    private boolean hasWriterKey()
    {
        return this.writerKey != null && this.writerKey.length() > 0;
    }

}
