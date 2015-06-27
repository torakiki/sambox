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
package org.apache.pdfbox.input.source;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.pdfbox.SAMBox;
import org.apache.pdfbox.util.IOUtils;

/**
 * This class consists of solely static methods to create the most appropriate {@link SeekableSource} based on the given
 * input or to bridge {@link SeekableSource}s to the more traditional {@link InputStream} or other standard I/O classes.
 * 
 * @author Andrea Vacondio
 *
 */
public final class SeekableSources
{

    private static final long MB_16 = 1 << 24;

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
        if (file.length() > Long.getLong(SAMBox.MAPPED_SIZE_THRESHOLD_PROPERTY, MB_16))
        {
            return new BufferedSeekableSource(new MemoryMappedSeekableSource(file));
        }
        return new BufferedSeekableSource(new FileChannelSeekableSource(file));
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link InputStream}. The whole stream is read an stored
     * in a byte array with a max size of 2GB.
     * 
     * @param stream
     * @return a {@link SeekableSource} from the given stream.
     * @throws IOException
     */
    public static SeekableSource inMemorySeekableSourceFrom(InputStream stream) throws IOException
    {
        requireNonNull(stream);
        return new ByteArraySeekableSource(IOUtils.toByteArray(stream));
    }

    /**
     * Factory method to create a {@link SeekableSource} from a byte array.
     * 
     * @param bytes
     * @return a {@link SeekableSource} wrapping the given byte array.
     * @throws IOException
     */
    public static SeekableSource inMemorySeekableSourceFrom(byte[] bytes)
    {
        requireNonNull(bytes);
        return new ByteArraySeekableSource(bytes);
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link InputStream}. The whole stream is copied to a
     * temporary file.
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
        Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
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

    /**
     * Factory method to create an {@link InputStream} from a {@link SeekableSource}.
     * 
     * @param source the source
     * @return the input stream wrapping the given {@link SeekableSource}
     */
    public static InputStream inputStreamFrom(SeekableSource source)
    {
        requireNonNull(source);
        return new SeekableSourceInputStream(source);
    }
}
