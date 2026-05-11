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
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

/**
 * A marked-content reference.
 *
 * @author Johannes Koch
 */
public class PDMarkedContentReference extends PDDictionaryWrapper implements StructureElement
{
    public static final String TYPE = "MCR";

    private PDAbstractStructureNode parent;

    public PDMarkedContentReference(PDAbstractStructureNode parent)
    {
        this(COSDictionary.of(COSName.TYPE, COSName.getPDFName(TYPE)), parent);
    }

    public PDMarkedContentReference(COSDictionary dictionary, PDAbstractStructureNode parent)
    {
        requireNotNullArg(dictionary, "Dictionary cannot be null");
        requireNotNullArg(parent, "Parent cannot be null");
        super(dictionary);
        this.parent = parent;
    }

    /**
     * @return The page object of the page on which the object shall be rendered
     */
    public PDPage getPage()
    {
        COSDictionary pg = this.getCOSObject().getDictionaryObject(COSName.PG, COSDictionary.class);
        if (nonNull(pg))
        {
            return new PDPage(pg);
        }
        return null;
    }

    /**
     * @param page the page
     */
    public void setPage(PDPage page)
    {
        this.getCOSObject().setItem(COSName.PG, page);
    }

    /**
     * @return the marked content identifier
     */
    public int getMCID()
    {
        return this.getCOSObject().getInt(COSName.MCID);
    }

    /**
     * @param mcid the marked content identifier
     */
    public void setMCID(int mcid)
    {
        this.getCOSObject().setInt(COSName.MCID, mcid);
    }

    @Override
    public PDAbstractStructureNode getParent()
    {
        return parent;
    }

    @Override
    public void setParent(PDAbstractStructureNode parent)
    {
        this.parent = parent;
    }

}
