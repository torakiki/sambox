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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.junit.Test;

public class PDDeviceCMYKTest
{

    @Test
    public void testCMYK() throws IOException
    {
        assertArrayEquals(new float[] { 0.73669034f, 0.594995f, 0.32845044f },
                PDDeviceCMYK.INSTANCE.toRGB(new float[] { 0.25f, 0.35f, 0.7f, 0.11f }), 0.01f);
        PDDeviceCMYK.INSTANCE = PDDeviceCMYK.eagerInstance(
                PDDeviceCMYKTest.class.getResourceAsStream(
                        "/org/sejda/sambox/resources/icc/CGATS001Compat-v2-micro.icc"));
        assertArrayEquals(new float[] { 0.692668f, 0.57610434f, 0.36299688f },
                PDDeviceCMYK.INSTANCE.toRGB(new float[] { 0.25f, 0.35f, 0.7f, 0.11f }), 0.01f);
    }

    @Test
    public void nullProfile() throws IOException
    {
        assertThrows(IllegalArgumentException.class, () -> PDDeviceCMYK.eagerInstance(null));
    }
}
