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

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a PDF Number tree. See the PDF Reference 1.7 section 7.9.7 for more details.
 *
 * @author Ben Litchfield,
 * @author Igor Podolskiy
 */
public class PDNumberTreeNode implements COSObjectable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDNumberTreeNode.class);

    private final COSDictionary node;
    private Class<? extends COSObjectable> valueType = null;

    /**
     * Constructor.
     *
     * @param valueClass The PD Model type of object that is the value.
     */
    public PDNumberTreeNode(Class<? extends COSObjectable> valueClass)
    {
        node = new COSDictionary();
        valueType = valueClass;
    }

    /**
     * Constructor.
     *
     * @param dict The dictionary that holds the name information.
     * @param valueClass The PD Model type of object that is the value.
     */
    public PDNumberTreeNode(COSDictionary dict, Class<? extends COSObjectable> valueClass)
    {
        node = dict;
        valueType = valueClass;
    }

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos object that matches this Java object.
     */
    @Override
    public COSDictionary getCOSObject()
    {
        return node;
    }

    /**
     * Return the children of this node. This list will contain PDNumberTreeNode objects.
     *
     * @return The list of children or null if there are no children.
     */
    public List<PDNumberTreeNode> getKids()
    {
        COSArray kids = node.getDictionaryObject(COSName.KIDS, COSArray.class);
        if (nonNull(kids))
        {
            List<PDNumberTreeNode> pdObjects = new ArrayList<>();
            for (int i = 0; i < kids.size(); i++)
            {
                pdObjects.add(createChildNode((COSDictionary) kids.getObject(i)));
            }
            return new COSArrayList<>(pdObjects, kids);
        }

        return null;
    }

    /**
     * Set the children of this number tree.
     *
     * @param kids The children of this number tree.
     */
    public void setKids(List<? extends PDNumberTreeNode> kids)
    {
        if (kids != null && !kids.isEmpty())
        {
            PDNumberTreeNode firstKid = kids.get(0);
            PDNumberTreeNode lastKid = kids.get(kids.size() - 1);
            Integer lowerLimit = firstKid.getLowerLimit();
            this.setLowerLimit(lowerLimit);
            Integer upperLimit = lastKid.getUpperLimit();
            this.setUpperLimit(upperLimit);
        }
        else if (node.getDictionaryObject(COSName.NUMS) == null)
        {
            // Remove limits if there are no kids and no numbers set.
            node.removeItem(COSName.LIMITS);
        }
        node.setItem(COSName.KIDS, COSArrayList.converterToCOSArray(kids));
    }

    /**
     * Returns the value corresponding to an index in the number tree.
     *
     * @param index The index in the number tree.
     *
     * @return The value corresponding to the index.
     *
     * @throws IOException If there is a problem creating the values.
     */
    public Object getValue(Integer index) throws IOException
    {
        Map<Integer, COSObjectable> numbers = getNumbers();
        if (nonNull(numbers))
        {
            return numbers.get(index);
        }
        Object retval = null;
        List<PDNumberTreeNode> kids = getKids();
        if (nonNull(kids))
        {
            for (int i = 0; i < kids.size() && retval == null; i++)
            {
                PDNumberTreeNode childNode = kids.get(i);
                if (childNode.getLowerLimit().compareTo(index) <= 0
                        && childNode.getUpperLimit().compareTo(index) >= 0)
                {
                    retval = childNode.getValue(index);
                }
            }
        }
        else
        {
            LOG.warn("NumberTreeNode does not have \"nums\" nor \"kids\" objects.");
        }
        return retval;
    }

    /**
     * This will return a map of numbers. The key will be a java.lang.Integer, the value will depend on where this class
     * is being used.
     *
     * @return A map of COS objects.
     *
     * @throws IOException If there is a problem creating the values.
     */
    public Map<Integer, COSObjectable> getNumbers() throws IOException
    {
        Map<Integer, COSObjectable> indices = null;
        COSArray numbersArray = node.getDictionaryObject(COSName.NUMS, COSArray.class);
        if (nonNull(numbersArray))
        {
            indices = new HashMap<>();
            for (int i = 0; i < numbersArray.size(); i += 2)
            {
                COSBase base = numbersArray.getObject(i);
                if (!(base instanceof COSInteger))
                {
                    LOG.error("page labels ignored, index {} should be a number, but is {}", i,
                            base);
                    return null;
                }
                COSInteger key = (COSInteger) base;
                COSBase cosValue = numbersArray.getObject(i + 1);
                indices.put(key.intValue(), cosValue == null ? null : convertCOSToPD(cosValue));
            }
            indices = Collections.unmodifiableMap(indices);
        }
        return indices;
    }

    /**
     * Method to convert the COS value in the name tree to the PD Model object. The default implementation will simply
     * use reflection to create the correct object type. Subclasses can do whatever they want.
     *
     * @param base The COS object to convert.
     * @return The converted PD Model object.
     * @throws IOException If there is an error during creation.
     */
    protected COSObjectable convertCOSToPD(COSBase base) throws IOException
    {
        try
        {
            return valueType.getConstructor(base.getClass()).newInstance(base);
        }
        catch (Throwable t)
        {
            throw new IOException("Error while trying to create value in number tree", t);
        }
    }

    /**
     * Create a child node object.
     *
     * @param dic The dictionary for the child node object to refer to.
     * @return The new child node object.
     */
    protected PDNumberTreeNode createChildNode(COSDictionary dic)
    {
        return new PDNumberTreeNode(dic, valueType);
    }

    /**
     * Set the names of for this node. The keys should be java.lang.String and the values must be a COSObjectable. This
     * method will set the appropriate upper and lower limits based on the keys in the map.
     *
     * @param numbers The map of names to objects.
     */
    public void setNumbers(Map<Integer, ? extends COSObjectable> numbers)
    {
        if (numbers == null)
        {
            node.setItem(COSName.NUMS, (COSObjectable) null);
            node.setItem(COSName.LIMITS, (COSObjectable) null);
        }
        else
        {
            List<Integer> keys = new ArrayList<>(numbers.keySet());
            Collections.sort(keys);
            COSArray array = new COSArray();
            for (Integer key : keys)
            {
                array.add(COSInteger.get(key));
                COSObjectable obj = numbers.get(key);
                array.add(obj == null ? COSNull.NULL : obj);
            }
            Integer lower = null;
            Integer upper = null;
            if (!keys.isEmpty())
            {
                lower = keys.get(0);
                upper = keys.get(keys.size() - 1);
            }
            setUpperLimit(upper);
            setLowerLimit(lower);
            node.setItem(COSName.NUMS, array);
        }
    }

    /**
     * Get the highest value for a key in the name map.
     *
     * @return The highest value for a key in the map.
     */
    public Integer getUpperLimit()
    {
        Integer retval = null;
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (arr != null && arr.get(0) != null)
        {
            retval = arr.getInt(1);
        }
        return retval;
    }

    /**
     * Set the highest value for the key in the map.
     *
     * @param upper The new highest value for a key in the map.
     */
    private void setUpperLimit(Integer upper)
    {
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (arr == null)
        {
            arr = new COSArray();
            arr.add(null);
            arr.add(null);
            node.setItem(COSName.LIMITS, arr);
        }
        if (upper != null)
        {
            arr.set(1, COSInteger.get(upper));
        }
        else
        {
            arr.set(1, null);
        }
    }

    /**
     * Get the lowest value for a key in the name map.
     *
     * @return The lowest value for a key in the map.
     */
    public Integer getLowerLimit()
    {
        Integer retval = null;
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (arr != null && arr.get(0) != null)
        {
            retval = arr.getInt(0);
        }
        return retval;
    }

    /**
     * Set the lowest value for the key in the map.
     *
     * @param lower The new lowest value for a key in the map.
     */
    private void setLowerLimit(Integer lower)
    {
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (arr == null)
        {
            arr = new COSArray();
            arr.add(null);
            arr.add(null);
            node.setItem(COSName.LIMITS, arr);
        }
        if (lower != null)
        {
            arr.set(0, COSInteger.get(lower));
        }
        else
        {
            arr.set(0, null);
        }
    }
}
