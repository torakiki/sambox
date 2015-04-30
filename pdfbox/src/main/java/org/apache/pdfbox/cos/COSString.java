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
package org.apache.pdfbox.cos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.util.Hex;

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
public final class COSString extends COSBase
{
    private byte[] bytes;
    private boolean forceHexForm;

    /**
     * Creates a new PDF string from a byte array. This method can be used to read a string from an existing PDF file,
     * or to create a new byte string.
     *
     * @param bytes The raw bytes of the PDF text string or byte string.
     */
    private COSString(byte[] bytes)
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
        return forceHexForm;
    }

    /**
     * @return the content of this string as a PDF <i>text string</i>.
     */
    public String getString()
    {
        // text string - BOM indicates Unicode
        if (bytes.length > 2)
        {
            if ((bytes[0] & 0xff) == 0xFE && (bytes[1] & 0xff) == 0xFF)
            {
                // UTF-16BE
                return new String(bytes, 2, bytes.length - 2, Charsets.UTF_16BE);
            }
            else if ((bytes[0] & 0xff) == 0xFF && (bytes[1] & 0xff) == 0xFE)
            {
                // UTF-16LE - not in the PDF spec!
                return new String(bytes, 2, bytes.length - 2, Charsets.UTF_16LE);
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
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            sb.append(Hex.getString(b));
        }
        return sb.toString();
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
        return new COSString(Arrays.copyOf(value, value.length));
    }

    /**
     * Factory method creating a {@link COSString} from a literal string.
     *
     * @param literal A literal string.
     * @return A {@link COSString} with the hex characters converted to their actual bytes.
     * @throws IOException If there is an error with the hex string.
     */
    public static COSString parseLiteral(String literal)
    {
        // check whether the string uses only characters available in PDFDocEncoding
        for (char c : literal.toCharArray())
        {
            if (!PDFDocEncoding.containsChar(c))
            {
                byte[] data = literal.getBytes(Charsets.UTF_16BE);
                ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 2);
                out.write(0xFE); // BOM
                out.write(0xFF); // BOM
                try
                {
                    out.write(data);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                return new COSString(out.toByteArray());
            }
        }
        return new COSString(PDFDocEncoding.getBytes(literal));
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
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringBuilder hexBuffer = new StringBuilder(hex.trim());

        // if odd number then the last hex digit is assumed to be 0
        if (hexBuffer.length() % 2 != 0)
        {
            hexBuffer.append('0');
        }

        int length = hexBuffer.length();
        for (int i = 0; i < length; i += 2)
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
