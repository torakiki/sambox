/*
 * Copyright 2021 Sober Lemur S.a.s. di Vacondio Andrea and Sejda BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * A COSBase that will be written as indirect object. This can be used to wrap COSBase (COSName, COSInteger, COSNull,
 * COSBoolean) that are otherwise written as direct object by the body writer.
 * 
 * @author Andrea Vacondio
 */
public class IndirectCOSObject extends COSBase
{

    private COSBase baseObject;

    private IndirectCOSObject(COSBase wrapped)
    {
        this.baseObject = wrapped;
    }

    @Override
    public COSBase getCOSObject()
    {
        return baseObject;
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        baseObject.accept(visitor);
    }

    /**
     * Factory method for an object that will be written as an indirect object.
     * 
     * @param wrapped
     * @return the new instance
     */
    public static IndirectCOSObject asIndirectObject(COSObjectable wrapped)
    {
        return new IndirectCOSObject(
                Optional.ofNullable(wrapped).orElse(COSNull.NULL).getCOSObject());
    }

}
