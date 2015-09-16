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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sejda.sambox.util.DateConverter;

/**
 * This class represents a dictionary where name/value pairs reside.
 *
 * @author Ben Litchfield
 * 
 */
public class COSDictionary extends COSBase
{
    /**
     * The name-value pairs of this dictionary. The pairs are kept in the order they were added to the dictionary.
     */
    private Map<COSName, COSBase> items = new LinkedHashMap<>();

    public COSDictionary()
    {
        // default constructor
    }

    /**
     * Copy Constructor. This will make a shallow copy of this dictionary.
     *
     * @param dict The dictionary to copy.
     */
    public COSDictionary(COSDictionary dict)
    {
        items.putAll(dict.items);
    }

    /**
     * Search in the map for the value that matches the parameter and return the first key that maps to that value.
     *
     * @param value The value to search for in the map.
     * @return The key for the value in the map or null if it does not exist.
     */
    public COSName getKeyForValue(COSBase value)
    {
        for (Map.Entry<COSName, COSBase> entry : items.entrySet())
        {
            if (entry.getValue().getCOSObject().equals(value.getCOSObject()))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * @return The number of elements in the dictionary.
     */
    public int size()
    {
        return items.size();
    }

    public void clear()
    {
        items.clear();
    }

    /**
     * This will get an object from this dictionary. If the object is a reference then it will dereference it. If the
     * object is COSNull then null will be returned.
     *
     * @param key The key to the object that we are getting.
     * @return The object that matches the key.
     */
    public COSBase getDictionaryObject(String key)
    {
        return getDictionaryObject(COSName.getPDFName(key));
    }

    /**
     * This is a special case of getDictionaryObject that takes multiple keys, it will handle the situation where
     * multiple keys could get the same value, ie if either CS or ColorSpace is used to get the colorspace. This will
     * get an object from this dictionary. If the object is a reference then it will dereference it and get it from the
     * document. If the object is COSNull then null will be returned.
     *
     * @param firstKey The first key to try.
     * @param secondKey The second key to try.
     * @return The object that matches the key.
     */
    public COSBase getDictionaryObject(COSName firstKey, COSName secondKey)
    {
        COSBase retval = getDictionaryObject(firstKey);
        if (retval == null && secondKey != null)
        {
            return getDictionaryObject(secondKey);
        }
        return retval;
    }

    /**
     * Get an object from this dictionary. If the object is a reference then it will dereference it. If the object is
     * COSNull then null will be returned.
     *
     * @param key The key to the object that we are getting.
     * @return The object that matches the key.
     */
    public COSBase getDictionaryObject(COSName key)
    {
        return Optional.ofNullable(items.get(key)).map(COSBase::getCOSObject)
                .filter(i -> !COSNull.NULL.equals(i)).orElse(null);
    }

    /**
     * Set an item in the dictionary. If value is null then the result will be the same as removeItem( key ).
     *
     * @param key The key to the dictionary object.
     * @param value The value to the dictionary object.
     */
    public void setItem(COSName key, COSBase value)
    {
        if (value == null)
        {
            removeItem(key);
        }
        else
        {
            items.put(key, value);
        }
    }

    /**
     * Set the wrapped {@link COSBase} as item in the dictionary. If value is null then the result will be the same as
     * removeItem( key ).
     *
     * @param key The key to the dictionary object.
     * @param value The value to the dictionary object.
     */
    public void setItem(COSName key, COSObjectable value)
    {
        COSBase base = null;
        if (value != null)
        {
            base = value.getCOSObject();
        }
        setItem(key, base);
    }

    /**
     * Set the wrapped {@link COSBase} as item in the dictionary. If value is null then the result will be the same as
     * removeItem( key ).
     *
     * @param key The key to the dictionary object.
     * @param value The value to the dictionary object.
     */
    public void setItem(String key, COSObjectable value)
    {
        setItem(COSName.getPDFName(key), value);
    }

    /**
     * Set a boolean item in the dictionary.
     *
     * @param key The key to the dictionary object.
     * @param value The value to the dictionary object.
     */
    public void setBoolean(String key, boolean value)
    {
        setItem(COSName.getPDFName(key), COSBoolean.valueOf(value));
    }

    /**
     * Set a boolean item in the dictionary.
     *
     * @param key The key to the dictionary object.
     * @param value The value to the dictionary object.
     */
    public void setBoolean(COSName key, boolean value)
    {
        setItem(key, COSBoolean.valueOf(value));
    }

    /**
     * Set an item in the dictionary. If value is null then the result will be the same as removeItem( key ).
     *
     * @param key The key to the dictionary object.
     * @param value The value to the dictionary object.
     */
    public void setItem(String key, COSBase value)
    {
        setItem(COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSName object. If it is null then the object will be
     * removed.
     *
     * @param key The key to the object,
     * @param value The string value for the name.
     */
    public void setName(String key, String value)
    {
        setName(COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSName object. If it is null then the object will be
     * removed.
     *
     * @param key The key to the object,
     * @param value The string value for the name.
     */
    public void setName(COSName key, String value)
    {
        setItem(key, COSName.getPDFName(value));
    }

    /**
     * Set the value of a date entry in the dictionary.
     *
     * @param key The key to the date value.
     * @param date The date value.
     */
    public void setDate(String key, Calendar date)
    {
        setDate(COSName.getPDFName(key), date);
    }

    /**
     * Set the date object.
     *
     * @param key The key to the date.
     * @param date The date to set.
     */
    public void setDate(COSName key, Calendar date)
    {
        setString(key, DateConverter.toString(date));
    }

    /**
     * Set the value of a date entry in the dictionary.
     *
     * @param embedded The embedded dictionary.
     * @param key The key to the date value.
     * @param date The date value.
     */
    public void setEmbeddedDate(String embedded, String key, Calendar date)
    {
        setEmbeddedDate(embedded, COSName.getPDFName(key), date);
    }

    /**
     * Set the date object.
     *
     * @param embedded The embedded dictionary.
     * @param key The key to the date.
     * @param date The date to set.
     */
    public void setEmbeddedDate(String embedded, COSName key, Calendar date)
    {
        COSDictionary dic = (COSDictionary) getDictionaryObject(embedded);
        if (dic == null && date != null)
        {
            dic = new COSDictionary();
            setItem(embedded, dic);
        }
        if (dic != null)
        {
            dic.setDate(key, date);
        }
    }

    /**
     * Convenience method that will convert the value to a COSString object. If it is null then the object will be
     * removed.
     *
     * @param key The key to the object,
     * @param value The string value for the name.
     */
    public void setString(String key, String value)
    {
        setString(COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSString object. If it is null then the object will be
     * removed.
     *
     * @param key The key to the object,
     * @param value The string value for the name.
     */
    public void setString(COSName key, String value)
    {
        COSString name = null;
        if (value != null)
        {
            name = COSString.parseLiteral(value);
        }
        setItem(key, name);
    }

    /**
     * Convenience method that will convert the value to a COSString object. If it is null then the object will be
     * removed.
     *
     * @param embedded The embedded dictionary to set the item in.
     * @param key The key to the object,
     * @param value The string value for the name.
     */
    public void setEmbeddedString(String embedded, String key, String value)
    {
        setEmbeddedString(embedded, COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSString object. If it is null then the object will be
     * removed.
     *
     * @param embedded The embedded dictionary to set the item in.
     * @param key The key to the object,
     * @param value The string value for the name.
     */
    public void setEmbeddedString(String embedded, COSName key, String value)
    {
        COSDictionary dic = (COSDictionary) getDictionaryObject(embedded);
        if (dic == null && value != null)
        {
            dic = new COSDictionary();
            setItem(embedded, dic);
        }
        if (dic != null)
        {
            dic.setString(key, value);
        }
    }

    /**
     * Convenience method that will convert the value to a COSInteger object.
     *
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setInt(String key, int value)
    {
        setInt(COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSInteger object.
     *
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setInt(COSName key, int value)
    {
        setItem(key, COSInteger.get(value));
    }

    /**
     * Convenience method that will convert the value to a COSInteger object.
     *
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setLong(String key, long value)
    {
        setLong(COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSInteger object.
     *
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setLong(COSName key, long value)
    {
        setItem(key, COSInteger.get(value));
    }

    /**
     * Convenience method that will convert the value to a COSInteger object.
     *
     * @param embeddedDictionary The embedded dictionary.
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setEmbeddedInt(String embeddedDictionary, String key, int value)
    {
        setEmbeddedInt(embeddedDictionary, COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSInteger object.
     *
     * @param embeddedDictionary The embedded dictionary.
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setEmbeddedInt(String embeddedDictionary, COSName key, int value)
    {
        COSDictionary embedded = (COSDictionary) getDictionaryObject(embeddedDictionary);
        if (embedded == null)
        {
            embedded = new COSDictionary();
            setItem(embeddedDictionary, embedded);
        }
        embedded.setInt(key, value);
    }

    /**
     * Convenience method that will convert the value to a COSFloat object.
     *
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setFloat(String key, float value)
    {
        setFloat(COSName.getPDFName(key), value);
    }

    /**
     * Convenience method that will convert the value to a COSFloat object.
     *
     * @param key The key to the object,
     * @param value The int value for the name.
     */
    public void setFloat(COSName key, float value)
    {
        setItem(key, new COSFloat(value));
    }

    /**
     * Sets the given boolean value at bitPos in the flags.
     *
     * @param field The COSName of the field to set the value into.
     * @param bitFlag the bit position to set the value in.
     * @param value the value the bit position should have.
     */
    public void setFlag(COSName field, int bitFlag, boolean value)
    {
        int currentFlags = getInt(field, 0);
        if (value)
        {
            currentFlags = currentFlags | bitFlag;
        }
        else
        {
            currentFlags &= ~bitFlag;
        }
        setInt(field, currentFlags);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name. Null is returned if the
     * entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @return The COS name.
     */
    public COSName getCOSName(COSName key)
    {
        return getCOSName(key, null);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name. Default is returned if the
     * entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The COS name.
     */
    public COSName getCOSName(COSName key, COSName defaultValue)
    {
        COSBase name = getDictionaryObject(key);
        if (name instanceof COSName)
        {
            return (COSName) name;
        }
        return defaultValue;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public String getNameAsString(String key)
    {
        return getNameAsString(COSName.getPDFName(key));
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public String getNameAsString(COSName key)
    {
        COSBase name = getDictionaryObject(key);
        if (name instanceof COSName)
        {
            return ((COSName) name).getName();
        }
        else if (name instanceof COSString)
        {
            return ((COSString) name).getString();
        }
        return null;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The name converted to a string.
     */
    public String getNameAsString(String key, String defaultValue)
    {
        return getNameAsString(COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The name converted to a string.
     */
    public String getNameAsString(COSName key, String defaultValue)
    {
        return Optional.ofNullable(getNameAsString(key)).orElse(defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public String getString(String key)
    {
        return getString(COSName.getPDFName(key));
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public String getString(COSName key)
    {
        COSBase value = getDictionaryObject(key);
        if (value instanceof COSString)
        {
            return ((COSString) value).getString();
        }
        return null;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a string.
     */
    public String getString(String key, String defaultValue)
    {
        return getString(COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a string.
     */
    public String getString(COSName key, String defaultValue)
    {
        return Optional.ofNullable(getString(key)).orElse(defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary.
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public String getEmbeddedString(String embedded, String key)
    {
        return getEmbeddedString(embedded, COSName.getPDFName(key), null);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary.
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public String getEmbeddedString(String embedded, COSName key)
    {
        return getEmbeddedString(embedded, key, null);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary.
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a string.
     */
    public String getEmbeddedString(String embedded, String key, String defaultValue)
    {
        return getEmbeddedString(embedded, COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary.
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a string.
     */
    public String getEmbeddedString(String embedded, COSName key, String defaultValue)
    {
        COSDictionary dic = (COSDictionary) getDictionaryObject(embedded);
        if (dic != null)
        {
            return dic.getString(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary or if the date was invalid.
     *
     * @param key The key to the item in the dictionary.
     * @return The name converted to a date.
     */
    public Calendar getDate(String key)
    {
        return getDate(COSName.getPDFName(key));
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary or if the date was invalid.
     *
     * @param key The key to the item in the dictionary.
     * @return The name converted to a date.
     */
    public Calendar getDate(COSName key)
    {
        return DateConverter.toCalendar((COSString) getDictionaryObject(key));
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a date. Null is returned if the
     * entry does not exist in the dictionary or if the date was invalid.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a date.
     */
    public Calendar getDate(String key, Calendar defaultValue)
    {
        return getDate(COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a date. Null is returned if the
     * entry does not exist in the dictionary or if the date was invalid.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a date.
     */
    public Calendar getDate(COSName key, Calendar defaultValue)
    {
        return Optional.ofNullable(getDate(key)).orElse(defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary to get.
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public Calendar getEmbeddedDate(String embedded, String key)
    {
        return getEmbeddedDate(embedded, COSName.getPDFName(key), null);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a name and convert it to a string.
     * Null is returned if the entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary to get.
     * @param key The key to the item in the dictionary.
     * @return The name converted to a string.
     */
    public Calendar getEmbeddedDate(String embedded, COSName key)
    {
        return getEmbeddedDate(embedded, key, null);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a date. Null is returned if the
     * entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary to get.
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a string.
     */
    public Calendar getEmbeddedDate(String embedded, String key, Calendar defaultValue)
    {
        return getEmbeddedDate(embedded, COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a date. Null is returned if the
     * entry does not exist in the dictionary.
     *
     * @param embedded The embedded dictionary to get.
     * @param key The key to the item in the dictionary.
     * @param defaultValue The default value to return.
     * @return The name converted to a string.
     */
    public Calendar getEmbeddedDate(String embedded, COSName key, Calendar defaultValue)
    {
        COSDictionary eDic = (COSDictionary) getDictionaryObject(embedded);
        if (eDic != null)
        {
            return eDic.getDate(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a cos boolean and convert it to a
     * primitive boolean.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value returned if the entry is null.
     *
     * @return The value converted to a boolean.
     */
    public boolean getBoolean(String key, boolean defaultValue)
    {
        return getBoolean(COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a COSBoolean and convert it to a
     * primitive boolean.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value returned if the entry is null.
     *
     * @return The entry converted to a boolean.
     */
    public boolean getBoolean(COSName key, boolean defaultValue)
    {
        return getBoolean(key, null, defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a COSBoolean and convert it to a
     * primitive boolean.
     *
     * @param firstKey The first key to the item in the dictionary.
     * @param secondKey The second key to the item in the dictionary.
     * @param defaultValue The value returned if the entry is null.
     *
     * @return The entry converted to a boolean.
     */
    public boolean getBoolean(COSName firstKey, COSName secondKey, boolean defaultValue)
    {
        COSBase bool = getDictionaryObject(firstKey, secondKey);
        if (bool instanceof COSBoolean)
        {
            return ((COSBoolean) bool).getValue();
        }
        return defaultValue;
    }

    /**
     * Get an integer from an embedded dictionary. Useful for 1-1 mappings. default:-1
     *
     * @param embeddedDictionary The name of the embedded dictionary.
     * @param key The key in the embedded dictionary.
     *
     * @return The value of the embedded integer.
     */
    public int getEmbeddedInt(String embeddedDictionary, String key)
    {
        return getEmbeddedInt(embeddedDictionary, COSName.getPDFName(key));
    }

    /**
     * Get an integer from an embedded dictionary. Useful for 1-1 mappings. default:-1
     *
     * @param embeddedDictionary The name of the embedded dictionary.
     * @param key The key in the embedded dictionary.
     *
     * @return The value of the embedded integer.
     */
    public int getEmbeddedInt(String embeddedDictionary, COSName key)
    {
        return getEmbeddedInt(embeddedDictionary, key, -1);
    }

    /**
     * Get an integer from an embedded dictionary. Useful for 1-1 mappings.
     *
     * @param embeddedDictionary The name of the embedded dictionary.
     * @param key The key in the embedded dictionary.
     * @param defaultValue The value if there is no embedded dictionary or it does not contain the key.
     *
     * @return The value of the embedded integer.
     */
    public int getEmbeddedInt(String embeddedDictionary, String key, int defaultValue)
    {
        return getEmbeddedInt(embeddedDictionary, COSName.getPDFName(key), defaultValue);
    }

    /**
     * Get an integer from an embedded dictionary. Useful for 1-1 mappings.
     *
     * @param embeddedDictionary The name of the embedded dictionary.
     * @param key The key in the embedded dictionary.
     * @param defaultValue The value if there is no embedded dictionary or it does not contain the key.
     *
     * @return The value of the embedded integer.
     */
    public int getEmbeddedInt(String embeddedDictionary, COSName key, int defaultValue)
    {
        COSDictionary embedded = (COSDictionary) getDictionaryObject(embeddedDictionary);
        if (embedded != null)
        {
            return embedded.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an int. -1 is returned if there is
     * no value.
     *
     * @param key The key to the item in the dictionary.
     * @return The integer value.
     */
    public int getInt(String key)
    {
        return getInt(COSName.getPDFName(key), -1);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an int. -1 is returned if there is
     * no value.
     *
     * @param key The key to the item in the dictionary.
     * @return The integer value..
     */
    public int getInt(COSName key)
    {
        return getInt(key, -1);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an integer. If the dictionary value
     * is null then the default Value will be returned.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The integer value.
     */
    public int getInt(String key, int defaultValue)
    {
        return getInt(COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an integer. If the dictionary value
     * is null then the default Value will be returned.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The integer value.
     */
    public int getInt(COSName key, int defaultValue)
    {
        return getInt(key, null, defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an integer. If the dictionary value
     * is null then the default Value -1 will be returned.
     *
     * @param firstKey The first key to the item in the dictionary.
     * @param secondKey The second key to the item in the dictionary.
     * @return The integer value.
     */
    public int getInt(COSName firstKey, COSName secondKey)
    {
        return getInt(firstKey, secondKey, -1);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an integer. If the dictionary value
     * is null then the default Value will be returned.
     *
     * @param firstKey The first key to the item in the dictionary.
     * @param secondKey The second key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The integer value.
     */
    public int getInt(COSName firstKey, COSName secondKey, int defaultValue)
    {
        COSBase obj = getDictionaryObject(firstKey, secondKey);
        if (obj instanceof COSNumber)
        {
            return ((COSNumber) obj).intValue();
        }
        return defaultValue;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an long. -1 is returned if there is
     * no value.
     *
     * @param key The key to the item in the dictionary.
     *
     * @return The long value.
     */
    public long getLong(String key)
    {
        return getLong(COSName.getPDFName(key), -1L);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an long. -1 is returned if there is
     * no value.
     *
     * @param key The key to the item in the dictionary.
     * @return The long value.
     */
    public long getLong(COSName key)
    {
        return getLong(key, -1L);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an integer. If the dictionary value
     * is null then the default Value will be returned.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The integer value.
     */
    public long getLong(String key, long defaultValue)
    {
        return getLong(COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an integer. If the dictionary value
     * is null then the default Value will be returned.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The integer value.
     */
    public long getLong(COSName key, long defaultValue)
    {
        COSBase obj = getDictionaryObject(key);
        if (obj instanceof COSNumber)
        {
            return ((COSNumber) obj).longValue();
        }
        return defaultValue;
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an float. -1 is returned if there
     * is no value.
     *
     * @param key The key to the item in the dictionary.
     * @return The float value.
     */
    public float getFloat(String key)
    {
        return getFloat(COSName.getPDFName(key), -1);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an float. -1 is returned if there
     * is no value.
     *
     * @param key The key to the item in the dictionary.
     * @return The float value.
     */
    public float getFloat(COSName key)
    {
        return getFloat(key, -1);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be a float. If the dictionary value is
     * null then the default Value will be returned.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The float value.
     */
    public float getFloat(String key, float defaultValue)
    {
        return getFloat(COSName.getPDFName(key), defaultValue);
    }

    /**
     * Convenience method that will get the dictionary object that is expected to be an float. If the dictionary value
     * is null then the default Value will be returned.
     *
     * @param key The key to the item in the dictionary.
     * @param defaultValue The value to return if the dictionary item is null.
     * @return The float value.
     */
    public float getFloat(COSName key, float defaultValue)
    {
        COSBase obj = getDictionaryObject(key);
        if (obj instanceof COSNumber)
        {
            return ((COSNumber) obj).floatValue();
        }
        return defaultValue;
    }

    /**
     * Gets the boolean value from the flags at the given bit position.
     *
     * @param field The COSName of the field to get the flag from.
     * @param bitFlag the bitPosition to get the value from.
     *
     * @return true if the number at bitPos is '1'
     */
    public boolean getFlag(COSName field, int bitFlag)
    {
        int ff = getInt(field, 0);
        return (ff & bitFlag) == bitFlag;
    }

    /**
     * Remove an item for the dictionary. This will do nothing of the object does not exist.
     *
     * @param key The key to the item to remove from the dictionary.
     */
    public void removeItem(COSName key)
    {
        items.remove(key);
    }

    /**
     * Remove the items for the dictionary. This will do nothing of the object does not exist.
     *
     * @param keys The keys to the item to remove from the dictionary.
     */
    public void removeItems(COSName... keys)
    {
        Arrays.stream(keys).forEach(items::remove);
    }

    /**
     * @param key The key to the object.
     * @return The item that matches the key.
     */
    public COSBase getItem(COSName key)
    {
        return items.get(key);
    }

    /**
     * @param key The key to the object.
     * @return The item that matches the key.
     */
    public COSBase getItem(String key)
    {
        return getItem(COSName.getPDFName(key));
    }

    /**
     * @return names of the entries in this dictionary. The returned set is in the order the entries were added to the
     * dictionary.
     */
    public Set<COSName> keySet()
    {
        return items.keySet();
    }

    /**
     * @return name-value entries in this dictionary. The returned set is in the order the entries were added to the
     * dictionary.
     */
    public Set<Map.Entry<COSName, COSBase>> entrySet()
    {
        return items.entrySet();
    }

    /**
     * @return All the values for the dictionary.
     */
    public Collection<COSBase> getValues()
    {
        return items.values();
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    /**
     * This will add all of the dictionaries keys/values to this dictionary.
     * 
     * @param dic The dic to get the keys from.
     */
    public void addAll(COSDictionary dic)
    {
        for (Map.Entry<COSName, COSBase> entry : dic.entrySet())
        {
            setItem(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @see java.util.Map#containsKey(Object)
     *
     * @param name The key to find in the map.
     * @return true if the map contains this key.
     */
    public boolean containsKey(COSName name)
    {
        return this.items.containsKey(name);
    }

    /**
     * @see java.util.Map#containsKey(Object)
     *
     * @param name The key to find in the map.
     * @return true if the map contains this key.
     */
    public boolean containsKey(String name)
    {
        return containsKey(COSName.getPDFName(name));
    }

    /**
     * Adds all of the dictionaries keys/values to this dictionary, but only if they don't already exist. If a key
     * already exists in this dictionary then nothing is changed.
     *
     * @param dic The {@link COSDictionary} to get the keys from.
     */
    public void mergeWithoutOverwriting(COSDictionary dic)
    {
        for (Map.Entry<COSName, COSBase> entry : dic.entrySet())
        {
            if (getItem(entry.getKey()) == null)
            {
                setItem(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds all of the dictionaries keys/values to this dictionary. If a key already exists in this dictionary the value
     * is overridden.
     *
     * @param dic The {@link COSDictionary} to get the keys from.
     */
    public void merge(COSDictionary dic)
    {
        for (Map.Entry<COSName, COSBase> entry : dic.entrySet())
        {
            setItem(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @return an unmodifiable view of this dictionary
     */
    public COSDictionary asUnmodifiableDictionary()
    {
        return new UnmodifiableCOSDictionary(this);
    }

    /**
     * @return a new {@link COSDictionary} that is a duplicate of this
     */
    public COSDictionary duplicate()
    {
        return new COSDictionary(this);
    }

    @Override
    public String toString()
    {
        StringBuilder retVal = new StringBuilder("COSDictionary{");
        for (COSName key : items.keySet())
        {
            retVal.append("(");
            retVal.append(key);
            retVal.append(":");
            retVal.append(Optional.ofNullable(getItem(key)).map(v -> {
                if (v instanceof COSDictionary)
                {
                    return "COSDictionary{.." + ((COSDictionary) v).size() + " items ..}";
                }
                else if (v instanceof COSArray)
                {
                    return "COSArray{.." + ((COSArray) v).size() + " items ..}";
                }
                return v.toString();
            }).orElse("null"));
            retVal.append(") ");
        }
        retVal.append("}");
        return retVal.toString();
    }

}
