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
import java.util.Optional;

import org.sejda.sambox.xref.XrefEntry;

/**
 * A disposable indirect object reference. It holds all the necessary information to write a {@link COSBase} as an
 * indirect reference.
 * 
 * @author Andrea Vacondio
 */
public class IndirectCOSObjectReference extends COSBase implements DisposableCOSObject
{
    private COSBase baseObject;
    private XrefEntry xrefEntry;

    public IndirectCOSObjectReference(long objectNumber, int generationNumber, COSBase baseObject)
    {
        this.xrefEntry = XrefEntry.unknownOffsetEntry(objectNumber, generationNumber);
        this.baseObject = baseObject;
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    public XrefEntry xrefEntry()
    {
        return xrefEntry;
    }

    public void setValue(COSBase baseObject)
    {
        this.baseObject = baseObject;
    }

    @Override
    public COSBase getCOSObject()
    {
        return Optional.ofNullable(baseObject).orElse(COSNull.NULL);
    }

    @Override
    public void releaseCOSObject()
    {
        if (baseObject instanceof DisposableCOSObject)
        {
            ((DisposableCOSObject) baseObject).releaseCOSObject();
        }
        baseObject = null;
    }

    @Override
    public String toString()
    {
        return Long.toString(xrefEntry().key().objectNumber()) + " "
                + Integer.toString(xrefEntry().key().generation()) + " R";
    }

}
