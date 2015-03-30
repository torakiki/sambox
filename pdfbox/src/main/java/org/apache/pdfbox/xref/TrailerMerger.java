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
package org.apache.pdfbox.xref;

import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSDictionary;

/**
 * Component that can merge multiple trailers keeping a history of the merged ones.
 * 
 * @author Andrea Vacondio
 *
 */
public final class TrailerMerger
{
    private static final Log LOG = LogFactory.getLog(TrailerMerger.class);

    private TreeMap<Long, COSDictionary> history = new TreeMap<>();
    private COSDictionary trailer = new COSDictionary();

    /**
     * Merge the given dictionary to the current trailer. It doesn't overwrite values previously set.
     * 
     * @param offset the byte offset for the trailer we are merging
     * @param toMerge
     */
    public void mergeTrailerWithoutOverwriting(long offset, COSDictionary toMerge)
    {
        LOG.trace("Merging trailer at offset " + offset);
        trailer.mergeInto(toMerge);
        history.put(offset, toMerge);
    }

    public COSDictionary getTrailer()
    {
        return trailer;
    }

    /**
     * @return the first trailer that has been merged
     */
    public COSDictionary getFirstTrailer()
    {
        return history.firstEntry().getValue();
    }

    /**
     * @return the last trailer that has been merged
     */
    public COSDictionary getLastTrailer()
    {
        return history.lastEntry().getValue();
    }

    /**
     * Resets the component state
     */
    public void reset()
    {
        trailer.clear();
        history.clear();
    }
}
