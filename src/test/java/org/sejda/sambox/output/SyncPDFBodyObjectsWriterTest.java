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
package org.sejda.sambox.output;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.sejda.sambox.cos.IndirectCOSObjectReference;

/**
 * @author Andrea Vacondio
 *
 */
public class SyncPDFBodyObjectsWriterTest
{
    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new SyncPDFBodyObjectsWriter(null);
    }

    @Test
    public void writeObject() throws Exception
    {
        IndirectObjectsWriter writer = mock(IndirectObjectsWriter.class);
        SyncPDFBodyObjectsWriter victim = new SyncPDFBodyObjectsWriter(writer);
        IndirectCOSObjectReference ref = mock(IndirectCOSObjectReference.class);
        victim.writeObject(ref);
        verify(writer).writeObjectIfNotWritten(ref);
    }
}
