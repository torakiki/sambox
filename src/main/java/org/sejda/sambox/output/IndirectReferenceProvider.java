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

import java.util.concurrent.atomic.AtomicLong;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.cos.NonStorableInObjectStreams;

/**
 * Component responsible for creating the {@link IndirectCOSObjectReference}s keeping count of the next available object
 * number.
 * 
 * @author Andrea Vacondio
 */
class IndirectReferenceProvider
{
    final AtomicLong referencesCounter;

    IndirectReferenceProvider()
    {
        referencesCounter = new AtomicLong(0);
    }

    /**
     * @param next reference number will be highestAlreadyExisting+1
     */
    IndirectReferenceProvider(long highestAlreadyExisting)
    {
        referencesCounter = new AtomicLong(highestAlreadyExisting);
    }

    IndirectCOSObjectReference nextReferenceFor(COSBase baseObject)
    {
        return new IndirectCOSObjectReference(referencesCounter.incrementAndGet(), 0, baseObject);
    }

    IndirectCOSObjectReference nextNonStorableInObjectStreamsReferenceFor(COSBase baseObject)
    {
        return new NonStorableInObjectStreams(referencesCounter.incrementAndGet(), 0, baseObject);
    }

    IndirectCOSObjectReference nextNonStorableInObjectStreamsReference()
    {
        return new NonStorableInObjectStreams(referencesCounter.incrementAndGet(), 0, null);
    }
}
