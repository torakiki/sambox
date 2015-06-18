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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.pdfbox.input.ExistingIndirectCOSObject;
import org.apache.pdfbox.xref.XrefEntry;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class IndirectCOSObjectReferenceTest
{

    @Test
    public void nullCOSBase()
    {
        IndirectCOSObjectReference victim = new IndirectCOSObjectReference(1, 1, null);
        assertEquals(COSNull.NULL, victim.getCOSObject());
    }

    @Test
    public void correctXrefEntry()
    {
        IndirectCOSObjectReference victim = new IndirectCOSObjectReference(20, 10,
                COSInteger.get(5));
        assertEquals(20, victim.xrefEntry().getObjectNumber());
        assertEquals(10, victim.xrefEntry().getGenerationNumber());
        assertEquals(XrefEntry.UNKNOWN_OFFSET, victim.xrefEntry().getByteOffset());
    }

    @Test
    public void release()
    {
        IndirectCOSObjectReference victim = new IndirectCOSObjectReference(1, 1, COSInteger.get(5));
        assertEquals(COSInteger.get(5), victim.getCOSObject());
        victim.releaseCOSObject();
        assertEquals(COSNull.NULL, victim.getCOSObject());
    }

    @Test
    public void releaseDisposable()
    {
        ExistingIndirectCOSObject cosObject = mock(ExistingIndirectCOSObject.class);
        IndirectCOSObjectReference victim = new IndirectCOSObjectReference(1, 1, cosObject);
        victim.releaseCOSObject();
        verify(cosObject).releaseCOSObject();
    }
}
