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

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sejda.sambox.xref.Xref;
import org.sejda.sambox.xref.XrefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component performing a full scan of the document and retrieving objects definition and the corresponding offset. This
 * implementation is lazy and the full scan is performed the first time the entries are accessed.
 * 
 * @author Andrea Vacondio
 */
class ObjectsFullScanner
{
    private static final Logger LOG = LoggerFactory.getLogger(ObjectsFullScanner.class);
    private static final Pattern OBJECT_DEF_PATTERN = Pattern
            .compile("^(\\d+)[\\s]+(\\d+)[\\s]+obj");

    private Xref xref = new Xref();
    private SourceReader reader;
    private boolean scanned = false;

    ObjectsFullScanner(SourceReader reader)
    {
        requireNotNullArg(reader, "Cannot read from a null reader");
        this.reader = reader;
    }

    private void scan()
    {
        LOG.info("Performing full scan to retrieve objects");
        try
        {
            long savedPos = reader.position();
            reader.position(0);
            reader.skipSpaces();
            while (reader.source().peek() != -1)
            {
                long offset = reader.position();
                addEntryIfObjectDefinition(offset, reader.readLine());
                reader.skipSpaces();
            }
            reader.position(savedPos);
        }
        catch (IOException e)
        {
            LOG.error("An error occurred performing a full scan of the document", e);
        }
    }

    private void addEntryIfObjectDefinition(long offset, String line) throws IOException
    {
        Matcher matcher = OBJECT_DEF_PATTERN.matcher(line);
        if (matcher.find())
        {
            xref.add(XrefEntry.inUseEntry(Long.parseUnsignedLong(matcher.group(1)), offset,
                    Integer.parseUnsignedInt(matcher.group(2))));
            onObjectDefinitionLine(offset, line);
        }
        else
        {
            onNonObjectDefinitionLine(offset, line);
        }
    }

    /**
     * Called when the the scanner has read a line which is not an object definition
     * 
     * @param originalOffset offset from where the line was read
     * @param line
     * @throws IOException
     */
    protected void onNonObjectDefinitionLine(long originalOffset, String line) throws IOException
    {
        // nothing
    }

    /**
     * Called when the the scanner has read a line which is an object definition
     * 
     * @param originalOffset offset from where the line was read
     * @param line
     * @throws IOException
     */
    protected void onObjectDefinitionLine(long originalOffset, String line) throws IOException
    {
        // nothing
    }

    /**
     * @return the scanned entries
     */
    Xref entries()
    {
        if (!scanned)
        {
            this.scanned = true;
            scan();
        }
        return xref;
    }
}
