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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.IndirectCOSObjectIdentifier;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class IncrementablePDDocument implements Closeable
{

    private Map<IndirectCOSObjectIdentifier, COSObjectable> replacements = new ConcurrentHashMap<>();
    private Set<COSObjectable> newObjects = new HashSet<>();

    private PDDocument incremented;
    public final COSParser parser;

    IncrementablePDDocument(PDDocument incremented, COSParser parser)
    {
        requireNotNullArg(incremented, "Incremented document cannot be null");
        this.incremented = incremented;
        this.parser = parser;
    }

    public PDDocument incremented()
    {
        return incremented;
    }

    /**
     * Replaces the object with the given {@link IndirectCOSObjectIdentifier} during the incremental update
     * 
     * @param toReplace
     * @param replacement
     */
    public void replace(IndirectCOSObjectIdentifier toReplace, COSObjectable replacement)
    {
        requireNotNullArg(replacement, "Missing id of the object to be replaced");
        replacements.put(toReplace, ofNullable(replacement).orElse(COSNull.NULL));
    }

    /**
     * Adds the given object to be written during the incremental update
     * 
     * @param newObject
     */
    public void add(COSObjectable newObject)
    {
        if (nonNull(newObject))
        {
            newObjects.add(newObject);
        }
    }

    @Override
    public void close() throws IOException
    {
        incremented.close();
        IOUtils.close(parser.provider());
        IOUtils.close(parser);
    }

}
