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
package org.sejda.sambox.encryption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sejda.sambox.encryption.EncryptUtils.padOrTruncate;
import static org.sejda.sambox.encryption.EncryptUtils.rnd;
import static org.sejda.sambox.encryption.EncryptUtils.truncate127;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class EncryptUtilsTest
{

    @Test
    public void rndTest()
    {
        byte[] val = rnd(10);
        assertNotNull(val);
        assertEquals(10, val.length);
    }

    @Test
    public void truncate127Test()
    {
        assertEquals(10, truncate127(rnd(10)).length);
        assertEquals(127, truncate127(rnd(200)).length);
    }

    @Test
    public void padOrTruncateTest()
    {
        assertEquals(32, padOrTruncate(rnd(10)).length);
        assertEquals(32, padOrTruncate(rnd(200)).length);
        byte[] val = padOrTruncate(rnd(30));
        assertEquals((byte) 0x28, val[30]);
        assertEquals((byte) 0xBF, val[31]);
    }

}
