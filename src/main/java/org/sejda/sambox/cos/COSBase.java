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

import static java.util.Objects.nonNull;

import java.io.IOException;

/**
 * The base object that all objects in the PDF document will extend.
 *
 * @author Ben Litchfield
 */
public abstract class COSBase implements COSObjectable
{
    private IndirectCOSObjectIdentifier id;

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
     * @return an identifier that can be used to identify this {@link COSBase}.
     */
    public IndirectCOSObjectIdentifier id()
    {
        return id;
    }

    /**
     * Assign an id to this {@link COSBase} if it doesn't have one already.
     * 
     * @param id
     */
    public void idIfAbsent(IndirectCOSObjectIdentifier id)
    {
        if (!hasId() && nonNull(id))
        {
            this.id = id;
        }
    }

    /**
     * @return true if the {@link COSBase} has an id assigned to it
     */
    public boolean hasId()
    {
        return nonNull(id());
    }

}
