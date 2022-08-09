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
package org.sejda.sambox.pdmodel.interactive.action;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;

/**
 * This represents a JavaScript action.
 *
 * @author Michael Schwarzenberger
 */
public class PDActionJavaScript extends PDAction
{
    /**
     * This type of action this object represents.
     */
    public static final String SUB_TYPE = "JavaScript";

    /**
     * Constructor #1.
     */
    public PDActionJavaScript()
    {
        setSubType(SUB_TYPE);
    }

    /**
     * Constructor.
     *
     * @param js Some javascript code.
     */
    public PDActionJavaScript(String js)
    {
        this();
        setAction(js);
    }

    /**
     * Constructor #2.
     *
     * @param a The action dictionary.
     */
    public PDActionJavaScript(COSDictionary a)
    {
        super(a);
    }

    /**
     * @param sAction The JavaScript.
     */
    public void setAction(String sAction)
    {
        action.setString("JS", sAction);
    }

    /**
     * @return The Javascript Code.
     */
    public String getAction()
    {
        COSBase base = action.getDictionaryObject(COSName.JS);
        if (base instanceof COSString)
        {
            return ((COSString) base).getString();
        }
        if (base instanceof COSStream)
        {
            return ((COSStream) base).asTextString();
        }
        return null;
    }
}
