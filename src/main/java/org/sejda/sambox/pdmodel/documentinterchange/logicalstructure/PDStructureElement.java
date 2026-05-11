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
import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.util.Iterator;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.sejda.sambox.pdmodel.documentinterchange.taggedpdf.StandardStructureType;

/**
 * A structure element.
 *
 * @author Ben Litchfield
 * @author Johannes Koch
 */
public class PDStructureElement extends PDAbstractStructureNode
{

    public static final String TYPE = "StructElem";

    public PDStructureElement(StandardStructureType type, PDAbstractStructureNode parent)
    {
        this(type.type(), parent);
    }

    public PDStructureElement(String structureType, PDAbstractStructureNode parent)
    {
        this(COSDictionary.of(COSName.TYPE, COSName.getPDFName(TYPE)));
        this.setStructureType(structureType);
        this.setParent(parent);
    }

    /**
     * @param dictionary The existing structure element dictionary.
     */
    public PDStructureElement(COSDictionary dictionary)
    {
        requireNotNullArg(dictionary, "Dictionary cannot be null");
        super(dictionary);

    }

    /**
     * @return the structure type
     */
    public String getStructureType()
    {
        return this.getCOSObject().getNameAsString(COSName.S);
    }

    public final void setStructureType(String structureType)
    {
        this.getCOSObject().setName(COSName.S, structureType);
    }

    /**
     * @return the parent in the structure hierarchy
     */
    @Override
    public PDAbstractStructureNode getParent()
    {
        COSDictionary parent = this.getCOSObject()
                .getDictionaryObject(COSName.P, COSDictionary.class);
        if (nonNull(parent))
        {
            return PDAbstractStructureNode.create(parent);
        }
        return null;
    }

    public final void setParent(PDAbstractStructureNode structureNode)
    {
        this.getCOSObject().setItem(COSName.P, structureNode);
    }

    /**
     * @return the element identifier
     */
    public String getElementIdentifier()
    {
        return this.getCOSObject().getString(COSName.ID);
    }

    public void setElementIdentifier(String id)
    {
        this.getCOSObject().setString(COSName.ID, id);
    }

    /**
     * @return the page on which some or all of the content items designated by the K entry shall be
     * rendered
     */
    public PDPage getPage()
    {
        COSDictionary page = this.getCOSObject()
                .getDictionaryObject(COSName.PG, COSDictionary.class);
        if (nonNull(page))
        {
            return new PDPage(page);
        }
        return null;
    }

    public void setPage(PDPage page)
    {
        this.getCOSObject().setItem(COSName.PG, page);
    }

    /**
     * @return he attributes together with their revision numbers (A).
     */
    public Revisions<PDAttributeObject> getAttributes()
    {
        Revisions<PDAttributeObject> attributes = new Revisions<>();
        COSBase a = this.getCOSObject().getDictionaryObject(COSName.A);
        if (a instanceof COSArray aa)
        {
            Iterator<COSBase> it = aa.iterator();
            PDAttributeObject ao = null;
            while (it.hasNext())
            {
                COSBase item = it.next().getCOSObject();
                if (item instanceof COSDictionary itemDic)
                {
                    ao = PDAttributeObject.create(itemDic);
                    ao.setStructureElement(this);
                    attributes.addObject(ao, 0);
                }
                else if (item instanceof COSInteger itemInt)
                {
                    attributes.setRevisionNumber(ao, itemInt.intValue());
                }
            }
        }
        if (a instanceof COSDictionary aDic)
        {
            PDAttributeObject ao = PDAttributeObject.create(aDic);
            ao.setStructureElement(this);
            attributes.addObject(ao, 0);
        }
        return attributes;
    }

    /**
     * Sets the attributes together with their revision numbers (A).
     */
    public void setAttributes(Revisions<PDAttributeObject> attributes)
    {
        COSName key = COSName.A;
        if ((attributes.size() == 1) && (attributes.getRevisionNumber(0) == 0))
        {
            PDAttributeObject attributeObject = attributes.getObject(0);
            attributeObject.setStructureElement(this);
            this.getCOSObject().setItem(key, attributeObject);
            return;
        }
        COSArray array = new COSArray();
        for (int i = 0; i < attributes.size(); i++)
        {
            PDAttributeObject attributeObject = attributes.getObject(i);
            attributeObject.setStructureElement(this);
            int revisionNumber = attributes.getRevisionNumber(i);
            if (revisionNumber < 0)
            {
                throw new IllegalArgumentException("The revision number shall be > -1");
            }
            array.add(attributeObject);
            array.add(COSInteger.get(revisionNumber));
        }
        this.getCOSObject().setItem(key, array);
    }

    /**
     * Adds an attribute object.
     *
     * @param attributeObject the attribute object
     */
    public void addAttribute(PDAttributeObject attributeObject)
    {
        COSName key = COSName.A;
        attributeObject.setStructureElement(this);
        COSBase a = this.getCOSObject().getDictionaryObject(key);
        COSArray array;
        if (a instanceof COSArray)
        {
            array = (COSArray) a;
        }
        else
        {
            array = new COSArray();
            if (a != null)
            {
                array.add(a);
                array.add(COSInteger.get(0));
            }
        }
        this.getCOSObject().setItem(key, array);
        array.add(attributeObject);
        array.add(COSInteger.get(this.getRevisionNumber()));
    }

    /**
     * Removes an attribute object.
     *
     * @param attributeObject the attribute object
     */
    public void removeAttribute(PDAttributeObject attributeObject)
    {
        COSName key = COSName.A;
        COSBase a = this.getCOSObject().getDictionaryObject(key);
        if (a instanceof COSArray array)
        {
            array.remove(attributeObject.getCOSObject());
            if ((array.size() == 2) && (array.getInt(1) == 0))
            {
                this.getCOSObject().setItem(key, array.getObject(0));
            }
        }
        else
        {
            if (attributeObject.getCOSObject().equals(a.getCOSObject()))
            {
                this.getCOSObject().removeItem(key);
            }
        }
        attributeObject.setStructureElement(null);
    }

    /**
     * Updates the revision number for the given attribute object.
     *
     * @param attributeObject the attribute object
     */
    public void attributeChanged(PDAttributeObject attributeObject)
    {
        COSName key = COSName.A;
        COSBase a = this.getCOSObject().getDictionaryObject(key);
        if (a instanceof COSArray array)
        {
            for (int i = 0; i < array.size(); i++)
            {
                COSBase entry = array.getObject(i);
                if (entry.equals(attributeObject.getCOSObject()))
                {
                    COSBase next = array.get(i + 1);
                    if (next instanceof COSInteger)
                    {
                        array.set(i + 1, COSInteger.get(this.getRevisionNumber()));
                    }
                }
            }
        }
        else
        {
            this.getCOSObject()
                    .setItem(key, new COSArray(a, COSInteger.get(this.getRevisionNumber())));
        }
    }

    /**
     * @return the class names together with their revision numbers (C).
     */
    public Revisions<String> getClassNames()
    {
        COSName key = COSName.C;
        Revisions<String> classNames = new Revisions<>();
        COSBase c = this.getCOSObject().getDictionaryObject(key);
        if (c instanceof COSName name)
        {
            classNames.addObject(name.getName(), 0);
        }
        if (c instanceof COSArray array)
        {
            Iterator<COSBase> it = array.iterator();
            String className = null;
            while (it.hasNext())
            {
                COSBase item = it.next().getCOSObject();
                if (item instanceof COSName name)
                {
                    className = name.getName();
                    classNames.addObject(className, 0);
                }
                else if (item instanceof COSInteger integer)
                {
                    classNames.setRevisionNumber(className, integer.intValue());
                }
            }
        }
        return classNames;
    }

    /**
     * Sets the class names together with their revision numbers (C).
     *
     * @param classNames the class names
     */
    public void setClassNames(Revisions<String> classNames)
    {
        if (classNames == null)
        {
            return;
        }
        COSName key = COSName.C;
        if ((classNames.size() == 1) && (classNames.getRevisionNumber(0) == 0))
        {
            String className = classNames.getObject(0);
            this.getCOSObject().setName(key, className);
            return;
        }
        COSArray array = new COSArray();
        for (int i = 0; i < classNames.size(); i++)
        {
            String className = classNames.getObject(i);
            int revisionNumber = classNames.getRevisionNumber(i);
            if (revisionNumber < 0)
            {
                throw new IllegalArgumentException("The revision number shall be > -1");
            }
            array.add(COSName.getPDFName(className));
            array.add(COSInteger.get(revisionNumber));
        }
        this.getCOSObject().setItem(key, array);
    }

    public void addClassName(String className)
    {
        if (className == null)
        {
            return;
        }
        COSName key = COSName.C;
        COSBase c = this.getCOSObject().getDictionaryObject(key);
        COSArray array;
        if (c instanceof COSArray a)
        {
            array = a;
        }
        else
        {
            array = new COSArray();
            if (c != null)
            {
                array.add(c);
                array.add(COSInteger.get(0));
            }
        }
        this.getCOSObject().setItem(key, array);
        array.add(COSName.getPDFName(className));
        array.add(COSInteger.get(this.getRevisionNumber()));
    }

    public void removeClassName(String className)
    {
        if (className == null)
        {
            return;
        }
        COSName key = COSName.C;
        COSBase c = this.getCOSObject().getDictionaryObject(key);
        COSName name = COSName.getPDFName(className);
        if (c instanceof COSArray array)
        {
            array.remove(name);
            if ((array.size() == 2) && (array.getInt(1) == 0))
            {
                this.getCOSObject().setItem(key, array.getObject(0));
            }
        }
        else
        {
            if (name.equals(c.getCOSObject()))
            {
                this.getCOSObject().removeItem(key);
            }
        }
    }

    /**
     * @return the revision number
     */
    public int getRevisionNumber()
    {
        return this.getCOSObject().getInt(COSName.R, 0);
    }

    public void setRevisionNumber(int revisionNumber)
    {
        requireArg(revisionNumber >= 0, "The revision number must be >= 0");
        this.getCOSObject().setInt(COSName.R, revisionNumber);
    }

    /**
     * Increments th revision number.
     */
    public void incrementRevisionNumber()
    {
        this.setRevisionNumber(this.getRevisionNumber() + 1);
    }

    /**
     * @return the title
     */
    public String getTitle()
    {
        return this.getCOSObject().getString(COSName.T);
    }

    public void setTitle(String title)
    {
        this.getCOSObject().setString(COSName.T, title);
    }

    /**
     * @return the language
     */
    public String getLanguage()
    {
        return this.getCOSObject().getString(COSName.LANG);
    }

    public void setLanguage(String language)
    {
        this.getCOSObject().setString(COSName.LANG, language);
    }

    /**
     * @return the alternate description
     */
    public String getAlternateDescription()
    {
        return this.getCOSObject().getString(COSName.ALT);
    }

    public void setAlternateDescription(String alternateDescription)
    {
        this.getCOSObject().setString(COSName.ALT, alternateDescription);
    }

    /**
     * @return the expanded form
     */
    public String getExpandedForm()
    {
        return this.getCOSObject().getString(COSName.E);
    }

    public void setExpandedForm(String expandedForm)
    {
        this.getCOSObject().setString(COSName.E, expandedForm);
    }

    /**
     * @return the actual text
     */
    public String getActualText()
    {
        return this.getCOSObject().getString(COSName.ACTUAL_TEXT);
    }

    public void setActualText(String actualText)
    {
        this.getCOSObject().setString(COSName.ACTUAL_TEXT, actualText);
    }

    /**
     * @return the standard structure type, the actual structure type is mapped to in the role map
     */
    public String getStandardStructureType()
    {
        COSName type = this.getCOSObject().getCOSName(COSName.S);
        if (nonNull(type))
        {
            COSName mapped = getRoleMap().getCOSName(type);
            if (nonNull(mapped))
            {
                return mapped.getName();
            }

        }
        return ofNullable(type).map(COSName::getName).orElse("");
    }

    /**
     * Appends a marked-content sequence kid.
     */
    public void appendKid(PDMarkedContent markedContent)
    {
        if (nonNull(markedContent))
        {
            this.appendKid(new PDMarkedContentIdentifier(COSInteger.get(markedContent.getMCID())));
        }
    }


    /**
     * @return the structure tree root
     */
    private PDStructureTreeRoot getStructureTreeRoot()
    {
        PDAbstractStructureNode parent = this.getParent();
        while (parent instanceof PDStructureElement element)
        {
            parent = element.getParent();
        }
        if (parent instanceof PDStructureTreeRoot root)
        {
            return root;
        }
        return null;
    }

    /**
     * @return the role map
     */
    private COSDictionary getRoleMap()
    {
        return ofNullable(this.getStructureTreeRoot()).map(PDStructureTreeRoot::getRoleMap)
                .orElse(null);
    }

}
