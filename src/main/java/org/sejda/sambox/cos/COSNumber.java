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
package org.sejda.sambox.cos;

import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * This class represents an abstract number in a PDF document.
 *
 * @author Ben Litchfield
 */
public abstract class COSNumber extends COSBase
{

    /**
     * @return The float value of this object.
     */
    public abstract float floatValue();

    /**
     * @return The double value of this number.
     */
    public abstract double doubleValue();

    /**
     * @return The integer value of this number.
     */
    public abstract int intValue();

    /**
     * @return The long value of this number.
     */
    public abstract long longValue();

    private static Pattern NUMBER = Pattern.compile("(E|e|\\+|\\-|\\.|\\d)+");

    /**
     * This factory method will get the appropriate number object.
     *
     * @param number The string representation of the number.
     * @return A number object, either float or int.
     * @throws IOException If the string is not a number.
     */
    public static COSNumber get(String number) throws IOException
    {
        requireNotNullArg(number, "Number cannot be null");
        requireArg(NUMBER.matcher(number).matches(), "Invalid number " + number);
        if (number.length() == 1)
        {
            char digit = number.charAt(0);
            if ('0' <= digit && digit <= '9')
            {
                return COSInteger.get(digit - '0');
            }
            // PDFBOX-592
            return COSInteger.ZERO;
        }
        else if (number.indexOf('.') == -1 && (number.toLowerCase().indexOf('e') == -1))
        {
            try
            {
                return COSInteger.get(Long.parseLong(number));
            }
            catch (NumberFormatException e)
            {
                // PDFBOX-3589 --242
                return COSInteger.ZERO;
            }
        }

        return new COSFloat(number);
    }

    @Override
    public void idIfAbsent(IndirectCOSObjectIdentifier id)
    {
        // we don't store id for numbers. We write them as direct objects. Wrap this with an IndirectCOSObject if you
        // want to write a number as indirect reference
    }
}
