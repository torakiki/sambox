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
package org.sejda.sambox.pdmodel.graphics.color;

import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.common.PDStream;

/**
 * An Output Intent describes the colour reproduction characteristics of a possible output device or
 * production condition. Output intents provide a means for matching the colour characteristics of a
 * PDF document with those of a target output device or production environment in which the document
 * will be printed.
 *
 * @author Guillaume Bailleul
 */
public final class PDOutputIntent extends PDDictionaryWrapper
{

    public PDOutputIntent(InputStream colorProfile) throws IOException
    {
        super();
        getCOSObject().setItem(COSName.TYPE, COSName.OUTPUT_INTENT);
        getCOSObject().setItem(COSName.S, COSName.GTS_PDFA1);
        getCOSObject().setItem(COSName.DEST_OUTPUT_PROFILE, configureOutputProfile(colorProfile));
    }

    public PDOutputIntent(COSDictionary dictionary)
    {
        super(dictionary);
    }

    public COSStream getDestOutputIntent()
    {
        return getCOSObject().getDictionaryObject(COSName.DEST_OUTPUT_PROFILE, COSStream.class);
    }

    public String getInfo()
    {
        return getCOSObject().getString(COSName.INFO);
    }

    public void setInfo(String value)
    {
        getCOSObject().setString(COSName.INFO, value);
    }

    public String getOutputCondition()
    {
        return getCOSObject().getString(COSName.OUTPUT_CONDITION);
    }

    public void setOutputCondition(String value)
    {
        getCOSObject().setString(COSName.OUTPUT_CONDITION, value);
    }

    public String getOutputConditionIdentifier()
    {
        return getCOSObject().getString(COSName.OUTPUT_CONDITION_IDENTIFIER);
    }

    public void setOutputConditionIdentifier(String value)
    {
        getCOSObject().setString(COSName.OUTPUT_CONDITION_IDENTIFIER, value);
    }

    public String getRegistryName()
    {
        return getCOSObject().getString(COSName.REGISTRY_NAME);
    }

    public void setRegistryName(String value)
    {
        getCOSObject().setString(COSName.REGISTRY_NAME, value);
    }

    private PDStream configureOutputProfile(InputStream colorProfile) throws IOException
    {
        ICC_Profile icc = ICC_Profile.getInstance(colorProfile);
        PDStream stream = new PDStream(new ByteArrayInputStream(icc.getData()),
                COSName.FLATE_DECODE);
        stream.getCOSObject().setInt(COSName.N, icc.getNumComponents());
        return stream;
    }
}
