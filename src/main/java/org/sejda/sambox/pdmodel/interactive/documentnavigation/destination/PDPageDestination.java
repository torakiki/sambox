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
package org.sejda.sambox.pdmodel.interactive.documentnavigation.destination;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageTree;
import org.sejda.sambox.util.Matrix;

/**
 * This represents a destination to a page, see subclasses for specific parameters.
 *
 * @author Ben Litchfield
 */
public abstract class PDPageDestination extends PDDestination
{
    /**
     * Storage for the page destination.
     */
    protected final COSArray array;

    protected PDPageDestination()
    {
        this.array = new COSArray();
    }

    /**
     * @param array A page destination array.
     */
    protected PDPageDestination(COSArray array)
    {
        this.array = array;
    }

    /**
     * This will get the page for this destination. A page destination can either reference a page (for a local
     * destination) or a page number (when doing a remote destination to another PDF). If this object is referencing by
     * page number then this method will return null and {@link #getPageNumber()} should be used.
     *
     * @return The page for this destination.
     */
    public PDPage getPage()
    {
        if (array.size() > 0)
        {
            COSBase page = array.getObject(0);
            if (page instanceof COSDictionary)
            {
                return new PDPage((COSDictionary) page);
            }
        }
        return null;
    }

    /**
     * Set the page for a local destination. For an external destination, call {@link #setPageNumber(int)
     * setPageNumber(int pageNumber)}.
     *
     * @param page The page for a local destination.
     */
    public void setPage(PDPage page)
    {
        array.set(0, page);
    }

    /**
     * This will get the page number for this destination. A page destination can either reference a page (for a local
     * destination) or a page number (when doing a remote destination to another PDF). If this object is referencing by
     * page number then this method will return that number, otherwise -1 will be returned.
     *
     * @return The zero-based page number for this destination.
     */
    public int getPageNumber()
    {
        if (array.size() > 0)
        {
            COSBase page = array.getObject(0);
            if (page instanceof COSNumber)
            {
                return ((COSNumber) page).intValue();
            }
        }
        return -1;
    }

    /**
     * Returns the page number for this destination, regardless of whether this is a page number or a reference to a
     * page.
     *
     * @see org.sejda.sambox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
     * @return the 0-based page number, or -1 if the destination type is unknown.
     */
    public int retrievePageNumber()
    {
        if (array.size() > 0)
        {
            COSBase page = array.getObject(0);
            if (page instanceof COSNumber)
            {
                return ((COSNumber) page).intValue();
            }
            if (page instanceof COSDictionary)
            {
                return indexOfPageTree((COSDictionary) page);
            }
        }
        return -1;
    }

    // climb up the page tree up to the top to be able to call PageTree.indexOf for a page dictionary
    private int indexOfPageTree(COSDictionary pageDict)
    {
        COSDictionary parent = pageDict;
        while (parent.getDictionaryObject(COSName.PARENT, COSName.P) instanceof COSDictionary)
        {
            parent = (COSDictionary) parent.getDictionaryObject(COSName.PARENT, COSName.P);
        }
        if (parent.containsKey(COSName.KIDS) && COSName.PAGES.equals(parent.getItem(COSName.TYPE)))
        {
            // now parent is the highest pages node
            PDPageTree pages = new PDPageTree(parent);
            return pages.indexOf(new PDPage(pageDict));
        }
        return -1;
    }

    /**
     * Set the page number for a remote destination. For an internal destination, call {@link #setPage(PDPage)
     * setPage(PDPage page)}.
     *
     * @param pageNumber The page for a remote destination.
     */
    public void setPageNumber(int pageNumber)
    {
        array.set(0, COSInteger.get(pageNumber));
    }

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos object that matches this Java object.
     */
    @Override
    public COSArray getCOSObject()
    {
        return array;
    }

    /**
     * Transforms the destination target coordinates based on the given transformation
     *
     * @param transformation
     */
    public abstract void transform(Matrix transformation);

}
