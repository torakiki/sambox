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
package org.apache.pdfbox.pdfparser.xref;

import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

/**
 * Component that can merge multiple trailers keeping a history of the merged ones.
 * 
 * @author Andrea Vacondio
 *
 */
public final class TrailerMerger
{
    private TreeMap<Long, COSDictionary> history = new TreeMap<>();
    private COSDictionary trailer;

    /**
     * Merge the given dictionary to the current trailer. It doesn't overwrite values previously set.
     * 
     * @param offset the byte offset for the trailer we are merging
     * @param toMerge
     */
    public void mergeTrailerWithoutOverwriting(long offset, COSDictionary toMerge)
    {
        if (trailer == null)
        {
            trailer = new COSDictionary();
        }
        // TODO check what happens to existing INFO, PREV...
        for (Map.Entry<COSName, COSBase> entry : toMerge.entrySet())
        {
            if (!trailer.containsKey(entry.getKey()))
            {
                trailer.setItem(entry.getKey(), entry.getValue());
            }
        }
        history.put(offset, toMerge);
    }

    public boolean hasTrailer()
    {
        return trailer != null;
    }

    public COSDictionary getTrailer()
    {
        return trailer;
    }

    public COSDictionary getFirstTrailer()
    {
        return history.firstEntry().getValue();
    }

    public COSDictionary getLastTrailer()
    {
        return history.lastEntry().getValue();
    }

    /**
     * Resets the component state
     */
    public void reset()
    {
        trailer = null;
        history.clear();
    }
}
