package org.sejda.sambox.pdmodel.interactive.documentnavigation.outline;
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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Queue;

import static java.util.Objects.nonNull;

/**
 * An iterator over the whole outline tree
 *
 * @author Andrea Vacondio
 */
public class PDOutlineTreeIterator implements Iterator<PDOutlineItem>
{
    private final LinkedHashSet<PDOutlineItem> elements = new LinkedHashSet<>();
    private final Queue<PDOutlineItem> queue = new ArrayDeque<>();

    public PDOutlineTreeIterator(PDDocumentOutline outline)
    {
        if (nonNull(outline))
        {
            enqueueChildren(outline.children());
        }
    }

    private void enqueueChildren(Iterable<PDOutlineItem> children)
    {

        for (PDOutlineItem item : children)
        {
            if (!elements.contains(item))
            {
                if (item.hasChildren())
                {
                    elements.add(item);
                    enqueueChildren(item.children());
                }
                else
                {
                    elements.add(item);
                }
            }
        }
    }

    @Override
    public boolean hasNext()
    {
        return !elements.isEmpty();
    }

    @Override
    public PDOutlineItem next()
    {
        PDOutlineItem next = elements.stream().findFirst().orElseThrow(NoSuchElementException::new);
        elements.remove(next);
        return next;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
