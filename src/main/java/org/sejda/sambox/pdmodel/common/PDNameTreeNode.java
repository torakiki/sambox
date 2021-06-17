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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a node in a name tree.
 *
 * @author Ben Litchfield
 */
public abstract class PDNameTreeNode<T extends COSObjectable> implements COSObjectable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDNameTreeNode.class);

    private final COSDictionary node;
    private PDNameTreeNode<T> parent;

    /**
     * Constructor.
     */
    protected PDNameTreeNode()
    {
        node = new COSDictionary();
    }

    /**
     * Constructor.
     *
     * @param dict The dictionary that holds the name information.
     */
    protected PDNameTreeNode(COSDictionary dict)
    {
        node = dict;
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
     * Returns the parent node.
     * 
     * @return parent node
     */
    public PDNameTreeNode<T> getParent()
    {
        return parent;
    }

    /**
     * Sets the parent to the given node.
     * 
     * @param parentNode the node to be set as parent
     */
    public void setParent(PDNameTreeNode<T> parentNode)
    {
        parent = parentNode;
        calculateLimits();
    }

    /**
     * Determines if this is a root node or not.
     * 
     * @return true if this is a root node
     */
    public boolean isRootNode()
    {
        return parent == null;
    }

    /**
     * Return the children of this node. This list will contain PDNameTreeNode objects.
     *
     * @return The list of children or null if there are no children.
     */
    public List<PDNameTreeNode<T>> getKids()
    {
        if (nonNull(node))
        {
            COSArray kids = (COSArray) node.getDictionaryObject(COSName.KIDS);
            if (kids != null)
            {
                List<PDNameTreeNode<T>> pdObjects = new ArrayList<>();
                for (int i = 0; i < kids.size(); i++)
                {
                    pdObjects.add(createChildNode((COSDictionary) kids.getObject(i)));
                }
                return new COSArrayList<>(pdObjects, kids);
            }
        }
        return null;
    }

    /**
     * Set the children of this named tree.
     *
     * @param kids The children of this named tree.
     */
    public void setKids(List<? extends PDNameTreeNode<T>> kids)
    {
        if (kids != null && kids.size() > 0)
        {
            for (PDNameTreeNode<T> kidsNode : kids)
            {
                kidsNode.setParent(this);
            }
            node.setItem(COSName.KIDS, COSArrayList.converterToCOSArray(kids));
            // root nodes with kids don't have Names
            if (isRootNode())
            {
                node.removeItem(COSName.NAMES);
            }
        }
        else
        {
            node.removeItem(COSName.KIDS);
            node.removeItem(COSName.LIMITS);
        }
        calculateLimits();
    }

    private void calculateLimits()
    {
        if (isRootNode())
        {
            node.removeItem(COSName.LIMITS);
        }
        else
        {
            List<PDNameTreeNode<T>> kids = getKids();
            if (kids != null && kids.size() > 0)
            {
                PDNameTreeNode<T> firstKid = kids.get(0);
                PDNameTreeNode<T> lastKid = kids.get(kids.size() - 1);
                String lowerLimit = firstKid.getLowerLimit();
                setLowerLimit(lowerLimit);
                String upperLimit = lastKid.getUpperLimit();
                setUpperLimit(upperLimit);
            }
            else
            {
                try
                {
                    Map<String, T> names = getNames();
                    if (names != null && names.size() > 0)
                    {
                        Set<String> strings = names.keySet();
                        String[] keys = strings.toArray(new String[strings.size()]);
                        String lowerLimit = keys[0];
                        setLowerLimit(lowerLimit);
                        String upperLimit = keys[keys.length - 1];
                        setUpperLimit(upperLimit);
                    }
                    else
                    {
                        node.removeItem(COSName.LIMITS);
                    }
                }
                catch (IOException exception)
                {
                    node.removeItem(COSName.LIMITS);
                    LOG.error("Error while calculating the Limits of a PageNameTreeNode:",
                            exception);
                }
            }
        }
    }

    /**
     * The name to retrieve.
     *
     * @param name The name in the tree.
     * @return The value of the name in the tree.
     * @throws IOException If an there is a problem creating the destinations.
     */
    public T getValue(String name)
    {
        try
        {
            Map<String, T> names = getNames();
            if (isNull(names))
            {
                List<PDNameTreeNode<T>> kids = getKids();
                if (kids != null)
                {
                    for (PDNameTreeNode<T> childNode : kids)
                    {
                        if (childNode.couldContain(name))
                        {
                            T value = childNode.getValue(name);
                            if (nonNull(value))
                            {
                                return value;
                            }
                        }
                    }
                }
                else
                {
                    LOG.warn("NameTreeNode does not have \"names\" nor \"kids\" objects.");
                }
            }
            else
            {
                return names.get(name);
            }
        }
        catch (IOException e)
        {
            LOG.warn("NameTreeNode couldn't get the names map", e);
        }
        return null;
    }

    /**
     * @param name
     * @return true if the given name can be part of this node/branch
     */
    private boolean couldContain(String name)
    {
        if (isNull(node) || isNull(getLowerLimit()) || isNull(getUpperLimit()))
        {
            LOG.warn("Missing required name tree node Limits array");
            return false;
        }
        return getLowerLimit().compareTo(name) <= 0 && getUpperLimit().compareTo(name) >= 0;

    }

    /**
     * This will return a map of names. The key will be a string, and the value will depend on where this class is being
     * used.
     *
     * @return ordered map of cos objects or <code>null</code> if dictionary contains no 'Names' entry
     * @throws IOException If there is an error while creating the sub types.
     */
    public Map<String, T> getNames() throws IOException
    {
        COSArray namesArray = (COSArray) node.getDictionaryObject(COSName.NAMES);
        if (namesArray != null)
        {
            Map<String, T> names = new LinkedHashMap<>();
            for (int i = 0; i < namesArray.size(); i += 2)
            {
                COSString key = (COSString) namesArray.getObject(i);
                if(i + 1 >= namesArray.size())
                {
                    // some invalid NAMES arrays have only the key without a value
                    LOG.warn("Found key without value in NAMES array: " + key.getString() + ", at index: " + i);
                    continue;
                }
                COSBase cosValue = namesArray.getObject(i + 1);
                names.put(key.getString(), convertCOSToPD(cosValue));
            }
            return Collections.unmodifiableMap(names);
        }
        return null;
    }

    /**
     * Method to convert the COS value in the name tree to the PD Model object. The default implementation will simply
     * return the given COSBase object. Subclasses should do something specific.
     *
     * @param base The COS object to convert.
     * @return The converted PD Model object.
     * @throws IOException If there is an error during creation.
     */
    protected abstract T convertCOSToPD(COSBase base) throws IOException;

    /**
     * Create a child node object.
     *
     * @param dic The dictionary for the child node object to refer to.
     * @return The new child node object.
     */
    protected abstract PDNameTreeNode<T> createChildNode(COSDictionary dic);

    /**
     * Set the names of for this node. The keys should be java.lang.String and the values must be a COSObjectable. This
     * method will set the appropriate upper and lower limits based on the keys in the map.
     *
     * @param names map of names to objects, or <code>null</code>
     */
    public void setNames(Map<String, T> names)
    {
        if (names == null)
        {
            node.setItem(COSName.NAMES, (COSObjectable) null);
            node.setItem(COSName.LIMITS, (COSObjectable) null);
        }
        else
        {
            COSArray array = new COSArray();
            List<String> keys = new ArrayList<>(names.keySet());
            Collections.sort(keys);
            for (String key : keys)
            {
                array.add(COSString.parseLiteral(key));
                array.add(names.get(key));
            }
            node.setItem(COSName.NAMES, array);
            calculateLimits();
        }
    }

    /**
     * Get the highest value for a key in the name map.
     *
     * @return The highest value for a key in the map.
     */
    public String getUpperLimit()
    {
        String retval = null;
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (arr != null)
        {
            retval = arr.getString(1);
        }
        return retval;
    }

    /**
     * Set the highest value for the key in the map.
     *
     * @param upper The new highest value for a key in the map.
     */
    private void setUpperLimit(String upper)
    {
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (arr == null)
        {
            arr = new COSArray();
            arr.add(null);
            arr.add(null);
            node.setItem(COSName.LIMITS, arr);
        }
        arr.setString(1, upper);
    }

    /**
     * Get the lowest value for a key in the name map.
     *
     * @return The lowest value for a key in the map.
     */
    public String getLowerLimit()
    {
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (nonNull(arr))
        {
            return arr.getString(0);
        }
        return null;
    }

    /**
     * Set the lowest value for the key in the map.
     *
     * @param lower The new lowest value for a key in the map.
     */
    private void setLowerLimit(String lower)
    {
        COSArray arr = (COSArray) node.getDictionaryObject(COSName.LIMITS);
        if (arr == null)
        {
            arr = new COSArray();
            arr.add(null);
            arr.add(null);
            node.setItem(COSName.LIMITS, arr);
        }
        arr.setString(0, lower);
    }
}
