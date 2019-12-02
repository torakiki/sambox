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

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

/**
 * An array of PDFBase objects as part of the PDF document.
 *
 * @author Ben Litchfield
 */
public class COSArray extends COSBase implements List<COSBase>
{
    private final List<COSBase> objects = new ArrayList<>();

    public COSArray()
    {
        // default constructor
    }

    public COSArray(COSBase... items)
    {
        Arrays.stream(items).forEach(objects::add);
    }

    /**
     * Add an object to the array
     * 
     * @param object The object to add to the array.
     * @see List#add(Object)
     */
    public boolean add(COSObjectable object)
    {
        return add(object.getCOSObject());
    }

    @Override
    public boolean add(COSBase object)
    {
        return objects.add(object);
    }

    /**
     * Add an object at the index location and push the rest to the right.
     *
     * @param index The index to add at.
     * @param object The object to add at that index.
     * @see List#add(int, Object)
     */
    public void add(int index, COSObjectable object)
    {
        add(index, object.getCOSObject());
    }

    @Override
    public void add(int index, COSBase object)
    {
        objects.add(index, object);
    }

    @Override
    public void clear()
    {
        objects.clear();
    }

    @Override
    public boolean removeAll(Collection<?> objectsList)
    {
        return objects.removeAll(objectsList);
    }

    @Override
    public boolean retainAll(Collection<?> objectsList)
    {
        return objects.retainAll(objectsList);
    }

    @Override
    public boolean addAll(Collection<? extends COSBase> objectsList)
    {
        return objects.addAll(objectsList);
    }

    /**
     * This will add all objects to this array.
     *
     * @param objectList The objects to add.
     */
    public boolean addAll(COSArray objectList)
    {
        if (objectList != null)
        {
            return objects.addAll(objectList.objects);
        }
        return false;
    }

    @Override
    public boolean addAll(int i, Collection<? extends COSBase> objectList)
    {
        return objects.addAll(i, objectList);
    }

    @Override
    public COSBase set(int index, COSBase object)
    {
        return objects.set(index, object);
    }

    /**
     * Set an object at a specific index.
     *
     * @param index zero based index into array.
     * @param object The object to set.
     */
    public void set(int index, COSObjectable object)
    {
        COSBase base = null;
        if (object != null)
        {
            base = object.getCOSObject();
        }
        set(index, base);
    }

    /**
     * This will get an object from the array. This will dereference the object. If the object is COSNull then null will
     * be returned.
     *
     * @param index The index into the array to get the object.
     * @return The object at the requested index.
     */
    public COSBase getObject(int index)
    {
        return Optional.of(objects.get(index)).map(COSBase::getCOSObject)
                .filter(i -> i != COSNull.NULL).orElse(null);
    }

    /**
     * This will get an object from the array. This will dereference the object. If the type is not compatible, null is
     * returned
     * 
     * @param index
     * @param clazz
     * @return The object that matches the key and the type or null.
     */
    public <T extends COSBase> T getObject(int index, Class<T> clazz)
    {
        return ofNullable(objects.get(index)).map(COSBase::getCOSObject)
                .filter(i -> clazz.isInstance(i)).map(clazz::cast).orElse(null);
    }

    /**
     * Get an object from the array. This will NOT derefernce the COS object.
     *
     * @param index The index into the array to get the object.
     * @return The object at the requested index.
     * @see List#get(int)
     */
    @Override
    public COSBase get(int index)
    {
        return objects.get(index);
    }

    /**
     * Get the value of the array as an integer.
     *
     * @param index The index into the list.
     * @return The value at that index or -1 if it is null.
     */
    public int getInt(int index)
    {
        return getInt(index, -1);
    }

    /**
     * Get the value of the array as an integer, return the default if it does not exist.
     *
     * @param index The value of the array.
     * @param defaultValue The value to return if the value is null.
     * @return The value at the index or the defaultValue.
     */
    public int getInt(int index, int defaultValue)
    {
        if (index < size())
        {
            COSBase obj = objects.get(index);
            if (obj instanceof COSNumber)
            {
                return ((COSNumber) obj).intValue();
            }
        }
        return defaultValue;
    }

    /**
     * Get the value of the array as a string.
     *
     * @param index The index into the array.
     * @return The name converted to a string or null if it does not exist.
     */
    public String getName(int index)
    {
        return getName(index, null);
    }

    /**
     * Get an entry in the array that is expected to be a COSName.
     * 
     * @param index The index into the array.
     * @param defaultValue The value to return if it is null.
     * @return The value at the index or defaultValue if none is found.
     */
    public String getName(int index, String defaultValue)
    {
        if (index < size())
        {
            COSBase obj = objects.get(index);
            if (obj instanceof COSName)
            {
                return ((COSName) obj).getName();
            }
        }
        return defaultValue;
    }

    /**
     * Set the value in the array as a string.
     * 
     * @param index The index into the array.
     * @param string The string to set in the array.
     */
    public void setString(int index, String string)
    {
        if (string != null)
        {
            set(index, COSString.parseLiteral(string));
        }
        else
        {
            set(index, null);
        }
    }

    /**
     * Get the value of the array as a string.
     *
     * @param index The index into the array.
     * @return The string or null if it does not exist.
     */
    public String getString(int index)
    {
        return getString(index, null);
    }

    /**
     * Get an entry in the array that is expected to be a COSName.
     * 
     * @param index The index into the array.
     * @param defaultValue The value to return if it is null.
     * @return The value at the index or defaultValue if none is found.
     */
    public String getString(int index, String defaultValue)
    {
        if (index < size())
        {
            Object obj = objects.get(index);
            if (obj instanceof COSString)
            {
                return ((COSString) obj).getString();
            }
        }
        return defaultValue;
    }

    @Override
    public int size()
    {
        return objects.size();
    }

    @Override
    public COSBase remove(int i)
    {
        return objects.remove(i);
    }

    /**
     * Removes the last object of the array
     * 
     * @return the removed object or null if the array was empty
     */
    public COSBase removeLast()
    {
        if (!objects.isEmpty())
        {
            return objects.remove(objects.size() - 1);
        }
        return null;
    }

    @Override
    public boolean remove(Object o)
    {
        return objects.remove(o);
    }

    /**
     * This will remove an element from the array. This method will also remove a reference to the object.
     *
     * @param o The object to remove.
     * @return <code>true</code> if the object was removed, <code>false</code> otherwise
     */
    public boolean removeObject(COSBase o)
    {
        boolean removed = this.remove(o);
        if (!removed)
        {
            for (int i = 0; i < this.size(); i++)
            {
                COSBase entry = this.get(i);
                if (entry.getCOSObject().equals(o))
                {
                    return this.remove(entry);
                }
            }
        }
        return removed;
    }

    @Override
    public Iterator<COSBase> iterator()
    {
        return objects.iterator();
    }

    @Override
    public ListIterator<COSBase> listIterator()
    {
        return objects.listIterator();
    }

    @Override
    public ListIterator<COSBase> listIterator(int index)
    {
        return objects.listIterator(index);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return objects.lastIndexOf(o);
    }

    @Override
    public int indexOf(Object object)
    {
        return objects.indexOf(object);
    }

    /**
     * This will return the index of the entry or -1 if it is not found. This method will also find references to
     * indirect objects.
     *
     * @param object The object to search for.
     * @return The index of the object or -1.
     */
    public int indexOfObject(COSBase object)
    {
        for (int i = 0; i < this.size(); i++)
        {
            if (this.get(i).equals(object) || this.get(i).getCOSObject().equals(object))
            {
                return i;
            }

        }
        return -1;
    }

    /**
     * This will add null values until the size of the array is at least as large as the parameter. If the array is
     * already larger than the parameter then nothing is done.
     *
     * @param size The desired size of the array.
     */
    public COSArray growToSize(int size)
    {
        return growToSize(size, null);
    }

    /**
     * This will add the object until the size of the array is at least as large as the parameter. If the array is
     * already larger than the parameter then nothing is done.
     *
     * @param size The desired size of the array.
     * @param object The object to fill the array with.
     */
    public COSArray growToSize(int size, COSBase object)
    {
        while (size() < size)
        {
            add(object);
        }
        return this;
    }

    /**
     * trims the array to the given size
     * 
     * @param size
     */
    public COSArray trimToSize(int size)
    {
        if (size() > size)
        {
            objects.subList(size, size()).clear();
        }
        return this;
    }

    /**
     * This will take an COSArray of numbers and convert it to a float[].
     *
     * @return This COSArray as an array of float numbers.
     */
    public float[] toFloatArray()
    {
        float[] retval = new float[size()];
        for (int i = 0; i < size(); i++)
        {
            retval[i] = ofNullable(getObject(i, COSNumber.class)).map(COSNumber::floatValue)
                    .orElse(0f);
        }
        return retval;
    }

    /**
     * Clear the current contents of the COSArray and set it with the float[].
     *
     * @param value The new value of the float array.
     */
    public void setFloatArray(float[] value)
    {
        this.clear();
        for (float aValue : value)
        {
            add(new COSFloat(aValue));
        }
    }

    /**
     * @return the COSArray as List
     */
    public List<? extends COSBase> toList()
    {
        ArrayList<COSBase> retList = new ArrayList<>(size());
        retList.addAll(objects);
        return retList;
    }

    @Override
    public boolean isEmpty()
    {
        return objects.isEmpty();
    }

    @Override
    public Object[] toArray()
    {
        return objects.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return objects.toArray(a);
    }

    @Override
    public boolean contains(Object o)
    {
        return objects.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return objects.containsAll(c);
    }

    @Override
    public List<COSBase> subList(int fromIndex, int toIndex)
    {
        return objects.subList(fromIndex, toIndex);
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    /**
     * @return a new {@link COSArray} that is a duplicate of this
     */
    public COSArray duplicate()
    {
        COSArray ret = new COSArray();
        ret.addAll(this);
        return ret;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }
        if (!(o instanceof COSArray))
        {
            return false;
        }
        return objects.equals(((COSArray) o).objects);
    }

    @Override
    public int hashCode()
    {
        return objects.hashCode();
    }

    @Override
    public String toString()
    {
        return "COSArray{" + objects + "}";
    }
}
