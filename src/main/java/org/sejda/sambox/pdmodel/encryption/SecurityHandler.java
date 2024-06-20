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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A security handler as described in the PDF specifications. A security handler is responsible of
 * documents protection.
 *
 * @author Ben Litchfield
 * @author Benoit Guillon
 * @author Manuel Kasper
 */
public abstract class SecurityHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(SecurityHandler.class);

    private static final short DEFAULT_KEY_LENGTH = 40;

    // see 7.6.2, page 58, PDF 32000-1:2008
    private static final byte[] AES_SALT = { (byte) 0x73, (byte) 0x41, (byte) 0x6c, (byte) 0x54 };

    /**
     * The length of the secret key used to encrypt the document.
     */
    protected short keyLength = DEFAULT_KEY_LENGTH;

    /**
     * The encryption key that will used to encrypt / decrypt.
     */
    private byte[] encryptionKey;

    /**
     * The RC4 implementation used for cryptographic functions.
     */
    private final RC4Cipher rc4 = new RC4Cipher();

    /**
     * indicates if the Metadata have to be decrypted of not.
     */
    private boolean decryptMetadata;

    // PDFBOX-4453, PDFBOX-4477: Originally this was just a Set. This failed in rare cases
    // when a decrypted string was identical to an encrypted string.
    // Because COSString.equals() checks the contents, decryption was then skipped.
    // This solution keeps all different "equal" objects.
    // IdentityHashMap solves this problem and is also faster than a HashMap
    private final Set<COSBase> objects = Collections.newSetFromMap(new IdentityHashMap<>());

    private boolean useAES;

    /**
     * The access permission granted to the current user for the document. These permissions are
     * computed during decryption and are in read only mode.
     */
    private AccessPermission currentAccessPermission = null;

    /**
     * The stream filter name.
     */
    private COSName streamFilterName;

    /**
     * The string filter name.
     */
    private COSName stringFilterName;

    /**
     * Set wether to decrypt meta data.
     *
     * @param decryptMetadata true if meta data has to be decrypted.
     */
    protected void setDecryptMetadata(boolean decryptMetadata)
    {
        this.decryptMetadata = decryptMetadata;
    }

    /**
     * @return True if meta data has to be decrypted.
     */
    public boolean isDecryptMetadata()
    {
        return decryptMetadata;
    }

    /**
     * Set the string filter name.
     *
     * @param stringFilterName the string filter name.
     */
    protected void setStringFilterName(COSName stringFilterName)
    {
        this.stringFilterName = stringFilterName;
    }

    /**
     * Set the stream filter name.
     *
     * @param streamFilterName the stream filter name.
     */
    protected void setStreamFilterName(COSName streamFilterName)
    {
        this.streamFilterName = streamFilterName;
    }

    /**
     * Prepares everything to decrypt the document.
     *
     * @param encryption         encryption dictionary, can be retrieved via {@link
     *                           PDDocument#getEncryption()}
     * @param documentIDArray    document id which is returned via {@link org.sejda.sambox.cos.COSDocument#getDocumentID()}
     * @param decryptionMaterial Information used to decrypt the document.
     * @throws IOException If there is an error accessing data.
     */
    public abstract void prepareForDecryption(PDEncryption encryption, COSArray documentIDArray,
            DecryptionMaterial decryptionMaterial) throws IOException;

    /**
     * Encrypt or decrypt a set of data.
     *
     * @param objectNumber The data object number.
     * @param genNumber    The data generation number.
     * @param data         The data to encrypt.
     * @param output       The output to write the encrypted data to.
     * @throws IOException If there is an error reading the data.
     */
    private void decryptData(long objectNumber, long genNumber, InputStream data,
            OutputStream output) throws IOException
    {
        // Determine whether we're using Algorithm 1 (for RC4 and AES-128), or 1.A (for AES-256)
        if (useAES && encryptionKey.length == 32)
        {
            decryptDataAES256(data, output);
        }
        else
        {
            byte[] finalKey = calcFinalKey(objectNumber, genNumber);

            if (useAES)
            {
                decryptDataAESother(finalKey, data, output);
            }
            else
            {
                decryptDataRC4(finalKey, data, output);
            }
        }
        IOUtils.close(output);
    }

    /**
     * Calculate the key to be used for RC4 and AES-128.
     *
     * @param objectNumber The data object number.
     * @param genNumber    The data generation number.
     * @return the calculated key.
     */
    private byte[] calcFinalKey(long objectNumber, long genNumber)
    {
        byte[] newKey = new byte[encryptionKey.length + 5];
        System.arraycopy(encryptionKey, 0, newKey, 0, encryptionKey.length);
        // PDF 1.4 reference pg 73
        // step 1
        // we have the reference
        // step 2
        newKey[newKey.length - 5] = (byte) (objectNumber & 0xff);
        newKey[newKey.length - 4] = (byte) (objectNumber >> 8 & 0xff);
        newKey[newKey.length - 3] = (byte) (objectNumber >> 16 & 0xff);
        newKey[newKey.length - 2] = (byte) (genNumber & 0xff);
        newKey[newKey.length - 1] = (byte) (genNumber >> 8 & 0xff);
        // step 3
        MessageDigest md = MessageDigests.getMD5();
        md.update(newKey);
        if (useAES)
        {
            md.update(AES_SALT);
        }
        byte[] digestedKey = md.digest();
        // step 4
        int length = Math.min(newKey.length, 16);
        byte[] finalKey = new byte[length];
        System.arraycopy(digestedKey, 0, finalKey, 0, length);
        return finalKey;
    }

    /**
     * Encrypt or decrypt data with RC4.
     *
     * @param finalKey The final key obtained with via {@link #calcFinalKey(long, long)}.
     * @param input    The data to encrypt.
     * @param output   The output to write the encrypted data to.
     * @throws IOException If there is an error reading the data.
     */
    protected void decryptDataRC4(byte[] finalKey, InputStream input, OutputStream output)
            throws IOException
    {
        rc4.setKey(finalKey);
        rc4.write(input, output);
    }

    /**
     * Encrypt or decrypt data with RC4.
     *
     * @param finalKey The final key obtained with via {@link #calcFinalKey(long, long)}.
     * @param input    The data to encrypt.
     * @param output   The output to write the encrypted data to.
     * @throws IOException If there is an error reading the data.
     */
    protected void decryptDataRC4(byte[] finalKey, byte[] input, OutputStream output)
            throws IOException
    {
        rc4.setKey(finalKey);
        rc4.write(input, output);
    }

    /**
     * Encrypt or decrypt data with AES with key length other than 256 bits.
     *
     * @param finalKey The final key obtained with via .
     * @param data     The data to encrypt.
     * @param output   The output to write the encrypted data to.
     * @throws IOException If there is an error reading the data.
     */
    private void decryptDataAESother(byte[] finalKey, InputStream data, OutputStream output)
            throws IOException
    {
        byte[] iv = new byte[16];

        int ivSize = data.read(iv);
        if (ivSize == -1)
        {
            return;
        }
        if (ivSize != iv.length)
        {
            throw new IOException("AES initialization vector not fully read: only " + ivSize
                    + " bytes read instead of " + iv.length);
        }

        try
        {
            Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            SecretKey aesKey = new SecretKeySpec(finalKey, "AES");
            IvParameterSpec ips = new IvParameterSpec(iv);
            decryptCipher.init(Cipher.DECRYPT_MODE, aesKey, ips);
            byte[] buffer = new byte[256];
            int n;
            while ((n = data.read(buffer)) != -1)
            {
                byte[] update = decryptCipher.update(buffer, 0, n);
                if (update != null)
                {
                    output.write(update);
                }
            }
            output.write(decryptCipher.doFinal());
        }
        catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Encrypt or decrypt data with AES256.
     *
     * @param data   The data to encrypt.
     * @param output The output to write the encrypted data to.
     * @throws IOException If there is an error reading the data.
     */
    private void decryptDataAES256(InputStream data, OutputStream output) throws IOException
    {
        byte[] iv = new byte[16];

        // read IV from stream
        int ivSize = data.read(iv);
        if (ivSize == -1)
        {
            return;
        }

        if (ivSize != iv.length)
        {
            throw new IOException("AES initialization vector not fully read: only " + ivSize
                    + " bytes read instead of " + iv.length);
        }
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                CBCBlockCipher.newInstance(new AESFastEngine()));
        cipher.init(false, new ParametersWithIV(new KeyParameter(encryptionKey), iv));
        try (CipherInputStream cis = new CipherInputStream(data, cipher))
        {
            IOUtils.copy(cis, output);
        }
    }

    /**
     * This will dispatch to the correct method.
     *
     * @param obj    The object to decrypt.
     * @param objNum The object number.
     * @param genNum The object generation Number.
     * @throws IOException If there is an error getting the stream data.
     */
    public void decrypt(COSBase obj, long objNum, long genNum) throws IOException
    {
        // PDFBOX-4477: only cache strings and streams, this improves speed and memory footprint
        if (obj instanceof COSString)
        {
            if (objects.contains(obj))
            {
                return;
            }
            objects.add(obj);
            decryptString((COSString) obj, objNum, genNum);
        }
        else if (obj instanceof COSStream)
        {
            if (objects.contains(obj))
            {
                return;
            }
            objects.add(obj);
            decryptStream((COSStream) obj, objNum, genNum);
        }
        else if (obj instanceof COSDictionary)
        {
            decryptDictionary((COSDictionary) obj, objNum, genNum);
        }
        else if (obj instanceof COSArray)
        {
            decryptArray((COSArray) obj, objNum, genNum);
        }
    }

    /**
     * This will decrypt a stream.
     *
     * @param stream The stream to decrypt.
     * @param objNum The object number.
     * @param genNum The object generation number.
     * @throws IOException If there is an error getting the stream data.
     */
    public void decryptStream(COSStream stream, long objNum, long genNum) throws IOException
    {
        if (!COSName.IDENTITY.equals(streamFilterName))
        {
            COSBase type = stream.getCOSName(COSName.TYPE);
            if (!decryptMetadata && COSName.METADATA.equals(type))
            {
                return;
            }
            // "The cross-reference stream shall not be encrypted"
            if (COSName.XREF.equals(type))
            {
                return;
            }
            if (COSName.METADATA.equals(type))
            {
                byte[] buf = new byte[10];
                // PDFBOX-3229 check case where metadata is not encrypted despite /EncryptMetadata missing
                try (InputStream is = stream.getUnfilteredStream())
                {
                    is.read(buf);
                }
                if (Arrays.equals(buf, "<?xpacket ".getBytes(StandardCharsets.ISO_8859_1)))
                {
                    LOG.warn("Metadata is not encrypted, but was expected to be");
                    LOG.warn("Read PDF specification about EncryptMetadata (default value: true)");
                    return;
                }
            }
            decryptDictionary(stream, objNum, genNum);
            byte[] encrypted = IOUtils.toByteArray(stream.getFilteredStream());
            ByteArrayInputStream encryptedStream = new ByteArrayInputStream(encrypted);
            try (OutputStream output = stream.createFilteredStream())
            {
                decryptData(objNum, genNum, encryptedStream, output);
            }
            catch (IOException e)
            {
                logErrorOnce("Failed to decrypt COSStream object " + objNum + " " + genNum + ": " + e.getMessage(), objNum);
                throw e;
            }
        }
    }

    /**
     * This will decrypt a dictionary.
     *
     * @param dictionary The dictionary to decrypt.
     * @param objNum     The object number.
     * @param genNum     The object generation number.
     * @throws IOException If there is an error creating a new string.
     */
    private void decryptDictionary(COSDictionary dictionary, long objNum, long genNum)
            throws IOException
    {
        if (dictionary.getItem(COSName.CF) != null)
        {
            // PDFBOX-2936: avoid orphan /CF dictionaries found in US govt "I-" files
            return;
        }
        COSBase type = dictionary.getDictionaryObject(COSName.TYPE);
        boolean isSignature = COSName.SIG.equals(type) || COSName.DOC_TIME_STAMP.equals(type) ||
                // PDFBOX-4466: /Type is optional, see
                // https://ec.europa.eu/cefdigital/tracker/browse/DSS-1538
                (dictionary.getDictionaryObject(COSName.CONTENTS) instanceof COSString
                        && dictionary.getDictionaryObject(COSName.BYTERANGE) instanceof COSArray);
        for (Map.Entry<COSName, COSBase> entry : dictionary.entrySet())
        {
            if (isSignature && COSName.CONTENTS.equals(entry.getKey()))
            {
                // do not decrypt the signature contents string
                continue;
            }
            COSBase value = entry.getValue();
            // within a dictionary only the following kind of COS objects have to be decrypted
            if (value instanceof COSString || value instanceof COSArray
                    || value instanceof COSDictionary)
            {
                decrypt(value, objNum, genNum);
            }
        }
    }

    /**
     * This will decrypt a string.
     *
     * @param string the string to decrypt.
     * @param objNum The object number.
     * @param genNum The object generation number.
     * @throws IOException If an error occurs writing the new string.
     */
    private void decryptString(COSString string, long objNum, long genNum) throws IOException
    {
        if (!COSName.IDENTITY.equals(stringFilterName))
        {
            ByteArrayInputStream data = new ByteArrayInputStream(string.getBytes());
            FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
            try
            {
                decryptData(objNum, genNum, data, outputStream);
                string.setValue(outputStream.toByteArray());
            }
            catch (IOException ex)
            {
                logErrorOnce("Failed to decrypt COSString object " + objNum + " " + genNum + ": " + ex.getMessage(), objNum);
            }
        }
    }

    private final Map<Long, Boolean> logOnceCache = new HashMap<>();
    private void logErrorOnce(String message, long key)
    {
        if(logOnceCache.containsKey(key))
        {
            return;
        }
        
        logOnceCache.put(key, null);
        LOG.error(message);
    }

    /**
     * This will decrypt an array.
     *
     * @param array  The array to decrypt.
     * @param objNum The object number.
     * @param genNum The object generation number.
     * @throws IOException If there is an error accessing the data.
     */
    private void decryptArray(COSArray array, long objNum, long genNum) throws IOException
    {
        for (int i = 0; i < array.size(); i++)
        {
            decrypt(array.get(i), objNum, genNum);
        }
    }

    /**
     * @return key length in bits
     */
    public int getKeyLength()
    {
        return keyLength;
    }

    public void setKeyLength(int keyLen)
    {
        this.keyLength = (short) keyLen;
    }

    /**
     * @param currentAccessPermission The access permissions to be set.
     */
    public void setCurrentAccessPermission(AccessPermission currentAccessPermission)
    {
        this.currentAccessPermission = currentAccessPermission;
    }

    /**
     * Returns the access permissions that were computed during document decryption. The returned
     * object is in read only mode.
     *
     * @return the access permissions or null if the document was not decrypted.
     */
    public AccessPermission getCurrentAccessPermission()
    {
        return currentAccessPermission;
    }

    /**
     * True if AES is used for encryption and decryption.
     *
     * @return true if AEs is used
     */
    public boolean isAES()
    {
        return useAES;
    }

    /**
     * Set to true if AES for encryption and decryption should be used.
     *
     * @param aesValue if true AES will be used
     */
    public void setAES(boolean aesValue)
    {
        useAES = aesValue;
    }

    public byte[] getEncryptionKey()
    {
        return encryptionKey;
    }

    protected void setEncryptionKey(byte[] encryptionKey)
    {
        this.encryptionKey = encryptionKey;
    }
}
