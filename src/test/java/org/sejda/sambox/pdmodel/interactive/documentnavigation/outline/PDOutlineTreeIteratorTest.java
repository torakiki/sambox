package org.sejda.sambox.pdmodel.interactive.documentnavigation.outline;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/*
 * Copyright 2022 Sober Lemur S.a.s. di Vacondio Andrea
 * Copyright 2022 Sejda BV
 *
 * Created 11/04/22
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
class PDOutlineTreeIteratorTest
{

    @Test
    public void infiniteLoop()
    {
        PDOutlineItem item1 = new PDOutlineItem();
        item1.setTitle("Item1");
        PDOutlineItem item1copy = new PDOutlineItem();
        item1copy.setTitle("Item1");
        PDOutlineItem item2 = new PDOutlineItem();
        item2.setTitle("Item2");
        PDOutlineItem item3 = new PDOutlineItem();
        item3.setTitle("Item3");
        PDOutlineItem item4 = new PDOutlineItem();
        item4.setTitle("Item4");
        PDOutlineItem item5 = new PDOutlineItem();
        item5.setTitle("Item5");

        item1.setNextSibling(item2);
        item1.setNextSibling(item1copy);
        item1copy.setNextSibling(item2);
        item2.setNextSibling(item3);
        item3.setNextSibling(item2);
        item3.addLast(item4);
        item4.addLast(item5);

        PDDocumentOutline outline = new PDDocumentOutline();
        outline.setFirstChild(item1);

        long counter = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new PDOutlineTreeIterator(outline),
                        Spliterator.ORDERED | Spliterator.NONNULL), false).count();

        Assertions.assertEquals(6, counter);
    }

    @Test
    public void noChildren()
    {
        PDOutlineTreeIterator iterator = new PDOutlineTreeIterator(null);
        Assertions.assertFalse(iterator.hasNext());
    }
}