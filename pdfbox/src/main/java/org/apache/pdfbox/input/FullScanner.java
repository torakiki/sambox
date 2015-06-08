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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.xref.Xref;
import org.apache.pdfbox.xref.XrefEntry;

/**
 * Component performing a full scan of the document and retrieves objects definition and the corresponding offset.
 * 
 * @author Andrea Vacondio
 *
 */
class FullScanner
{
    private static final Log LOG = LogFactory.getLog(FullScanner.class);
    private Xref xref = new Xref();
    private SourceReader reader;
    private Pattern objectDefPatter = Pattern.compile("^(\\d+)[\\s](\\d+)[\\s]obj");
    private boolean scanned = false;

    FullScanner(SourceReader reader)
    {
        requireNonNull(reader);
        this.reader = reader;
    }

    private void scan()
    {
        LOG.info("Performing full scan");
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

    private void addEntryIfObjectDefinition(long offset, String line)
    {
        Matcher matcher = objectDefPatter.matcher(line);
        if (matcher.find())
        {
            xref.add(XrefEntry.inUseEntry(Long.parseUnsignedLong(matcher.group(1)), offset,
                    Integer.parseUnsignedInt(matcher.group(2))));
        }
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
