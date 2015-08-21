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
package org.sejda.sambox.input;

import java.io.IOException;
import java.util.Optional;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSVisitor;
import org.sejda.sambox.cos.DisposableCOSObject;

/**
 * An indirect object belonging to an existing pdf document. Indirect objects are defined in Chap 7.3.10 of PDF
 * 32000-1:2008. The {@link COSBase} wrapped by an {@link ExistingIndirectCOSObject} is loaded on demand by querying the
 * associated {@link IndirectObjectsProvider} when the {@link ExistingIndirectCOSObject#getCOSObject()} is called.
 * 
 * @author Andrea Vacondio
 */
public class ExistingIndirectCOSObject extends COSBase implements DisposableCOSObject
{

    private COSBase baseObject;
    private COSObjectKey key;
    private IndirectObjectsProvider provider;
    private String writerKey;

    ExistingIndirectCOSObject(COSObjectKey key, IndirectObjectsProvider provider)
    {
        this.key = key;
        this.provider = provider;
        this.writerKey = String.format("%s %d %d", provider.id(), key.getNumber(),
                key.getGeneration());
        ;
    }

    @Override
    public COSBase getCOSObject()
    {
        // TODO multi thread?
        if (baseObject == null)
        {
            baseObject = Optional.ofNullable(provider.get(key)).orElse(COSNull.NULL);
            baseObject.writerKeyIfAbsent(this.writerKey());
        }
        return baseObject;
    }

    @Override
    public void releaseCOSObject()
    {
        provider.release(key);
        baseObject = null;
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        getCOSObject().accept(visitor);
    }

    public COSObjectKey key()
    {
        return key;
    }

    @Override
    public String writerKey()
    {
        return writerKey;
    }

}
