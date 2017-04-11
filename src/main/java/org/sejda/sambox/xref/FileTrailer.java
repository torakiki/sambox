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
package org.sejda.sambox.xref;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

/**
 * File trailer with information regarding the xref offset
 * 
 * @author Andrea Vacondio
 *
 */
public class FileTrailer extends PDDictionaryWrapper
{
    private long xrefOffset = -1;

    public FileTrailer()
    {
        super();
    }

    public FileTrailer(COSDictionary trailerDictionary)
    {
        super(trailerDictionary);
    }

    public long xrefOffset()
    {
        return xrefOffset;
    }

    public void xrefOffset(long offset)
    {
        this.xrefOffset = offset;
    }

}
