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
package org.sejda.sambox.pdmodel.common.filespecification;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Calendar;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.common.PDStream;

/**
 * This represents an embedded file in a file specification.
 *
 * @author Ben Litchfield
 */
public class PDEmbeddedFile extends PDStream
{

    public PDEmbeddedFile(COSStream str)
    {
        super(str);
    }

    public PDEmbeddedFile(InputStream str) throws IOException
    {
        super(str);
        getCOSObject().setItem(COSName.TYPE, COSName.EMBEDDED_FILE);
    }

    public PDEmbeddedFile(InputStream input, COSName filter) throws IOException
    {
        super(input, filter);
        getCOSObject().setItem(COSName.TYPE, COSName.EMBEDDED_FILE);
    }

    /**
     * Set the subtype for this embedded file. This should be a mime type value. Optional.
     *
     * @param mimeType The mimeType for the file.
     */
    public void setSubtype(String mimeType)
    {
        getCOSObject().setName(COSName.SUBTYPE, mimeType);
    }

    /**
     * @return the subtype(mimetype) for the embedded file.
     */
    public String getSubtype()
    {
        return getCOSObject().getNameAsString(COSName.SUBTYPE);
    }

    /**
     * @return The size of the embedded file.
     */
    public int getSize()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.PARAMS, COSDictionary.class)).map(
                d -> d.getInt(COSName.SIZE, -1)).orElse(-1);
    }

    public void setSize(long size)
    {
        getCOSObject().computeIfAbsent(COSName.PARAMS, _ -> new COSDictionary(),
                COSDictionary.class).setLong(COSName.SIZE, size);
    }

    /**
     * @return The Creation date of the embedded file.
     */
    public Calendar getCreationDate()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.PARAMS, COSDictionary.class)).map(
                d -> d.getDate(COSName.CREATION_DATE)).orElse(null);
    }

    public void setCreationDate(Instant creation)
    {
        getCOSObject().computeIfAbsent(COSName.PARAMS, _ -> new COSDictionary(),
                COSDictionary.class).setDate(COSName.CREATION_DATE, creation);
    }

    /**
     * @return The ModDate of the embedded file.
     */
    public Calendar getModDate()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.PARAMS, COSDictionary.class)).map(
                d -> d.getDate(COSName.MOD_DATE)).orElse(null);
    }

    public void setModDate(Instant modified)
    {
        getCOSObject().computeIfAbsent(COSName.MOD_DATE, _ -> new COSDictionary(),
                COSDictionary.class).setDate(COSName.CREATION_DATE, modified);
    }

    public String getCheckSum()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.PARAMS, COSDictionary.class)).map(
                d -> d.getString("CheckSum")).orElse(null);
    }

    public void setCheckSum(String checksum)
    {
        getCOSObject().computeIfAbsent(COSName.PARAMS, _ -> new COSDictionary(),
                COSDictionary.class).setString("CheckSum", checksum);
    }

    @Deprecated
    //Mac dictionary is deprecated in PDF 2.0
    public String getMacSubtype()
    {
        var params = getCOSObject().getDictionaryObject(COSName.PARAMS, COSDictionary.class);
        if (params != null)
        {
            return ofNullable(getCOSObject().getDictionaryObject("Mac", COSDictionary.class)).map(
                    d -> d.getString("Subtype")).orElse(null);
        }
        return null;
    }

    @Deprecated
    //Mac dictionary is deprecated in PDF 2.0
    public String getMacCreator()
    {
        var params = getCOSObject().getDictionaryObject(COSName.PARAMS, COSDictionary.class);
        if (params != null)
        {
            return ofNullable(getCOSObject().getDictionaryObject("Mac", COSDictionary.class)).map(
                    d -> d.getString("Creator")).orElse(null);
        }
        return null;
    }

    @Deprecated
    //Mac dictionary is deprecated in PDF 2.0
    public String getMacResFork()
    {
        var params = getCOSObject().getDictionaryObject(COSName.PARAMS, COSDictionary.class);
        if (params != null)
        {
            return ofNullable(getCOSObject().getDictionaryObject("Mac", COSDictionary.class)).map(
                    d -> d.getString("ResFork")).orElse(null);
        }
        return null;
    }

}
