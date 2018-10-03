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
import static org.sejda.commons.util.RequireUtils.requireArg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.pdmodel.PDDocumentCatalog;

/**
 * Represents the page label dictionary of a document.
 * 
 * @author Igor Podolskiy
 */
public class PDPageLabels implements COSObjectable
{

    private Map<Integer, PDPageLabelRange> labels = new TreeMap<>();

    /**
     * Creates an empty page label dictionary for the given document.
     * 
     * <p>
     * Note that the page label dictionary won't be automatically added to the document; you will still need to do it
     * manually (see {@link PDDocumentCatalog#setPageLabels(PDPageLabels)}.
     * </p>
     * 
     * @see PDDocumentCatalog#setPageLabels(PDPageLabels)
     */
    public PDPageLabels()
    {
        PDPageLabelRange defaultRange = new PDPageLabelRange();
        defaultRange.setStyle(PDPageLabelRange.STYLE_DECIMAL);
        labels.put(0, defaultRange);
    }

    /**
     * Creates an page label dictionary for a document using the information in the given COS dictionary.
     * 
     * <p>
     * Note that the page label dictionary won't be automatically added to the document; you will still need to do it
     * manually (see {@link PDDocumentCatalog#setPageLabels(PDPageLabels)}.
     * </p>
     * 
     * @param dict an existing page label dictionary
     * @see PDDocumentCatalog#setPageLabels(PDPageLabels)
     * @throws IOException If something goes wrong during the number tree conversion.
     */
    public PDPageLabels(COSDictionary dict) throws IOException
    {
        if (nonNull(dict))
        {
            findLabels(new PDNumberTreeNode(dict, COSDictionary.class));
        }
    }

    private void findLabels(PDNumberTreeNode node) throws IOException
    {
        if (node.getKids() != null)
        {
            List<PDNumberTreeNode> kids = node.getKids();
            for (PDNumberTreeNode kid : kids)
            {
                findLabels(kid);
            }
        }
        else if (node.getNumbers() != null)
        {
            Map<Integer, COSObjectable> numbers = node.getNumbers();
            for (Entry<Integer, COSObjectable> i : numbers.entrySet())
            {
                if (i.getKey() >= 0)
                {
                    labels.put(i.getKey(), new PDPageLabelRange((COSDictionary) i.getValue()));
                }
            }
        }
    }

    /**
     * Returns the number of page label ranges.
     * 
     * <p>
     * This will be always &gt;= 1, as the required default entry for the page range starting at the first page is added
     * automatically by this implementation (see PDF32000-1:2008, p. 375).
     * </p>
     * 
     * @return the number of page label ranges.
     */
    public int getPageRangeCount()
    {
        return labels.size();
    }

    /**
     * Returns the page label range starting at the given page, or {@code null} if no such range is defined.
     * 
     * @param startPage the 0-based page index representing the start page of the page range the item is defined for.
     * @return the page label range or {@code null} if no label range is defined for the given start page.
     */
    public PDPageLabelRange getPageLabelRange(int startPage)
    {
        return labels.get(startPage);
    }

    /**
     * Sets the page label range beginning at the specified start page.
     * 
     * @param startPage the 0-based index of the page representing the start of the page label range.
     * @param item the page label item to set.
     * @throws IllegalArgumentException if the startPage parameter is &lt; 0.
     */
    public void setLabelItem(int startPage, PDPageLabelRange item)
    {
        requireArg(startPage >= 0, "Cannot set a label starting from a negative page number");
        labels.put(startPage, item);
    }

    public Map<Integer, PDPageLabelRange> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    @Override
    public COSBase getCOSObject()
    {
        COSDictionary dict = new COSDictionary();
        COSArray arr = new COSArray();
        for (Entry<Integer, PDPageLabelRange> i : labels.entrySet())
        {
            arr.add(COSInteger.get(i.getKey()));
            arr.add(i.getValue());
        }
        dict.setItem(COSName.NUMS, arr);
        return dict;
    }

}
