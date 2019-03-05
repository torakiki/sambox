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
package org.sejda.sambox.pdmodel.graphics.optionalcontent;

import static java.util.Optional.ofNullable;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.sejda.sambox.rendering.RenderDestination;

/**
 * An optional content group (OCG).
 */
public class PDOptionalContentGroup extends PDPropertyList
{
    /**
     * Creates a new optional content group (OCG).
     * 
     * @param name the name of the content group
     */
    public PDOptionalContentGroup(String name)
    {
        this.dict.setItem(COSName.TYPE, COSName.OCG);
        setName(name);
    }

    /**
     * Creates a new instance based on a given {@link COSDictionary}.
     * 
     * @param dict the dictionary
     */
    public PDOptionalContentGroup(COSDictionary dict)
    {
        super(dict);
        if (!dict.getItem(COSName.TYPE).equals(COSName.OCG))
        {
            throw new IllegalArgumentException(
                    "Provided dictionary is not of type '" + COSName.OCG + "'");
        }
    }

    /**
     * Enumeration for the renderState dictionary entry on the "Export", "View" and "Print" dictionary.
     */
    public enum RenderState
    {
        /** The "ON" value. */
        ON(COSName.ON),
        /** The "OFF" value. */
        OFF(COSName.OFF);

        private final COSName name;

        private RenderState(COSName value)
        {
            this.name = value;
        }

        /**
         * Returns the base state represented by the given {@link COSName}.
         *
         * @param state the state name
         * @return the state enum value
         */
        public static RenderState valueOf(COSName state)
        {
            if (state == null)
            {
                return null;
            }

            return RenderState.valueOf(state.getName().toUpperCase());
        }

        /**
         * Returns the PDF name for the state.
         *
         * @return the name of the state
         */
        public COSName getName()
        {
            return this.name;
        }
    }

    /**
     * Returns the name of the optional content group.
     * 
     * @return the name
     */
    public String getName()
    {
        return dict.getString(COSName.NAME);
    }

    /**
     * Sets the name of the optional content group.
     * 
     * @param name the name
     */
    public void setName(String name)
    {
        dict.setString(COSName.NAME, name);
    }

    // TODO Add support for "Intent"
    /**
     * @param destination to be rendered
     * @return state or null if undefined
     */
    public RenderState getRenderState(RenderDestination destination)
    {
        COSName state = null;
        COSDictionary usage = dict.getDictionaryObject("Usage", COSDictionary.class);
        if (usage != null)
        {
            if (RenderDestination.PRINT.equals(destination))
            {
                state = ofNullable(usage.getDictionaryObject("Print", COSDictionary.class))
                        .map(p -> p.getDictionaryObject("PrintState", COSName.class)).orElse(null);
            }
            else if (RenderDestination.VIEW.equals(destination))
            {
                state = ofNullable(usage.getDictionaryObject("View", COSDictionary.class))
                        .map(v -> v.getDictionaryObject("ViewState", COSName.class)).orElse(null);
            }
            // Fallback to export
            if (state == null)
            {
                state = ofNullable(usage.getDictionaryObject("Export", COSDictionary.class))
                        .map(e -> e.getDictionaryObject("ExportState", COSName.class)).orElse(null);
            }
        }
        return ofNullable(state).map(RenderState::valueOf).orElse(null);
    }

    @Override
    public String toString()
    {
        return super.toString() + " (" + getName() + ")";
    }
}
