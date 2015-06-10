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
package org.apache.pdfbox.input;

import java.io.IOException;
import java.util.Optional;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSVisitor;
import org.apache.pdfbox.cos.DisposableCOSObject;

/**
 * @author Andrea Vacondio
 *
 */
public class IndirectCOSObject extends COSBase implements DisposableCOSObject
{

    private COSBase baseObject;
    private COSObjectKey key;
    private IndirectObjectsProvider provider;
    private String sourceId;

    public IndirectCOSObject(COSObjectKey key, IndirectObjectsProvider provider)
    {
        this.key = key;
        this.provider = provider;
        this.sourceId = provider.id();
    }

    @Override
    public COSBase getCOSObject()
    {
        // TODO multi thread?
        if (baseObject == null)
        {
            baseObject = Optional.ofNullable(provider.get(key)).orElse(COSNull.NULL);
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

    /**
     * @return the id of the source this indirect object was read from, this is used to identify objects read from the
     * same file.
     */
    public String sourceId()
    {
        return sourceId;
    }

}
