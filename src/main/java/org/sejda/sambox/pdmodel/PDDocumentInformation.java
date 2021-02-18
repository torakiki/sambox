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
package org.sejda.sambox.pdmodel;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

/**
 * This is the document metadata.  Each getXXX method will return the entry if
 * it exists or null if it does not exist.  If you pass in null for the setXXX
 * method then it will clear the value.
 *
 * @author Ben Litchfield
 * @author Gerardo Ortiz
 *
 */
public class PDDocumentInformation extends PDDictionaryWrapper
{
    public PDDocumentInformation()
    {
        super();
    }

    /**
     * Creates a new instance with a given COS dictionary.
     * 
     * @param dictionary the dictionary
     */
    public PDDocumentInformation(COSDictionary dictionary)
    {
        super(dictionary);
    }

    /**
     * Return the properties String value.
     * <p>
     * Allows to retrieve the
     * low level date for validation purposes.
     * </p> 
     * 
     * @param propertyKey the dictionaries key
     * @return the properties value
     */
     public Object getPropertyStringValue(String propertyKey)
     {
        return getCOSObject().getString(propertyKey);
     }    

    /**
     * This will get the title of the document.  This will return null if no title exists.
     *
     * @return The title of the document.
     */
    public String getTitle()
    {
        return getCOSObject().getString(COSName.TITLE);
    }

    /**
     * This will set the title of the document.
     *
     * @param title The new title for the document.
     */
    public void setTitle( String title )
    {
        getCOSObject().setString(COSName.TITLE, title);
    }

    /**
     * This will get the author of the document.  This will return null if no author exists.
     *
     * @return The author of the document.
     */
    public String getAuthor()
    {
        return getCOSObject().getString(COSName.AUTHOR);
    }

    /**
     * This will set the author of the document.
     *
     * @param author The new author for the document.
     */
    public void setAuthor( String author )
    {
        getCOSObject().setString(COSName.AUTHOR, author);
    }

    /**
     * This will get the subject of the document.  This will return null if no subject exists.
     *
     * @return The subject of the document.
     */
    public String getSubject()
    {
        return getCOSObject().getString(COSName.SUBJECT);
    }

    /**
     * This will set the subject of the document.
     *
     * @param subject The new subject for the document.
     */
    public void setSubject( String subject )
    {
        getCOSObject().setString(COSName.SUBJECT, subject);
    }

    /**
     * This will get the keywords of the document.  This will return null if no keywords exists.
     *
     * @return The keywords of the document.
     */
    public String getKeywords()
    {
        return getCOSObject().getString(COSName.KEYWORDS);
    }

    /**
     * This will set the keywords of the document.
     *
     * @param keywords The new keywords for the document.
     */
    public void setKeywords( String keywords )
    {
        getCOSObject().setString(COSName.KEYWORDS, keywords);
    }

    /**
     * This will get the creator of the document.  This will return null if no creator exists.
     *
     * @return The creator of the document.
     */
    public String getCreator()
    {
        return getCOSObject().getString(COSName.CREATOR);
    }

    /**
     * This will set the creator of the document.
     *
     * @param creator The new creator for the document.
     */
    public void setCreator( String creator )
    {
        getCOSObject().setString(COSName.CREATOR, creator);
    }

    /**
     * This will get the producer of the document.  This will return null if no producer exists.
     *
     * @return The producer of the document.
     */
    public String getProducer()
    {
        return getCOSObject().getString(COSName.PRODUCER);
    }

    /**
     * This will set the producer of the document.
     *
     * @param producer The new producer for the document.
     */
    public void setProducer( String producer )
    {
        getCOSObject().setString(COSName.PRODUCER, producer);
    }

    /**
     * This will get the creation date of the document.  This will return null if no creation date exists.
     *
     * @return The creation date of the document.
     */
    public Calendar getCreationDate()
    {
        return getCOSObject().getDate(COSName.CREATION_DATE);
    }

    /**
     * This will set the creation date of the document.
     *
     * @param date The new creation date for the document.
     */
    public void setCreationDate( Calendar date )
    {
        getCOSObject().setDate(COSName.CREATION_DATE, date);
    }

    /**
     * This will get the modification date of the document.  This will return null if no modification date exists.
     *
     * @return The modification date of the document.
     */
    public Calendar getModificationDate()
    {
        return getCOSObject().getDate(COSName.MOD_DATE);
    }

    /**
     * This will set the modification date of the document.
     *
     * @param date The new modification date for the document.
     */
    public void setModificationDate( Calendar date )
    {
        getCOSObject().setDate(COSName.MOD_DATE, date);
    }

    /**
     * This will get the trapped value for the document.
     * This will return null if one is not found.
     *
     * @return The trapped value for the document.
     */
    public String getTrapped()
    {
        return getCOSObject().getNameAsString(COSName.TRAPPED);
    }

    /**
     * This will get the keys of all metadata getCOSObject()rmation fields for the document.
     *
     * @return all metadata key strings.
     * @since Apache PDFBox 1.3.0
     */
    public Set<String> getMetadataKeys()
    {
        Set<String> keys = new TreeSet<String>();
        for (COSName key : getCOSObject().keySet())
        {
            keys.add(key.getName());
        }
        return keys;
    }

    /**
     * This will get the value of a custom metadata getCOSObject()rmation field for the document. This will return null
     * if one is not found.
     *
     * @param fieldName Name of custom metadata field from pdf document.
     *
     * @return String Value of metadata field
     */
    public String getCustomMetadataValue(String fieldName)
    {
        return getCOSObject().getString(fieldName);
    }

    /**
     * Set the custom metadata value.
     *
     * @param fieldName The name of the custom metadata field.
     * @param fieldValue The value to the custom metadata field.
     */
    public void setCustomMetadataValue( String fieldName, String fieldValue )
    {
        getCOSObject().setString(fieldName, fieldValue);
    }
    
    public void removeMetadataField(String fieldName)
    {
        getCOSObject().removeItem(COSName.getPDFName(fieldName));
    }

    /**
     * This will set the trapped of the document.  This will be
     * 'True', 'False', or 'Unknown'.
     *
     * @param value The new trapped value for the document.
     */
    public void setTrapped( String value )
    {
        if( value != null &&
            !value.equals( "True" ) &&
            !value.equals( "False" ) &&
            !value.equals( "Unknown" ) )
        {
            throw new RuntimeException( "Valid values for trapped are " +
                                        "'True', 'False', or 'Unknown'" );
        }

        getCOSObject().setName(COSName.TRAPPED, value);
    }
}
