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
package org.sejda.sambox.pdmodel.common.filespecification;

import static java.util.Optional.ofNullable;

import java.util.Optional;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;

/**
 * This represents a file specification.
 *
 * @author Ben Litchfield
 * 
 */
public class PDComplexFileSpecification implements PDFileSpecification
{
    private final COSDictionary fileSpecificationDictionary;
    private COSDictionary embeddedFileDictionary;

    /**
     * @param dict The file specification dictionary.
     */
    public PDComplexFileSpecification(COSDictionary dict)
    {
        this.fileSpecificationDictionary = Optional.ofNullable(dict).orElseGet(COSDictionary::new);
        this.fileSpecificationDictionary.setItem(COSName.TYPE, COSName.FILESPEC);
    }

    @Override
    public COSDictionary getCOSObject()
    {
        return fileSpecificationDictionary;
    }

    private COSDictionary getEFDictionary()
    {
        if (embeddedFileDictionary == null && fileSpecificationDictionary != null)
        {
            embeddedFileDictionary = (COSDictionary) fileSpecificationDictionary
                    .getDictionaryObject(COSName.EF);
        }
        return embeddedFileDictionary;
    }

    private COSBase getObjectFromEFDictionary(COSName key)
    {
        COSDictionary ef = getEFDictionary();
        if (ef != null)
        {
            return ef.getDictionaryObject(key);
        }
        return null;
    }

    /**
     * <p>
     * Preferred method for getting the filename. It will determinate the recommended file name.
     * </p>
     * <p>
     * First of all we try to get the unicode filename if it exist. If it doesn't exist we take a look at the DOS, MAC
     * UNIX filenames. If no one exist the required F entry will be returned.
     * </p>
     *
     * @return The preferred file name.
     */
    public String getFilename()
    {
        String filename = getFileUnicode();
        if (filename == null)
        {
            filename = getFileDos();
        }
        if (filename == null)
        {
            filename = getFileMac();
        }
        if (filename == null)
        {
            filename = getFileUnix();
        }
        if (filename == null)
        {
            filename = getFile();
        }
        return filename;
    }

    /**
     * This will get the unicode file name.
     *
     * @return The file name.
     */
    public String getFileUnicode()
    {
        return fileSpecificationDictionary.getString(COSName.UF);
    }

    /**
     * This will set the unicode file name. If you call this, then do not forget to also call
     * {@link #setFile(java.lang.String) setFile(String)} or the attachment will not be visible on some viewers.
     *
     * @param file The name of the file.
     */
    public void setFileUnicode(String file)
    {
        fileSpecificationDictionary.setString(COSName.UF, file);
    }

    @Override
    public String getFile()
    {
        return fileSpecificationDictionary.getString(COSName.F);
    }

    /**
     * This will set the file name. You should also call {@link #setFileUnicode(java.lang.String)
     * setFileUnicode(String)} for cross-platform and cross-language compatibility.
     *
     * @param file The name of the file.
     */
    @Override
    public void setFile(String file)
    {
        fileSpecificationDictionary.setString(COSName.F, file);
    }

    /**
     * This will get the name representing a Dos file.
     *
     * @return The file name.
     */
    public String getFileDos()
    {
        return fileSpecificationDictionary.getString(COSName.DOS);
    }

    /**
     * This will set name representing a dos file.
     *
     * @param file The name of the file.
     * @deprecated This method is obsolescent and should not be used by conforming writers.
     */
    @Deprecated
    public void setFileDos(String file)
    {
        fileSpecificationDictionary.setString(COSName.DOS, file);
    }

    /**
     * This will get the name representing a Mac file.
     *
     * @return The file name.
     */
    public String getFileMac()
    {
        return fileSpecificationDictionary.getString(COSName.MAC);
    }

    /**
     * This will set name representing a Mac file.
     *
     * @param file The name of the file.
     * @deprecated This method is obsolescent and should not be used by conforming writers.
     */
    @Deprecated
    public void setFileMac(String file)
    {
        fileSpecificationDictionary.setString(COSName.MAC, file);
    }

    /**
     * This will get the name representing a Unix file.
     *
     * @return The file name.
     */
    public String getFileUnix()
    {
        return fileSpecificationDictionary.getString(COSName.UNIX);
    }

    /**
     * This will set name representing a Unix file.
     *
     * @param file The name of the file.
     * @deprecated This method is obsolescent and should not be used by conforming writers.
     */
    @Deprecated
    public void setFileUnix(String file)
    {
        fileSpecificationDictionary.setString(COSName.UNIX, file);
    }

    /**
     * Tell if the underlying file is volatile and should not be cached by the reader application. Default: false
     *
     * @param fileIsVolatile The new value for the volatility of the file.
     */
    public void setVolatile(boolean fileIsVolatile)
    {
        fileSpecificationDictionary.setBoolean(COSName.V, fileIsVolatile);
    }

    /**
     * Get if the file is volatile. Default: false
     *
     * @return True if the file is volatile attribute is set.
     */
    public boolean isVolatile()
    {
        return fileSpecificationDictionary.getBoolean(COSName.V, false);
    }

    /**
     * Get the embedded file.
     *
     * @return The embedded file for this file spec.
     */
    public PDEmbeddedFile getEmbeddedFile()
    {
        return embeddedFileFor(COSName.F);
    }

    /**
     * Set the embedded file for this spec. You should also call {@link #setEmbeddedFileUnicode(PDEmbeddedFile)} for
     * cross-platform and cross-language compatibility.
     *
     * @param file The file to be embedded.
     */
    public void setEmbeddedFile(PDEmbeddedFile file)
    {
        COSDictionary ef = getEFDictionary();
        if (ef == null && file != null)
        {
            ef = new COSDictionary();
            fileSpecificationDictionary.setItem(COSName.EF, ef);
        }
        if (ef != null)
        {
            ef.setItem(COSName.F, file);
        }
    }

    /**
     * Get the embedded dos file.
     *
     * @return The embedded dos file for this file spec.
     */
    public PDEmbeddedFile getEmbeddedFileDos()
    {
        return embeddedFileFor(COSName.DOS);
    }

    /**
     * Set the embedded dos file for this spec.
     *
     * @param file The dos file to be embedded.
     * @deprecated This method is obsolescent and should not be used by conforming writers.
     */
    @Deprecated
    public void setEmbeddedFileDos(PDEmbeddedFile file)
    {
        COSDictionary ef = getEFDictionary();
        if (ef == null && file != null)
        {
            ef = new COSDictionary();
            fileSpecificationDictionary.setItem(COSName.EF, ef);
        }
        if (ef != null)
        {
            ef.setItem(COSName.DOS, file);
        }
    }

    /**
     * Get the embedded Mac file.
     *
     * @return The embedded Mac file for this file spec.
     */
    public PDEmbeddedFile getEmbeddedFileMac()
    {
        return embeddedFileFor(COSName.MAC);
    }

    /**
     * Set the embedded Mac file for this spec.
     *
     * @param file The Mac file to be embedded.
     * @deprecated This method is obsolescent and should not be used by conforming writers.
     */
    @Deprecated
    public void setEmbeddedFileMac(PDEmbeddedFile file)
    {
        COSDictionary ef = getEFDictionary();
        if (ef == null && file != null)
        {
            ef = new COSDictionary();
            fileSpecificationDictionary.setItem(COSName.EF, ef);
        }
        if (ef != null)
        {
            ef.setItem(COSName.MAC, file);
        }
    }

    /**
     * Get the embedded Unix file.
     *
     * @return The embedded file for this file spec.
     */
    public PDEmbeddedFile getEmbeddedFileUnix()
    {
        return embeddedFileFor(COSName.UNIX);
    }

    /**
     * Set the embedded Unix file for this spec.
     *
     * @param file The Unix file to be embedded.
     * @deprecated This method is obsolescent and should not be used by conforming writers.
     */
    @Deprecated
    public void setEmbeddedFileUnix(PDEmbeddedFile file)
    {
        COSDictionary ef = getEFDictionary();
        if (ef == null && file != null)
        {
            ef = new COSDictionary();
            fileSpecificationDictionary.setItem(COSName.EF, ef);
        }
        if (ef != null)
        {
            ef.setItem(COSName.UNIX, file);
        }
    }

    /**
     * Get the embedded unicode file.
     *
     * @return The embedded unicode file for this file spec.
     */
    public PDEmbeddedFile getEmbeddedFileUnicode()
    {
        return embeddedFileFor(COSName.UF);
    }

    private PDEmbeddedFile embeddedFileFor(COSName key)
    {
        return ofNullable(getObjectFromEFDictionary(key)).filter(s -> s instanceof COSStream)
                .map(s -> (COSStream) s).map(PDEmbeddedFile::new).orElse(null);
    }

    /**
     * Set the embedded Unicode file for this spec. If you call this, then do not forget to also call
     * {@link #setEmbeddedFile(PDEmbeddedFile)} or the attachment will not be visible on some viewers.
     *
     * @param file The Unicode file to be embedded.
     */
    public void setEmbeddedFileUnicode(PDEmbeddedFile file)
    {
        COSDictionary ef = getEFDictionary();
        if (ef == null && file != null)
        {
            ef = new COSDictionary();
            fileSpecificationDictionary.setItem(COSName.EF, ef);
        }
        if (ef != null)
        {
            ef.setItem(COSName.UF, file);
        }
    }

    /**
     * 
     * @return the first/best available alternative of the embedded file
     */
    public PDEmbeddedFile getBestEmbeddedFile()
    {
        return ofNullable(getEmbeddedFileUnicode()).orElseGet(() -> {
            return ofNullable(getEmbeddedFileDos()).orElseGet(() -> {
                return ofNullable(getEmbeddedFileMac()).orElseGet(() -> {
                    return ofNullable(getEmbeddedFileUnix()).orElseGet(() -> {
                        return getEmbeddedFile();
                    });
                });
            });
        });
    }

    /**
     * Set the file description.
     * 
     * @param description The file description
     */
    public void setFileDescription(String description)
    {
        fileSpecificationDictionary.setString(COSName.DESC, description);
    }

    /**
     * This will get the description.
     *
     * @return The file description.
     */
    public String getFileDescription()
    {
        return fileSpecificationDictionary.getString(COSName.DESC);
    }

    /**
     * Set the collection item dictionary
     * 
     * @param the collection item dictionary
     */
    public void setCollectionItem(COSDictionary dictionary)
    {
        fileSpecificationDictionary.setItem(COSName.CI, dictionary);
    }
}
