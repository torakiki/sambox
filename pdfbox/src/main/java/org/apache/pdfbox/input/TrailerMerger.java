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

import org.apache.pdfbox.cos.COSDictionary;

/**
 * Component that can merge multiple trailers keeping a history of the merged ones.
 * 
 * @author Andrea Vacondio
 *
 */
final class TrailerMerger
{
    private COSDictionary trailer = new COSDictionary();

    /**
     * Merge the given dictionary to the current trailer. It doesn't overwrite values previously set.
     * 
     * @param toMerge
     */
    public void mergeWithoutOverwriting(COSDictionary toMerge)
    {
        trailer.mergeWithoutOverwriting(toMerge);
    }

    /**
     * Merge the given dictionary to the current trailer. It overwrites values previously set.
     * 
     * @param toMerge
     */
    public void merge(COSDictionary toMerge)
    {
        trailer.mergeWithoutOverwriting(toMerge);
    }

    public COSDictionary getTrailer()
    {
        return trailer;
    }

    /**
     * Resets the component state
     */
    public void reset()
    {
        trailer.clear();
    }
}
