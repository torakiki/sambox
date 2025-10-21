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

package org.sejda.sambox.pdmodel.interactive.annotation;

import static org.sejda.commons.util.RequireUtils.requireState;

import java.util.HashMap;
import java.util.Map;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;

/**
 * An entry in an appearance dictionary. May contain either a single appearance stream or an appearance subdictionary.
 *
 * @author John Hewson
 */
public class PDAppearanceEntry implements COSObjectable
{
    private COSBase entry;

    public PDAppearanceEntry(COSBase entry)
    {
        this.entry = entry;
    }

    @Override
    public COSBase getCOSObject()
    {
        return entry;
    }

    /**
     * @return true if this entry is an appearance subdictionary.
     */
    public boolean isSubDictionary()
    {
        return !(this.entry instanceof COSStream) && (this.entry instanceof COSDictionary);
    }

    /**
     * @return true if this entry is an appearance stream.
     */
    public boolean isStream()
    {
        return this.entry instanceof COSStream;
    }

    /**
     * @return the entry as an appearance stream.
     *
     * @throws IllegalStateException if this entry is not an appearance stream
     */
    public PDAppearanceStream getAppearanceStream()
    {
        requireState(isStream(), "Invalid appearance stream type was: " + this.entry);
        return new PDAppearanceStream((COSStream) entry);
    }

    /**
     * Returns the entry as an appearance subdictionary.
     *
     * @throws IllegalStateException if this entry is not an appearance subdictionary
     */
    public Map<COSName, PDAppearanceStream> getSubDictionary()
    {
        requireState(isSubDictionary(),
                "Invalid type, expected a sub-dictionary but was: " + this.entry);

        COSDictionary dict = (COSDictionary) entry;
        Map<COSName, PDAppearanceStream> map = new HashMap<>();

        for (COSName name : dict.keySet())
        {
            COSBase value = dict.getDictionaryObject(name);

            // the file from PDFBOX-1599 contains /null as its entry, so we skip non-stream entries
            if (value instanceof COSStream stream)
            {
                map.put(name, new PDAppearanceStream(stream));
            }
            // form fields with NeedsAppearances = true  might have an empty dictionary here instead of a stream
            // in order to define the field valid values for example
            // so adding an entry without a stream
            else 
            {
                if (value instanceof COSNull || value == null)
                {
                    map.put(name, null);    
                }
            }
        }

        return map;
    }
}
