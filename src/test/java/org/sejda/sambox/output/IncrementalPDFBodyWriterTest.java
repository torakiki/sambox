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

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.input.ExistingIndirectCOSObject;

/**
 * @author Andrea Vacondio
 *
 */
public class IncrementalPDFBodyWriterTest
{

    private IncrementalPDFBodyWriter victim;
    private PDFWriteContext context;
    private IndirectObjectsWriter writer;

    @Before
    public void setUp()
    {
        writer = mock(IndirectObjectsWriter.class);
        context = mock(PDFWriteContext.class);
        victim = new IncrementalPDFBodyWriter(writer, context);
    }

    @Test
    public void createIndirectReferenceIfNeededForExisting()
    {
        ExistingIndirectCOSObject ref = mock(ExistingIndirectCOSObject.class);
        victim.createIndirectReferenceIfNeededFor(ref);
        verify(context).addExistingReference(ref);
    }

    @Test
    public void createIndirectReferenceIfNeededFor()
    {
        COSDictionary ref = new COSDictionary();
        victim.createIndirectReferenceIfNeededFor(ref);
        verify(context).getOrCreateIndirectReferenceFor(ref);
    }
}
