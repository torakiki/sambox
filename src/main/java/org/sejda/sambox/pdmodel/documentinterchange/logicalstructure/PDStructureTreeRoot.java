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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDStructureElementNameTreeNode;
import org.sejda.sambox.pdmodel.common.PDNameTreeNode;
import org.sejda.sambox.pdmodel.common.PDNumberTreeNode;

/**
 * A root of a structure tree.
 *
 * @author Ben Litchfield
 * @author Johannes Koch
 *
 */
public class PDStructureTreeRoot extends PDAbstractStructureNode
{

    public static final String TYPE = "StructTreeRoot";

    public PDStructureTreeRoot()
    {
        this(COSDictionary.of(COSName.TYPE, COSName.getPDFName(TYPE)));
    }

    /**
     * @param dictionary The existing structure tree root dictionary.
     */
    public PDStructureTreeRoot(COSDictionary dictionary)
    {
        super(dictionary);
    }

    /**
     * @return the K entry
     */
    public COSBase getK()
    {
        return this.getCOSObject().getDictionaryObject(COSName.K);
    }

    public void setK(COSBase k)
    {
        this.getCOSObject().setItem(COSName.K, k);
    }

    /**
     * @return the ID tree
     */
    public PDNameTreeNode<PDStructureElement> getIDTree()
    {
        COSDictionary idTree = this.getCOSObject()
                .getDictionaryObject(COSName.ID_TREE, COSDictionary.class);
        if (nonNull(idTree))
        {
            return new PDStructureElementNameTreeNode(idTree);
        }
        return null;
    }

    public void setIDTree(PDNameTreeNode<PDStructureElement> idTree)
    {
        this.getCOSObject().setItem(COSName.ID_TREE, idTree);
    }

    /**
     * @return the parent tree
     */
    public PDNumberTreeNode getParentTree()
    {
        COSDictionary parentTree = getCOSObject().getDictionaryObject(COSName.PARENT_TREE,
                COSDictionary.class);
        if (nonNull(parentTree))
        {
            return new PDNumberTreeNode(parentTree, PDParentTreeValue.class);
        }
        return null;
    }

    public void setParentTree(PDNumberTreeNode parentTree)
    {
        this.getCOSObject().setItem(COSName.PARENT_TREE, parentTree);
    }

    /**
     * @return the next key in the parent tree
     */
    public int getParentTreeNextKey()
    {
        return this.getCOSObject().getInt(COSName.PARENT_TREE_NEXT_KEY);
    }

    public void setParentTreeNextKey(int parentTreeNextkey)
    {
        this.getCOSObject().setInt(COSName.PARENT_TREE_NEXT_KEY, parentTreeNextkey);
    }

    /**
     * @return the role map
     */
    public COSDictionary getRoleMap()
    {
        return this.getCOSObject().getDictionaryObject(COSName.ROLE_MAP, COSDictionary.class);
    }

    /**
     * @param roleMap the role map
     */
    public void setRoleMap(Map<String, String> roleMap)
    {
        getCOSObject().setItem(COSName.ROLE_MAP, ofNullable(roleMap).map(c -> {
            COSDictionary dictionary = new COSDictionary();
            c.forEach(dictionary::setName);
            return dictionary;
        }).orElse(null));
    }

    /**
     * @return an {@link Iterable} over all {@link StructureElement}s in this tree in pre-order
     * depth-first order, excluding the root itself
     */
    public Iterable<StructureElement> elements()
    {
        return () -> new PDStructureTreeIterator(this);
    }

    /**
     * @return a sequential {@link Stream} over all {@link StructureElement}s in this tree in
     * pre-order depth-first order, excluding the root itself
     */
    public Stream<StructureElement> stream()
    {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new PDStructureTreeIterator(this),
                        Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    @Override
    public PDAbstractStructureNode getParent()
    {
        return null;
    }

    @Override
    public void setParent(PDAbstractStructureNode parent)
    {
        throw new UnsupportedOperationException("Cannot set parent on a root node");
    }

}
