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

import static java.util.Objects.nonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Bidi;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * see http://translationtherapy.com/understanding-how-to-work-with-bi-directionality-bidi-text/ and
 * https://docs.oracle.com/javase/tutorial/2d/text/textlayoutbidirectionaltext.html for some info related to BiDi
 * contents
 * 
 * @author Andrea Vacondio
 *
 */
public final class BidiUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(BidiUtils.class);

    private static Map<Character, Character> MIRRORING_CHAR_MAP = new HashMap<>();

    static
    {
        InputStream stream = BidiUtils.class
                .getResourceAsStream("/org/sejda/sambox/resources/text/BidiMirroring.txt");
 
        if (nonNull(stream))
        {
            parseBidiFile(stream);
        }
        else
        {
            LOG.warn("Could not find 'BidiMirroring.txt', mirroring char map will be empty");
        }

    }

    private BidiUtils()
    {
        // hide
    }

    /**
     * Handles the LTR and RTL direction of the given words. The whole implementation stands and falls with the given
     * word. If the word is a full line, the results will be the best. If the word contains of single words or
     * characters, the order of the characters in a word or words in a line may wrong, due to RTL and LTR marks and
     * characters!
     * 
     * Based on http://www.nesterovsky-bros.com/weblog/2013/07/28/VisualToLogicalConversionInJava.aspx
     * 
     * @param text The word that shall be processed
     * @return new word with the correct direction of the containing characters
     */
    public static String visualToLogical(String text)
    {
        if (!CharUtils.isBlank(text))
        {
            Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            if (!bidi.isLeftToRight())
            {
                // collect individual bidi information
                int runCount = bidi.getRunCount();
                byte[] levels = new byte[runCount];
                Integer[] runs = new Integer[runCount];

                for (int i = 0; i < runCount; i++)
                {
                    levels[i] = (byte) bidi.getRunLevel(i);
                    runs[i] = i;
                }

                // reorder individual parts based on their levels
                Bidi.reorderVisually(levels, 0, runs, 0, runCount);

                // collect the parts based on the direction within the run
                StringBuilder result = new StringBuilder();

                for (int i = 0; i < runCount; i++)
                {
                    int index = runs[i];
                    int start = bidi.getRunStart(index);
                    int end = bidi.getRunLimit(index);
                    int level = levels[index];

                    if ((level & 1) != 0)
                    {
                        while (--end >= start)
                        {
                            char character = text.charAt(end);
                            if (Character.isMirrored(text.codePointAt(end))
                                    && MIRRORING_CHAR_MAP.containsKey(character))
                            {
                                result.append(MIRRORING_CHAR_MAP.get(character));
                            }
                            else
                            {
                                result.append(character);
                            }
                        }
                    }
                    else
                    {
                        result.append(text, start, end);
                    }
                }

                return result.toString();
            }
        }
        return text;
    }

    /**
     * This method parses the bidi file provided as inputstream.
     * 
     * @param inputStream - The bidi file as inputstream
     * @throws IOException if any line could not be read by the LineNumberReader
     */
    private static void parseBidiFile(InputStream inputStream)
    {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        {
            reader.lines().map(l -> {
                int comment = l.indexOf('#'); // ignore comments
                if (comment != -1)
                {
                    return l.substring(0, comment);
                }
                return l;
            }).filter(l -> !CharUtils.isBlank(l)).filter(l -> l.length() > 1).forEach(l -> {
                String[] tokens = l.split(";");
                if (tokens.length == 2)
                {
                    MIRRORING_CHAR_MAP.put((char) Integer.parseInt(tokens[0].trim(), 16),
                            (char) Integer.parseInt(tokens[1].trim(), 16));
                }
            });
        }
        catch (IOException e)
        {
            LOG.warn("An error occurred while parsing BidiMirroring.txt", e);
        }
    }
}
