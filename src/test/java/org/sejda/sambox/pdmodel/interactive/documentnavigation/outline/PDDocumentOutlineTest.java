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
package org.sejda.sambox.pdmodel.interactive.documentnavigation.outline;

import static java.util.stream.StreamSupport.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 */
public class PDDocumentOutlineTest
{
    /**
     * see PDF 32000-1:2008 table 152
     */
    @Test
    public void outlinesCountShouldNotBeNegative()
    {
        PDDocumentOutline outline = new PDDocumentOutline();
        PDOutlineItem firstLevelChild = new PDOutlineItem();
        outline.addLast(firstLevelChild);
        PDOutlineItem secondLevelChild = new PDOutlineItem();
        firstLevelChild.addLast(secondLevelChild);
        assertEquals(0, secondLevelChild.getOpenCount());
        assertEquals(-1, firstLevelChild.getOpenCount());
        assertFalse("Outlines count cannot be " + outline.getOpenCount(),
                outline.getOpenCount() < 0);
    }

    @Test
    public void outlinesCount()
    {
        PDDocumentOutline outline = new PDDocumentOutline();
        PDOutlineItem root = new PDOutlineItem();
        outline.addLast(root);
        assertEquals(1, outline.getOpenCount());
        root.addLast(new PDOutlineItem());
        assertEquals(-1, root.getOpenCount());
        assertEquals(1, outline.getOpenCount());
        root.addLast(new PDOutlineItem());
        assertEquals(-2, root.getOpenCount());
        assertEquals(1, outline.getOpenCount());
        root.openNode();
        assertEquals(2, root.getOpenCount());
        assertEquals(3, outline.getOpenCount());
    }

    private PDOutlineItem createOutlineItem(String title)
    {
        return createOutlineItem(title, new ArrayList<>());
    }

    private PDOutlineItem createOutlineItem(String title, List<String> children)
    {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        item.openNode();

        for (String child : children)
        {
            item.addLast(createOutlineItem(child));
        }

        return item;
    }

    private PDDocumentOutline createTestOutline()
    {
        PDDocumentOutline outline = new PDDocumentOutline();
        outline.addLast(
                createOutlineItem("Europe", Arrays.asList("Netherlands", "Italy", "France")));
        outline.addLast(createOutlineItem("Asia", Arrays.asList("Japan", "India", "Korea")));
        outline.addLast(createOutlineItem("Antarctica"));

        return outline;
    }

    private PDOutlineItem find(PDDocumentOutline outline, String title)
    {
        return stream(Spliterators.spliteratorUnknownSize(new PDOutlineTreeIterator(outline),
                Spliterator.ORDERED | Spliterator.NONNULL), false).filter(
                item -> title.equals(item.getTitle())).findFirst().orElse(null);
    }

    private String asString(PDOutlineNode node)
    {
        return asString(node, 0);
    }

    private String asString(PDOutlineNode node, int level)
    {
        StringBuilder sb = new StringBuilder();
        if (node instanceof PDOutlineItem item)
        {
            sb.append(item.getTitle());
        }

        if (node.hasChildren())
        {
            sb.append(" [");

            for (PDOutlineItem child : node.children())
            {
                sb.append(asString(child, level + 1)).append(" ");
            }

            sb.append("]");
        }

        return sb.toString();
    }

    private void assertOutline(PDDocumentOutline outline, String expected)
    {
        assertEquals(expected.trim(), asString(outline).trim());
    }

    private void assertOpenCount(PDDocumentOutline outline, String title, int expected)
    {
        assertEquals(expected, find(outline, title).getOpenCount());
    }

    private void assertOpenCount(PDDocumentOutline outline, int expected)
    {
        assertEquals(expected, outline.getOpenCount());
    }

    @Test
    public void deleteTopLevelItem()
    {
        PDDocumentOutline outline = createTestOutline();
        assertOpenCount(outline, 3);

        find(outline, "Antarctica").delete();

        assertOutline(outline, "[Europe [Netherlands Italy France ] Asia [Japan India Korea ] ]");
        assertOpenCount(outline, 2);

    }

    @Test
    public void deleteTopLevelItemWithChildren()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Antarctica").delete();
        find(outline, "Asia").delete();

        assertOutline(outline, "[Europe [Netherlands Italy France ] ]");
        assertOpenCount(outline, 1);
        assertOpenCount(outline, "Europe", -3);
    }

    @Test
    public void deleteFirstChild()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Netherlands").delete();

        assertOutline(outline, "[Europe [Italy France ] Asia [Japan India Korea ] Antarctica ]");
    }

    @Test
    public void deleteMiddleChild()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Italy").delete();

        assertOutline(outline,
                "[Europe [Netherlands France ] Asia [Japan India Korea ] Antarctica ]");
        assertOpenCount(outline, "Europe", -2);
    }

    @Test
    public void deleteLastChild()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "France").delete();

        assertOutline(outline,
                "[Europe [Netherlands Italy ] Asia [Japan India Korea ] Antarctica ]");
    }

    @Test
    public void insertFirstChild()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Europe").addAtPosition(createOutlineItem("Spain"), 0);

        assertOutline(outline,
                "[Europe [Spain Netherlands Italy France ] Asia [Japan India Korea ] Antarctica ]");
        assertOpenCount(outline, "Europe", -4);
    }

    @Test
    public void insertLastChild()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Europe").addAtPosition(createOutlineItem("Spain"), 3);

        assertOutline(outline,
                "[Europe [Netherlands Italy France Spain ] Asia [Japan India Korea ] Antarctica ]");
    }

    @Test
    public void insertLastChildIndexLargerThanLast()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Europe").addAtPosition(createOutlineItem("Spain"), 300);

        assertOutline(outline,
                "[Europe [Netherlands Italy France Spain ] Asia [Japan India Korea ] Antarctica ]");
    }

    @Test
    public void insertSecondChild()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Europe").addAtPosition(createOutlineItem("Spain"), 1);

        assertOutline(outline,
                "[Europe [Netherlands Spain Italy France ] Asia [Japan India Korea ] Antarctica ]");
    }

    @Test
    public void insertThirdChild()
    {
        PDDocumentOutline outline = createTestOutline();
        find(outline, "Europe").addAtPosition(createOutlineItem("Spain"), 2);

        assertOutline(outline,
                "[Europe [Netherlands Italy Spain France ] Asia [Japan India Korea ] Antarctica ]");
    }

    @Test
    public void exploresAllNodes()
    {
        assertEquals(9, stream(createTestOutline().nodes().spliterator(), false).count());
    }
}
