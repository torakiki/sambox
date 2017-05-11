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
package org.sejda.sambox.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BidiUtilsTest
{
    @Test
    public void testsVisualToLogical()
    {
        String text = "123 יתימאה ןחבמ";
        String actual = BidiUtils.visualToLogical(text);
        String expected = "מבחן האמיתי 123";

        assertEquals(expected, actual);

        text = "תירבע English תירבע בוש";
        actual = BidiUtils.visualToLogical(text);
        expected = "שוב עברית English עברית";

        assertEquals(expected, actual);
    }
}
