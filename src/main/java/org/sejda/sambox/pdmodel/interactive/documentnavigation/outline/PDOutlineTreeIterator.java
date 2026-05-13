package org.sejda.sambox.pdmodel.interactive.documentnavigation.outline;
/*
 * Copyright 2022 Sober Lemur S.r.l.
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

import static java.util.Objects.nonNull;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * An iterator over the whole outline tree
 *
 * @author Andrea Vacondio
 */
public class PDOutlineTreeIterator implements Iterator<PDOutlineItem>
{
    private final LinkedHashSet<PDOutlineItem> elements = new LinkedHashSet<>();

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
            if (elements.add(item))
            {
                if (item.hasChildren())
                {
                    enqueueChildren(item.children());
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
        return elements.removeFirst();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
