package org.sejda.sambox.pdmodel.documentinterchange.logicalstructure;
/*
 * Copyright 2026 Sober Lemur S.r.l.
 * Copyright 2026 Sejda BV
 *
 * Created 11/05/26
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSInteger;

/**
 * @author Andrea Vacondio
 */
public class PDMarkedContentIdentifier implements StructureElement
{

    private PDAbstractStructureNode parent;
    private final COSInteger id;

    /**
     * Creates an MDI not yet assigned to a tree, so no parent yet
     *
     * @param id
     */
    public PDMarkedContentIdentifier(COSInteger id)
    {
        requireNotNullArg(id, "Marked content identifier cannot be null");
        this.id = id;
    }

    public PDMarkedContentIdentifier(COSInteger id, PDAbstractStructureNode parent)
    {
        requireNotNullArg(parent, "Parent cannot be null");
        this(id);
        this.parent = parent;
    }

    @Override
    public COSBase getCOSObject()
    {
        return id.getCOSObject();
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
