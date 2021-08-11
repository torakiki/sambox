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
 * This class represents a boolean value in the PDF document.
 *
 * @author Ben Litchfield
 */
public final class COSBoolean extends COSBase
{
    public static final COSBoolean TRUE = new COSBoolean(true);
    public static final COSBoolean FALSE = new COSBoolean(false);

    private final boolean value;

    private COSBoolean(boolean value)
    {
        this.value = value;
    }

    /**
     * @return The boolean value of this object.
     */
    public boolean getValue()
    {
        return value;
    }

    /**
     * @param value Parameter telling which boolean value to get.
     *
     * @return The single boolean instance that matches the parameter.
     */
    public static COSBoolean valueOf(boolean value)
    {
        return value ? TRUE : FALSE;
    }

    @Override
    public String toString()
    {
        return Boolean.toString(value);
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    @Override
    public void idIfAbsent(IndirectCOSObjectIdentifier id)
    {
        // we don't store id for booleans. We write them as direct objects. Wrap this with an IndirectCOSObject if you
        // want to write a number as indirect reference
    }
}
