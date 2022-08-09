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
package org.sejda.sambox.pdmodel.documentinterchange.logicalstructure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDStructureElementNameTreeNode;
import org.sejda.sambox.pdmodel.common.COSDictionaryMap;
import org.sejda.sambox.pdmodel.common.PDNameTreeNode;
import org.sejda.sambox.pdmodel.common.PDNumberTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A root of a structure tree.
 * 
 * @author Ben Litchfield
 * @author Johannes Koch
 * 
 */
public class PDStructureTreeRoot extends PDStructureNode
{

    private static final Logger LOG = LoggerFactory.getLogger(PDStructureTreeRoot.class);

    private static final String TYPE = "StructTreeRoot";

    public PDStructureTreeRoot()
    {
        super(TYPE);
    }

    /**
     * Constructor for an existing structure element.
     * 
     * @param dic The existing dictionary.
     */
    public PDStructureTreeRoot(COSDictionary dic)
    {
        super(dic);
    }

    /**
     * 
     * @return the K array entry
     * @deprecated use {@link #getK()} only. /K can be a dictionary or an array, and the next level can also be a
     * dictionary. See file 054080.pdf in PDFBOX-4417 and read "Entries in the structure tree root" in the PDF
     * specification.
     */
    @Deprecated
    public COSArray getKArray()
    {
        COSBase k = this.getCOSObject().getDictionaryObject(COSName.K);
        if (k instanceof COSDictionary kdict)
        {
            k = kdict.getDictionaryObject(COSName.K);
            if (k instanceof COSArray)
            {
                return (COSArray) k;
            }
        }
        else if (k instanceof COSArray)
        {
            return (COSArray) k;
        }
        return null;
    }

    /**
     * Returns the K entry.
     * 
     * @return the K entry
     */
    public COSBase getK()
    {
        return this.getCOSObject().getDictionaryObject(COSName.K);
    }

    /**
     * Sets the K entry.
     * 
     * @param k the K value
     */
    public void setK(COSBase k)
    {
        this.getCOSObject().setItem(COSName.K, k);
    }

    /**
     * Returns the ID tree.
     * 
     * @return the ID tree
     */
    public PDNameTreeNode<PDStructureElement> getIDTree()
    {
        COSBase base = this.getCOSObject().getDictionaryObject(COSName.ID_TREE);
        if (base instanceof COSDictionary)
        {
            return new PDStructureElementNameTreeNode((COSDictionary) base);
        }
        return null;
    }

    /**
     * Sets the ID tree.
     * 
     * @param idTree the ID tree
     */
    public void setIDTree(PDNameTreeNode<PDStructureElement> idTree)
    {
        this.getCOSObject().setItem(COSName.ID_TREE, idTree);
    }

    /**
     * Returns the parent tree.
     * 
     * @return the parent tree
     */
    public PDNumberTreeNode getParentTree()
    {
        COSBase base = getCOSObject().getDictionaryObject(COSName.PARENT_TREE);
        if (base instanceof COSDictionary)
        {
            return new PDNumberTreeNode((COSDictionary) base, PDParentTreeValue.class);
        }
        return null;
    }

    /**
     * Sets the parent tree.
     * 
     * @param parentTree the parent tree
     */
    public void setParentTree(PDNumberTreeNode parentTree)
    {
        this.getCOSObject().setItem(COSName.PARENT_TREE, parentTree);
    }

    /**
     * Returns the next key in the parent tree.
     * 
     * @return the next key in the parent tree
     */
    public int getParentTreeNextKey()
    {
        return this.getCOSObject().getInt(COSName.PARENT_TREE_NEXT_KEY);
    }

    /**
     * Sets the next key in the parent tree.
     * 
     * @param parentTreeNextkey the next key in the parent tree.
     */
    public void setParentTreeNextKey(int parentTreeNextkey)
    {
        this.getCOSObject().setInt(COSName.PARENT_TREE_NEXT_KEY, parentTreeNextkey);
    }

    /**
     * Returns the role map.
     * 
     * @return the role map
     */
    public Map<String, Object> getRoleMap()
    {
        COSBase rm = this.getCOSObject().getDictionaryObject(COSName.ROLE_MAP);
        if (rm instanceof COSDictionary)
        {
            try
            {
                return COSDictionaryMap.convertBasicTypesToMap((COSDictionary) rm);
            }
            catch (IOException e)
            {
                LOG.error(e.getMessage(), e);
            }
        }
        return new HashMap<>();
    }

    /**
     * Sets the role map.
     * 
     * @param roleMap the role map
     */
    public void setRoleMap(Map<String, String> roleMap)
    {
        COSDictionary rmDic = new COSDictionary();
        for (Map.Entry<String, String> entry : roleMap.entrySet())
        {
            rmDic.setName(entry.getKey(), entry.getValue());
        }
        this.getCOSObject().setItem(COSName.ROLE_MAP, rmDic);
    }

}
