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
package org.sejda.sambox.cos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.sambox.util.Hex;

/**
 * A string object, which may be a text string, a PDFDocEncoded string, ASCII string, or byte string.
 *
 * <p>
 * Text strings are used for character strings that contain information intended to be human-readable, such as text
 * annotations, bookmark names, article names, document information, and so forth.
 *
 * <p>
 * PDFDocEncoded strings are used for characters that are represented in a single byte.
 *
 * <p>
 * ASCII strings are used for characters that are represented in a single byte using ASCII encoding.
 *
 * <p>
 * Byte strings are used for binary data represented as a series of bytes, but the encoding is not known. The bytes of
 * the string need not represent characters.
 * 
 * @author Ben Litchfield
 * @author John Hewson
 */
public final class COSString extends COSBase implements Encryptable
{
    private byte[] bytes;
    private boolean forceHexForm;
    private boolean encryptable = true;

    /**
     * Creates a new PDF string from a byte array. This method can be used to read a string from an existing PDF file,
     * or to create a new byte string.
     *
     * @param bytes The raw bytes of the PDF text string or byte string.
     */
    public COSString(byte[] bytes)
    {
        this.bytes = bytes;
    }

    /**
     * Sets the raw value of this string.
     *
     * @param value The raw bytes of the PDF text string or byte string.
     */
    public void setValue(byte[] value)
    {
        this.bytes = Arrays.copyOf(value, value.length);
    }

    /**
     * Sets whether or not to force the string is to be written in hex form. This is needed when signing PDF files.
     *
     * @param value True to force hex.
     */
    public void setForceHexForm(boolean value)
    {
        this.forceHexForm = value;
    }

    /**
     * @return true if the string is to be written in hex form.
     */
    public boolean isForceHexForm()
    {
        return forceHexForm || !isAscii();
    }

    private boolean isAscii()
    {
        for (byte b : bytes)
        {
            // if the byte is negative then it is an eight bit byte and is outside the ASCII range
            // PDFBOX-3107 EOL markers within a string are troublesome
            if (b < 0 || b == 0x0d || b == 0x0a)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the content PDF text string as defined in Chap 7.9 of PDF 32000-1:2008.
     */
    public String getString()
    {
        // text string - BOM indicates Unicode
        if (bytes.length >= 2)
        {
            if ((bytes[0] & 0xff) == 0xFE && (bytes[1] & 0xff) == 0xFF)
            {
                // UTF-16BE
                return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
            }
            else if ((bytes[0] & 0xff) == 0xFF && (bytes[1] & 0xff) == 0xFE)
            {
                // UTF-16LE - not in the PDF spec!
                return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
            }
        }

        // otherwise use PDFDocEncoding
        return PDFDocEncoding.toString(bytes);
    }

    /**
     * @return the raw bytes of the string. Best used with a PDF <i>byte string</i>.
     */
    public byte[] getBytes()
    {
        return bytes;
    }

    /**
     * @return A hex string representing the bytes in this string.
     */
    public String toHexString()
    {
        return Hex.getString(bytes);
    }

    @Override
    public boolean encryptable()
    {
        return encryptable;
    }

    @Override
    public void encryptable(boolean encryptable)
    {
        this.encryptable = encryptable;
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof COSString)
        {
            COSString strObj = (COSString) obj;
            return getString().equals(strObj.getString()) && forceHexForm == strObj.forceHexForm;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(bytes) + (forceHexForm ? 17 : 0);
    }

    @Override
    public String toString()
    {
        return "COSString{" + getString() + "}";
    }

    /**
     * Factory method for a {@link COSString} from a byte array
     * 
     * @param value
     * @return a new instance
     */
    public static COSString newInstance(byte[] value)
    {
        return new COSString(value);
    }

    /**
     * Factory method creating a {@link COSString} from a literal string.
     *
     * @param literal A literal string.
     * @return A {@link COSString} encoded with {@link PDFDocEncoding} encoding if possible, with
     * {@link Charsets#UTF_16BE} otherwise.
     * @throws IOException If there is an error with the hex string.
     */
    public static COSString parseLiteral(String literal)
    {
        return Optional.ofNullable(PDFDocEncoding.getBytes(literal)).map(COSString::new)
                .orElseGet(() -> {
                    byte[] data = literal.getBytes(StandardCharsets.UTF_16BE);
                    byte[] bytes = new byte[data.length + 2];
                    bytes[0] = (byte) 0xFE;
                    bytes[1] = (byte) 0xFF;
                    System.arraycopy(data, 0, bytes, 2, data.length);
                    return new COSString(bytes);
                });
    }

    /**
     * Factory method creating a {@link COSString} from a string of hex characters.
     *
     * @param hex A hex string.
     * @return A cos string with the hex characters converted to their actual bytes.
     * @throws IOException If there is an error with the hex string.
     */
    public static COSString parseHex(String hex) throws IOException
    {

        StringBuilder hexBuffer = new StringBuilder(hex.trim());

        // if odd number then the last hex digit is assumed to be 0
        if (hexBuffer.length() % 2 != 0)
        {
            hexBuffer.append('0');
        }

        try (FastByteArrayOutputStream bytes = new FastByteArrayOutputStream())
        {
            for (int i = 0; i < hexBuffer.length(); i += 2)
            {
                try
                {
                    bytes.write(Integer.parseInt(hexBuffer.substring(i, i + 2), 16));
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Invalid hex string: " + hex, e);
                }
            }
            COSString retVal = new COSString(bytes.toByteArray());
            retVal.setForceHexForm(true);
            return retVal;
        }
    }

}
