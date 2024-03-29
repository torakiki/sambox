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
package org.sejda.sambox.rendering;

import java.awt.image.BufferedImage;

/**
 * Image type for rendering.
 */
public enum ImageType
{
    /**
     * Black or white.
     */
    BINARY
            {
                @Override
                public int toBufferedImageType()
                {
                    return BufferedImage.TYPE_BYTE_BINARY;
                }
            },

    /**
     * Shades of gray
     */
    GRAY
            {
                @Override
                public int toBufferedImageType()
                {
                    return BufferedImage.TYPE_BYTE_GRAY;
                }
            },

    /**
     * Red, Green, Blue
     */
    RGB
            {
                @Override
                public int toBufferedImageType()
                {
                    return BufferedImage.TYPE_INT_RGB;
                }
            },

    /**
     * Alpha, Red, Green, Blue
     */
    ARGB
            {
                @Override
                public int toBufferedImageType()
                {
                    return BufferedImage.TYPE_INT_ARGB;
                }

            },
    /**
     * Blue, Green, Red
     */
    BGR
            {
                @Override
                public int toBufferedImageType()
                {
                    return BufferedImage.TYPE_3BYTE_BGR;
                }
            };

    public abstract int toBufferedImageType();
}
