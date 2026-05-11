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

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

/**
 * A node in the structure tree.
 *
 * @author Johannes Koch
 */
public abstract class PDAbstractStructureNode extends PDDictionaryWrapper
        implements StructureElement
{

    PDAbstractStructureNode(COSDictionary dictionary)
    {
        super(dictionary);
    }

    public String getType()
    {
        return this.getCOSObject().getNameAsString(COSName.TYPE);
    }

    /**
     * @return a list of objects for the kids
     */
    public List<StructureElement> getKids()
    {
        COSBase k = this.getCOSObject().getDictionaryObject(COSName.K);
        if (k instanceof COSArray array)
        {
            return array.stream().map(this::createKid).filter(Objects::nonNull).toList();
        }
        return Stream.of(this.createKid(k)).filter(Objects::nonNull).toList();
    }

    /**
     * Appends a StructureElement kid.
     */
    public void appendKid(StructureElement element)
    {
        if (nonNull(element))
        {

            COSBase k = this.getCOSObject().getDictionaryObject(COSName.K);
            if (k == null)
            {
                // currently no kid: set new kid as kids
                this.getCOSObject().setItem(COSName.K, element);
            }
            else if (k instanceof COSArray array)
            {
                // currently more than one kid: add new kid to existing array
                array.add(element);
            }
            else
            {
                // currently one kid: put current and new kid into array and set array as kids
                this.getCOSObject().setItem(COSName.K, new COSArray(k, element.getCOSObject()));
            }
            element.setParent(this);
        }
    }

    /**
     * Inserts a StructureElement kid before another kid. If the beforeThis element is not found
     * then element is not inserted
     */
    public void insertBefore(StructureElement element, StructureElement beforeThis)
    {
        if (nonNull(element) && nonNull(beforeThis))
        {
            COSBase k = this.getCOSObject().getDictionaryObject(COSName.K);
            if (nonNull(k))
            {
                if (k instanceof COSArray array)
                {
                    int index = array.indexOfObject(beforeThis.getCOSObject());
                    if (index == -1)
                    {
                        array.add(element);
                    }
                    else
                    {
                        array.add(index, element);
                    }
                    element.setParent(this);
                }
                else if (k.getCOSObject().equals(beforeThis.getCOSObject()))
                {
                    // currently one kid: put current and new kid into array and set array as kids
                    this.getCOSObject().setItem(COSName.K, new COSArray(element.getCOSObject(), k));
                    element.setParent(this);
                }
            }
        }
    }

    /**
     * Removes a StructureElement kid.
     *
     * @return <code>true</code> if the kid was removed, <code>false</code> otherwise
     */
    protected boolean removeKid(StructureElement element)
    {
        if (nonNull(element))
        {
            COSBase k = this.getCOSObject().getDictionaryObject(COSName.K);
            if (nonNull(k))
            {
                if (k instanceof COSArray array)
                {
                    // currently more than one kid: remove kid from existing array
                    boolean removed = array.removeObject(element.getCOSObject());
                    // if now only one kid: set remaining kid as kids
                    if (array.size() == 1)
                    {
                        this.getCOSObject().setItem(COSName.K, array.getObject(0));
                    }
                    return removed;
                }
                if (k.getCOSObject().equals(element.getCOSObject()))
                {
                    this.getCOSObject().removeItem(COSName.K);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a node in the structure tree. Can be either a structure tree root, or a structure
     * element.
     *
     * @param node the node dictionary
     * @return the structure node
     */
    public static PDAbstractStructureNode create(COSDictionary node)
    {
        String type = node.getNameAsString(COSName.TYPE);
        if ("StructTreeRoot".equals(type))
        {
            return new PDStructureTreeRoot(node);
        }
        if ((type == null) || "StructElem".equals(type))
        {
            return new PDStructureElement(node);
        }
        throw new IllegalArgumentException(
                "Dictionary must not include a Type entry with a value that is neither StructTreeRoot nor StructElem.");
    }

    /**
     * Creates an object for a kid of this structure node. The type of object depends on the type of
     * the kid. It can be
     * <ul>
     * <li>a {@link PDStructureElement},</li>
     * <li>a {@link PDObjectReference},</li>
     * <li>a {@link PDMarkedContentReference},</li>
     * <li>an {@link Integer}</li>
     * </ul>
     *
     * @param kid the kid
     * @return the object
     */
    private StructureElement createKid(COSBase kid)
    {
        if (kid.getCOSObject() instanceof COSDictionary dic)
        {
            String type = dic.getNameAsString(COSName.TYPE);
            if ((type == null) || PDStructureElement.TYPE.equals(type))
            {
                // A structure element dictionary denoting another structure element
                return new PDStructureElement(dic);
            }
            if (PDObjectReference.TYPE.equals(type))
            {
                // An object reference dictionary denoting a PDF object
                return new PDObjectReference(dic, this);
            }
            if (PDMarkedContentReference.TYPE.equals(type))
            {
                // A marked-content reference dictionary denoting a marked-content sequence
                return new PDMarkedContentReference(dic, this);
            }
        }
        if (kid instanceof COSInteger mcid)
        {
            return new PDMarkedContentIdentifier(mcid, this);
        }
        return null;
    }

}
