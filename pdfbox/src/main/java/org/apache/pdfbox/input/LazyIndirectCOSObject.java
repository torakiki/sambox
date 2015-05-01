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

/**
 * @author Andrea Vacondio
 *
 */
public class LazyIndirectCOSObject extends COSBase
{

    private COSBase baseObject;
    private COSObjectKey key;
    private IndirectObjectsProvider provider;

    public LazyIndirectCOSObject(COSObjectKey key, IndirectObjectsProvider provider)
    {
        // TODO verify if we need a null check here
        this.key = key;
        this.provider = provider;
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
    public void accept(COSVisitor visitor) throws IOException
    {
        getCOSObject().accept(visitor);
    }

    public COSObjectKey key()
    {
        return key;
    }

}
