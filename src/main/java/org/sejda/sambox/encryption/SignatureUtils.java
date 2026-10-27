/*
 * Created on 27/10/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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
package org.sejda.sambox.encryption;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;

/**
 * Utilities for signature. This is used both when decrypting and when encrypting.
 *
 * @author Andrea Vacondio
 */
public final class SignatureUtils
{
    private SignatureUtils()
    {
        // utility
    }

    /**
     * @return true if the given dictionary has /Type /Sig or /DocTimeStamp, or it has a direct
     * /Contents string and a /ByteRange array (PDFBOX-4466, /Type is optional)
     */
    public static boolean isSignature(COSDictionary dictionary)
    {
        COSBase type = dictionary.getDictionaryObject(COSName.TYPE);
        return COSName.SIG.equals(type) || COSName.DOC_TIME_STAMP.equals(type) ||
                // PDFBOX-4466: /Type is optional, see
                // https://ec.europa.eu/cefdigital/tracker/browse/DSS-1538
                (dictionary.getItem(COSName.CONTENTS) instanceof COSString
                        && dictionary.getDictionaryObject(COSName.BYTERANGE) instanceof COSArray);
    }
}
