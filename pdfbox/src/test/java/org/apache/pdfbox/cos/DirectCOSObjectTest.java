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

import static org.apache.pdfbox.cos.DirectCOSObject.asDirectObject;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class DirectCOSObjectTest
{

    @Test
    public void nullConstructor()
    {
        assertEquals(COSNull.NULL, asDirectObject(null).getCOSObject());
    }

    @Test
    public void constructor()
    {
        assertEquals(COSInteger.get(4), asDirectObject(COSInteger.get(4)).getCOSObject());
    }

    @Test
    public void cosDereferenced()
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 2, COSInteger.get(4));
        assertEquals(COSInteger.get(4), asDirectObject(ref).getCOSObject());
    }

    @Test
    public void visit() throws IOException
    {
        COSDictionary dictionary = mock(COSDictionary.class);
        when(dictionary.getCOSObject()).thenReturn(dictionary);
        COSVisitor visitor = mock(COSVisitor.class);
        asDirectObject(dictionary).accept(visitor);
        verify(dictionary).accept(visitor);
    }
}
