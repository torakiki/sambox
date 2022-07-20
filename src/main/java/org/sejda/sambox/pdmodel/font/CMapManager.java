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
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.fontbox.cmap.CMap;
import org.apache.fontbox.cmap.CMapParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CMap resource loader and cache.
 */
public final class CMapManager
{
    static Map<String, CMap> cMapCache = Collections.synchronizedMap(new HashMap<String, CMap>());

    private static final Logger LOG = LoggerFactory.getLogger(CMapManager.class);

    private CMapManager()
    {
    }

    /**
     * Fetches the predefined CMap from disk (or cache).
     *
     * @param cMapName CMap name
     * @return The predefined CMap, never null.
     * @throws IOException
     */
    public static CMap getPredefinedCMap(String cMapName) throws IOException
    {
        CMap cmap = cMapCache.get(cMapName);
        if (cmap != null)
        {
            return cmap;
        }

        CMap targetCmap = new CMapParser().parsePredefined(cMapName);

        // limit the cache to predefined CMaps
        cMapCache.put(targetCmap.getName(), targetCmap);
        return targetCmap;
    }

    /**
     * Parse the given CMap.
     *
     * @param cMapStream the CMap to be read
     * @return the parsed CMap
     */
    public static CMap parseCMap(InputStream cMapStream) throws IOException
    {
        if (cMapStream != null)
        {
            try
            {
                // parse CMap using strict mode
                return new CMapParser(true).parse(cMapStream);
            }
            catch (IOException e)
            {
                LOG.warn("Failed to parse CMap for font", e);
            }
        }
        return null;
    }
}
