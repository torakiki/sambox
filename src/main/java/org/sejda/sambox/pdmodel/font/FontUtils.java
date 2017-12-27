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
package org.sejda.sambox.pdmodel.font;

import java.io.IOException;
import java.util.Map;

import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.TrueTypeFont;

/**
 * @author Andrea Vacondio
 */
public final class FontUtils
{
    private static final String BASE25 = "BCDEFGHIJKLMNOPQRSTUVWXYZ";

    private FontUtils()
    {
        // util
    }

    /**
     * @return true if the fsType in the OS/2 table permits embedding.
     */
    public static boolean isEmbeddingPermitted(TrueTypeFont ttf) throws IOException
    {
        if (ttf.getOS2Windows() != null)
        {
            int fsType = ttf.getOS2Windows().getFsType();
            int exclusive = fsType & 0x8; // bits 0-3 are a set of exclusive bits

            if ((exclusive
                    & OS2WindowsMetricsTable.FSTYPE_RESTRICTED) == OS2WindowsMetricsTable.FSTYPE_RESTRICTED)
            {
                // restricted License embedding
                return false;
            }
            else if ((exclusive
                    & OS2WindowsMetricsTable.FSTYPE_BITMAP_ONLY) == OS2WindowsMetricsTable.FSTYPE_BITMAP_ONLY)
            {
                // bitmap embedding only
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if the fsType in the OS/2 table permits subsetting.
     */
    public static boolean isSubsettingPermitted(TrueTypeFont ttf) throws IOException
    {
        if (ttf.getOS2Windows() != null)
        {
            int fsType = ttf.getOS2Windows().getFsType();
            if ((fsType
                    & OS2WindowsMetricsTable.FSTYPE_NO_SUBSETTING) == OS2WindowsMetricsTable.FSTYPE_NO_SUBSETTING)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @return an uppercase 6-character unique tag for the given subset.
     */
    public static String getTag(Map<Integer, Integer> gidToCid)
    {
        // deterministic
        long num = gidToCid.hashCode();

        // base25 encode
        StringBuilder sb = new StringBuilder();
        do
        {
            long div = num / 25;
            int mod = (int) (num % 25);
            sb.append(BASE25.charAt(mod));
            num = div;
        } while (num != 0 && sb.length() < 6);

        // pad
        while (sb.length() < 6)
        {
            sb.insert(0, 'A');
        }

        return sb.append('+').toString();
    }

    /**
     * @return an uppercase 6-character unique tag randomly created
     */
    public static String getTag()
    {
        StringBuilder sb = new StringBuilder("");
        for (int k = 0; k < 6; ++k)
        {
            sb.append((char) (Math.random() * 26 + 'A'));
        }
        return sb.append('+').toString();
    }
}
