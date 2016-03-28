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
package org.sejda.sambox.pdmodel.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;

/**
 * This class represents metadata for various objects in a PDF document.
 *
 * @author Ben Litchfield
 */
public class PDMetadata extends PDStream
{

    /**
     * This will create a new PDMetadata object.
     */
    public PDMetadata()
    {
        getCOSObject().setName(COSName.TYPE, "Metadata");
        getCOSObject().setName(COSName.SUBTYPE, "XML");
    }

    /**
     * Constructor.  Reads all data from the input stream and embeds it into the
     * document, this will close the InputStream.
     *
     * @param str The stream parameter.
     * @param filtered True if the stream already has a filter applied.
     * @throws IOException If there is an error creating the stream in the document.
     */
    public PDMetadata(InputStream str) throws IOException
    {
        super(str);
        getCOSObject().setName(COSName.TYPE, "Metadata");
        getCOSObject().setName(COSName.SUBTYPE, "XML");
    }

    /**
     * Constructor.
     *
     * @param str The stream parameter.
     */
    public PDMetadata( COSStream str )
    {
        super( str );
    }

    /**
     * Extract the XMP metadata.
     * To persist changes back to the PDF you must call importXMPMetadata.
     *
     * @return A stream to get the xmp data from.
     *
     * @throws IOException If there is an error parsing the XMP data.
     */
    public InputStream exportXMPMetadata() throws IOException
    {
        return createInputStream();
    }

    /**
     * Import an XMP stream into the PDF document.
     *
     * @param xmp The XMP data.
     *
     * @throws IOException If there is an error generating the XML document.
     */
    public void importXMPMetadata( byte[] xmp )
        throws IOException
    {
        try (OutputStream os = createOutputStream())
        {
            os.write(xmp);
        }
    }
}
