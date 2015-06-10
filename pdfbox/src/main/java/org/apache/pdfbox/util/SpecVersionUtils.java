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
package org.apache.pdfbox.util;

import static org.apache.pdfbox.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Andrea Vacondio
 *
 */
public class SpecVersionUtils
{
    public static final int EXPECTED_HEADER_LENGTH = 8;
    public static final String PDF_HEADER = "%PDF-";

    public static final String V1_0 = "1.0";
    public static final String V1_1 = "1.1";
    public static final String V1_2 = "1.2";
    public static final String V1_3 = "1.3";
    public static final String V1_4 = "1.4";
    public static final String V1_5 = "1.5";
    public static final String V1_6 = "1.6";
    public static final String V1_7 = "1.7";
    public static final String V2_0 = "2.0";

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)$");

    private SpecVersionUtils()
    {
        // utility
    }

    /**
     * Parses a conforming file header string or returning the version (ex. "1.4") found. File header is defined in Chap
     * 7.5.2 of PDF 32000-1:2008
     * 
     * @param header
     * @return the parsed version (ex. "1.4")
     * @throws IOException if the string is not a valid header
     */
    public static String parseHeaderString(String header) throws IOException
    {
        String version = header.substring(EXPECTED_HEADER_LENGTH - 3, EXPECTED_HEADER_LENGTH);
        if (!VERSION_PATTERN.matcher(version).matches())
        {
            throw new IOException("Unable to get header version from " + header);
        }
        return version;
    }

    /**
     * @param version the version to compare
     * @param atLeast min version to return true (ex. "1.5")
     * @return true if the given pdf spec version is at least as high as the given atLeast version
     */
    public static boolean isAtLeast(String version, String atLeast)
    {
        requireNotNullArg(version, "Cannot compare a null version");
        requireNotNullArg(atLeast, "Cannot compare a null version");
        return version.compareTo(atLeast) >= 0;
    }
}
