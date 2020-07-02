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
package org.sejda.sambox;

import org.sejda.sambox.util.Version;

/**
 * Holder for configurable system properties. This is supposed to give developers a single place where they can find out
 * what's configurable in SAMBox.
 *
 * @author Andrea Vacondio
 */
public final class SAMBox
{
    /**
     * Pool size used in org.sejda.sambox.input.SourceReader
     */
    public static final String BUFFERS_POOL_SIZE_PROPERTY = "org.sejda.sambox.buffers.pool.size";
    /**
     * The number of objects to include in a single ObjectsStream.
     */
    public static final String OBJECTS_STREAM_SIZE_PROPERTY = "org.sejda.sambox.objects.stream.size";
    public static final String FONT_PROVIDER_PROPERTY = "org.sejda.sambox.font.provider";

    public static final String SAMBOX_PROPERTIES = "/org/sejda/sambox/resources/version.properties";

    public static String PRODUCER = "SAMBox " + Version.getVersion();
}
