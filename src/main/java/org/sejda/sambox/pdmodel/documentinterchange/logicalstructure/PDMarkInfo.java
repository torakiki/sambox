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
package org.sejda.sambox.pdmodel.documentinterchange.logicalstructure;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

/**
 * The MarkInfo provides additional information relevant to specialized uses of structured
 * documents.
 *
 * @author Ben Litchfield
 */
public class PDMarkInfo extends PDDictionaryWrapper
{

    public PDMarkInfo()
    {
        super();
    }

    /**
     * Constructor for an existing MarkInfo element.
     */
    public PDMarkInfo(COSDictionary dictionary)
    {
        super(dictionary);
    }


    /**
     * @return true If this is a tagged PDF.
     */
    public boolean isMarked()
    {
        return getCOSObject().getBoolean("Marked", false);
    }

    /**
     * Set if this is a tagged PDF.
     */
    public void setMarked(boolean value)
    {
        getCOSObject().setBoolean("Marked", value);
    }

    /**
     * @return A boolean telling if the structure elements use user properties.
     */
    public boolean usesUserProperties()
    {
        return getCOSObject().getBoolean("UserProperties", false);
    }

    /**
     * Set if the structure elements contain user properties.
     */
    public void setUserProperties(boolean userProps)
    {
        getCOSObject().setBoolean("UserProperties", userProps);
    }

    /**
     * Tells if this PDF contain 'suspect' tags.  See PDF Reference 1.6 section 10.6 "Logical
     * Structure" for more information about this property.
     *
     * @return true if the suspect flag has been set.
     */
    public boolean isSuspect()
    {
        return getCOSObject().getBoolean("Suspects", false);
    }

    /**
     * Set the value of the suspects property.  See PDF Reference 1.6 section 10.6 "Logical
     * Structure" for more information about this property.
     *
     * @param suspect The new "Suspects" value.
     */
    public void setSuspect(boolean suspect)
    {
        getCOSObject().setBoolean("Suspects", false);
    }
}
