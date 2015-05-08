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
package org.apache.pdfbox.input;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Andrea Vacondio
 *
 */
public final class SeekableSources
{
    private static final String MAPPED_SIZE_THRESHOLD_PROPERTY = "org.pdfbox.mapped.size.threshold";
    private static final long MAPPED_SIZE_THRESHOLD = Long.getLong(MAPPED_SIZE_THRESHOLD_PROPERTY,
            1 << 25);

    private SeekableSources()
    {
        // utility
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link File}. An attempt is made to return the best
     * {@link SeekableSource} implementation based on the size of the file.
     * 
     * @param file
     * @return a {@link SeekableSource} from the given file.
     * @throws IOException
     */
    public static SeekableSource seekableSourceFrom(File file) throws IOException
    {
        requireNonNull(file);
        if (file.length() > MAPPED_SIZE_THRESHOLD)
        {
            return new BufferedSeekableSource(new MemoryMappedSeekableSource(file));
        }
        return new BufferedSeekableSource(new FileChannelSeekableSource(file));
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link InputStream}. The whole stream is read an stored
     * in a byte array.
     * 
     * @param stream
     * @return a {@link SeekableSource} from the given stream.
     * @throws IOException
     */
    public static SeekableSource inMemorySeekableSourceFrom(InputStream stream) throws IOException
    {
        requireNonNull(stream);
        // TODO
        return null;
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link InputStream}. The whole stream is copied to a
     * temporary file,
     * 
     * @param stream
     * @return a {@link SeekableSource} from the given stream.
     * @throws IOException
     */
    public static SeekableSource onTempFileSeekableSourceFrom(InputStream stream)
            throws IOException
    {
        requireNonNull(stream);
        Path temp = Files.createTempFile("SAMBox", null);
        Files.copy(stream, temp);
        return new BufferedSeekableSource(new FileChannelSeekableSource(temp.toFile())
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                Files.deleteIfExists(temp);
            }
        });
    }
}
