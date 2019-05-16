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

package org.sejda.sambox.pdmodel.encryption;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.sambox.cos.DirectCOSObject.asDirectObject;

import java.io.IOException;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

/**
 * This class is a specialized view of the encryption getCOSObject() of a PDF document. It contains a low level
 * getCOSObject() (COSDictionary) and provides the methods to manage its fields.
 *
 * The available fields are the ones who are involved by standard security handler and public key security handler.
 *
 * @author Ben Litchfield
 * @author Benoit Guillon
 */
public class PDEncryption extends PDDictionaryWrapper
{
    /**
     * See PDF Reference 1.4 Table 3.13.
     */
    public static final int VERSION0_UNDOCUMENTED_UNSUPPORTED = 0;
    /**
     * See PDF Reference 1.4 Table 3.13.
     */
    public static final int VERSION1_40_BIT_ALGORITHM = 1;
    /**
     * See PDF Reference 1.4 Table 3.13.
     */
    public static final int VERSION2_VARIABLE_LENGTH_ALGORITHM = 2;
    /**
     * See PDF Reference 1.4 Table 3.13.
     */
    public static final int VERSION3_UNPUBLISHED_ALGORITHM = 3;
    /**
     * See PDF Reference 1.4 Table 3.13.
     */
    public static final int VERSION4_SECURITY_HANDLER = 4;

    /**
     * The default length for the encryption key.
     */
    public static final int DEFAULT_LENGTH = 40;

    /**
     * The default version, according to the PDF Reference.
     */
    public static final int DEFAULT_VERSION = VERSION0_UNDOCUMENTED_UNSUPPORTED;

    private SecurityHandler securityHandler;

    /**
     * creates a new empty encryption getCOSObject().
     */
    public PDEncryption()
    {
        super();
    }

    /**
     * creates a new encryption getCOSObject() from the low level getCOSObject() provided.
     * 
     * @param getCOSObject() a COS encryption getCOSObject()
     */
    public PDEncryption(COSDictionary dictionary)
    {
        super(dictionary);
        securityHandler = SecurityHandlerFactory.INSTANCE.newSecurityHandlerForFilter(getFilter());
    }

    /**
     * Returns the security handler specified in the getCOSObject()'s Filter entry.
     * 
     * @return a security handler instance
     * @throws IOException if there is no security handler available which matches the Filter
     */
    public SecurityHandler getSecurityHandler() throws IOException
    {
        if (securityHandler == null)
        {
            throw new IOException("No security handler for filter " + getFilter());
        }
        return securityHandler;
    }

    /**
     * Sets the security handler used in this encryption getCOSObject()
     * 
     * @param securityHandler new security handler
     */
    public void setSecurityHandler(SecurityHandler securityHandler)
    {
        this.securityHandler = securityHandler;
        // TODO set Filter (currently this is done by the security handlers)
    }

    /**
     * Returns true if the security handler specified in the getCOSObject()'s Filter is available.
     * 
     * @return true if the security handler is available
     */
    public boolean hasSecurityHandler()
    {
        return securityHandler == null;
    }

    /**
     * This will get the getCOSObject() associated with this encryption getCOSObject().
     *
     * @return The COS getCOSObject() that this object wraps.
     */
    @Deprecated
    public COSDictionary getCOSDictionary()
    {
        return getCOSObject();
    }

    /**
     * Sets the filter entry of the encryption getCOSObject().
     *
     * @param filter The filter name.
     */
    public void setFilter(String filter)
    {
        getCOSObject().setItem(COSName.FILTER, COSName.getPDFName(filter));
    }

    /**
     * Get the name of the filter.
     *
     * @return The filter name contained in this encryption getCOSObject().
     */
    public final String getFilter()
    {
        return getCOSObject().getNameAsString(COSName.FILTER);
    }

    /**
     * Get the name of the subfilter.
     *
     * @return The subfilter name contained in this encryption getCOSObject().
     */
    public String getSubFilter()
    {
        return getCOSObject().getNameAsString(COSName.SUB_FILTER);
    }

    /**
     * Set the subfilter entry of the encryption getCOSObject().
     *
     * @param subfilter The value of the subfilter field.
     */
    public void setSubFilter(String subfilter)
    {
        getCOSObject().setName(COSName.SUB_FILTER, subfilter);
    }

    /**
     * This will set the V entry of the encryption getCOSObject().<br />
     * <br />
     * See PDF Reference 1.4 Table 3.13. <br />
     * <br/>
     * <b>Note: This value is used to decrypt the pdf document. If you change this when the document is encrypted then
     * decryption will fail!.</b>
     *
     * @param version The new encryption version.
     */
    public void setVersion(int version)
    {
        getCOSObject().setInt(COSName.V, version);
    }

    /**
     * This will return the V entry of the encryption getCOSObject().<br />
     * <br />
     * See PDF Reference 1.4 Table 3.13.
     *
     * @return The encryption version to use.
     */
    public int getVersion()
    {
        return getCOSObject().getInt(COSName.V, 0);
    }

    /**
     * This will set the number of bits to use for the encryption algorithm.
     *
     * @param length The new key length.
     */
    public void setLength(int length)
    {
        getCOSObject().setInt(COSName.LENGTH, length);
    }

    /**
     * This will return the Length entry of the encryption getCOSObject().<br />
     * <br />
     * The length in <b>bits</b> for the encryption algorithm. This will return a multiple of 8.
     *
     * @return The length in bits for the encryption algorithm
     */
    public int getLength()
    {
        return getCOSObject().getInt(COSName.LENGTH, 40);
    }

    /**
     * This will set the R entry of the encryption getCOSObject().<br />
     * <br />
     * See PDF Reference 1.4 Table 3.14. <br />
     * <br/>
     *
     * <b>Note: This value is used to decrypt the pdf document. If you change this when the document is encrypted then
     * decryption will fail!.</b>
     *
     * @param revision The new encryption version.
     */
    public void setRevision(int revision)
    {
        getCOSObject().setInt(COSName.R, revision);
    }

    /**
     * This will return the R entry of the encryption getCOSObject().<br />
     * <br />
     * See PDF Reference 1.4 Table 3.14.
     *
     * @return The encryption revision to use.
     */
    public int getRevision()
    {
        return getCOSObject().getInt(COSName.R, DEFAULT_VERSION);
    }

    /**
     * This will set the O entry in the standard encryption getCOSObject().
     *
     * @param o A 32 byte array or null if there is no owner key.
     *
     * @throws IOException If there is an error setting the data.
     */
    public void setOwnerKey(byte[] o)
    {
        getCOSObject().setItem(COSName.O, COSString.newInstance(o));
    }

    /**
     * This will get the O entry in the standard encryption getCOSObject().
     *
     * @return A 32 byte array or null if there is no owner key.
     *
     * @throws IOException If there is an error accessing the data.
     */
    public byte[] getOwnerKey()
    {
        byte[] o = null;
        COSString owner = (COSString) getCOSObject().getDictionaryObject(COSName.O);
        if (owner != null)
        {
            o = owner.getBytes();
        }
        return o;
    }

    /**
     * This will set the U entry in the standard encryption getCOSObject().
     *
     * @param u A 32 byte array.
     *
     * @throws IOException If there is an error setting the data.
     */
    public void setUserKey(byte[] u)
    {
        getCOSObject().setItem(COSName.U, COSString.newInstance(u));
    }

    /**
     * This will get the U entry in the standard encryption getCOSObject().
     *
     * @return A 32 byte array or null if there is no user key.
     *
     * @throws IOException If there is an error accessing the data.
     */
    public byte[] getUserKey()
    {
        byte[] u = null;
        COSString user = (COSString) getCOSObject().getDictionaryObject(COSName.U);
        if (user != null)
        {
            u = user.getBytes();
        }
        return u;
    }

    /**
     * This will set the OE entry in the standard encryption getCOSObject().
     *
     * @param oe A 32 byte array or null if there is no owner encryption key.
     *
     * @throws IOException If there is an error setting the data.
     */
    public void setOwnerEncryptionKey(byte[] oe)
    {
        getCOSObject().setItem(COSName.OE, COSString.newInstance(oe));
    }

    /**
     * This will get the OE entry in the standard encryption getCOSObject().
     *
     * @return A 32 byte array or null if there is no owner encryption key.
     *
     * @throws IOException If there is an error accessing the data.
     */
    public byte[] getOwnerEncryptionKey()
    {
        byte[] oe = null;
        COSString ownerEncryptionKey = (COSString) getCOSObject().getDictionaryObject(COSName.OE);
        if (ownerEncryptionKey != null)
        {
            oe = ownerEncryptionKey.getBytes();
        }
        return oe;
    }

    /**
     * This will set the UE entry in the standard encryption getCOSObject().
     *
     * @param ue A 32 byte array or null if there is no user encryption key.
     *
     * @throws IOException If there is an error setting the data.
     */
    public void setUserEncryptionKey(byte[] ue)
    {
        getCOSObject().setItem(COSName.UE, COSString.newInstance(ue));
    }

    /**
     * This will get the UE entry in the standard encryption getCOSObject().
     *
     * @return A 32 byte array or null if there is no user encryption key.
     *
     * @throws IOException If there is an error accessing the data.
     */
    public byte[] getUserEncryptionKey()
    {
        byte[] ue = null;
        COSString userEncryptionKey = (COSString) getCOSObject().getDictionaryObject(COSName.UE);
        if (userEncryptionKey != null)
        {
            ue = userEncryptionKey.getBytes();
        }
        return ue;
    }

    /**
     * This will set the permissions bit mask.
     *
     * @param permissions The new permissions bit mask
     */
    public void setPermissions(int permissions)
    {
        getCOSObject().setInt(COSName.P, permissions);
    }

    /**
     * This will get the permissions bit mask.
     *
     * @return The permissions bit mask.
     */
    public int getPermissions()
    {
        return getCOSObject().getInt(COSName.P, 0);
    }

    /**
     * Will get the EncryptMetaData getCOSObject() info.
     * 
     * @return true if EncryptMetaData is explicitly set to false (the default is true)
     */
    public boolean isEncryptMetaData()
    {
        // default is true (see 7.6.3.2 Standard Encryption Dictionary PDF 32000-1:2008)
        boolean encryptMetaData = true;

        COSBase value = getCOSObject().getDictionaryObject(COSName.ENCRYPT_META_DATA);

        if (value instanceof COSBoolean)
        {
            encryptMetaData = ((COSBoolean) value).getValue();
        }

        return encryptMetaData;
    }

    /**
     * This will set the Recipients field of the getCOSObject(). This field contains an array of string.
     * 
     * @param recipients the array of bytes arrays to put in the Recipients field.
     * @throws IOException If there is an error setting the data.
     */
    public void setRecipients(byte[][] recipients)
    {
        COSArray array = new COSArray();
        for (byte[] recipient : recipients)
        {
            array.add(COSString.newInstance(recipient));
        }
        getCOSObject().setItem(COSName.RECIPIENTS, asDirectObject(array));
    }

    /**
     * Returns the number of recipients contained in the Recipients field of the getCOSObject().
     *
     * @return the number of recipients contained in the Recipients field.
     */
    public int getRecipientsLength()
    {
        COSArray array = (COSArray) getCOSObject().getItem(COSName.RECIPIENTS);
        return array.size();
    }

    /**
     * returns the COSString contained in the Recipients field at position i.
     *
     * @param i the position in the Recipients field array.
     *
     * @return a COSString object containing information about the recipient number i.
     */
    public COSString getRecipientStringAt(int i)
    {
        COSArray array = (COSArray) getCOSObject().getItem(COSName.RECIPIENTS);
        return (COSString) array.get(i);
    }

    /**
     * 
     * @return the standard crypt filter if available.
     */
    public PDCryptFilterDictionary getStdCryptFilterDictionary()
    {
        return getCryptFilterDictionary(COSName.STD_CF);
    }

    /**
     * 
     * @return the default crypt filter if available.
     */
    public PDCryptFilterDictionary getDefaultCryptFilterDictionary()
    {
        return getCryptFilterDictionary(COSName.DEFAULT_CRYPT_FILTER);
    }

    /**
     * Returns the crypt filter with the given name.
     * 
     * @param cryptFilterName the name of the crypt filter
     * 
     * @return the crypt filter with the given name if available
     */
    public PDCryptFilterDictionary getCryptFilterDictionary(COSName cryptFilterName)
    {
        COSDictionary cryptFilterDictionary = getCOSObject().getDictionaryObject(COSName.CF,
                COSDictionary.class);
        if (cryptFilterDictionary != null)
        {
            COSDictionary stdCryptFilterDictionary = cryptFilterDictionary
                    .getDictionaryObject(cryptFilterName, COSDictionary.class);
            if (stdCryptFilterDictionary != null)
            {
                return new PDCryptFilterDictionary(stdCryptFilterDictionary);
            }
        }
        return null;
    }

    /**
     * Sets the crypt filter with the given name.
     * 
     * @param cryptFilterName the name of the crypt filter
     * @param cryptFilterDictionary the crypt filter to set
     */
    public void setCryptFilterDictionary(COSName cryptFilterName,
            PDCryptFilterDictionary cryptFilterDictionary)
    {
        COSDictionary cfDictionary = ofNullable(
                getCOSObject().getDictionaryObject(COSName.CF, COSDictionary.class))
                        .orElseGet(COSDictionary::new);
        getCOSObject().setItem(COSName.CF, asDirectObject(cfDictionary));

        cfDictionary.setItem(cryptFilterName, asDirectObject(cryptFilterDictionary.getCOSObject()));
    }

    /**
     * Sets the standard crypt filter.
     * 
     * @param cryptFilterDictionary the standard crypt filter to set
     */
    public void setStdCryptFilterDictionary(PDCryptFilterDictionary cryptFilterDictionary)
    {
        setCryptFilterDictionary(COSName.STD_CF, cryptFilterDictionary);
    }

    /**
     * Sets the default crypt filter (for public-key security handler).
     * 
     * @param defaultFilterDictionary the standard crypt filter to set
     */
    public void setDefaultCryptFilterDictionary(PDCryptFilterDictionary defaultFilterDictionary)
    {
        setCryptFilterDictionary(COSName.DEFAULT_CRYPT_FILTER, defaultFilterDictionary);
    }

    /**
     * Returns the name of the filter which is used for de/encrypting streams. Default value is "Identity".
     * 
     * @return the name of the filter
     */
    public COSName getStreamFilterName()
    {
        COSName stmF = (COSName) getCOSObject().getDictionaryObject(COSName.STM_F);
        if (stmF == null)
        {
            stmF = COSName.IDENTITY;
        }
        return stmF;
    }

    /**
     * Sets the name of the filter which is used for de/encrypting streams.
     * 
     * @param streamFilterName the name of the filter
     */
    public void setStreamFilterName(COSName streamFilterName)
    {
        getCOSObject().setItem(COSName.STM_F, streamFilterName);
    }

    /**
     * Returns the name of the filter which is used for de/encrypting strings. Default value is "Identity".
     * 
     * @return the name of the filter
     */
    public COSName getStringFilterName()
    {
        COSName strF = (COSName) getCOSObject().getDictionaryObject(COSName.STR_F);
        if (strF == null)
        {
            strF = COSName.IDENTITY;
        }
        return strF;
    }

    /**
     * Sets the name of the filter which is used for de/encrypting strings.
     * 
     * @param stringFilterName the name of the filter
     */
    public void setStringFilterName(COSName stringFilterName)
    {
        getCOSObject().setItem(COSName.STR_F, stringFilterName);
    }

    /**
     * Set the Perms entry in the encryption getCOSObject().
     *
     * @param perms A 16 byte array.
     *
     * @throws IOException If there is an error setting the data.
     */
    public void setPerms(byte[] perms)
    {
        getCOSObject().setItem(COSName.PERMS, COSString.newInstance(perms));
    }

    /**
     * Get the Perms entry in the encryption getCOSObject().
     *
     * @return A 16 byte array or null if there is no Perms entry.
     *
     * @throws IOException If there is an error accessing the data.
     */
    public byte[] getPerms()
    {
        COSString permsCosString = getCOSObject().getDictionaryObject(COSName.PERMS,
                COSString.class);
        if (nonNull(permsCosString))
        {
            return permsCosString.getBytes();
        }
        return null;
    }

    /**
     * remove CF, StmF, and StrF entries. This is to be called if V is not 4 or 5.
     */
    public void removeV45filters()
    {
        getCOSObject().setItem(COSName.CF, null);
        getCOSObject().setItem(COSName.STM_F, null);
        getCOSObject().setItem(COSName.STR_F, null);
    }
}
