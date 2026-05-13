/*
 * Created on 12/05/26
 * Copyright 2026 by Sober Lemur S.r.l.
 * Copyright 2026 Sejda BV
 *
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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Pre-order depth-first iterator over all {@link StructureElement}s in a structure tree, excluding
 * the root. Leaf elements ({@link PDMarkedContentIdentifier}, {@link PDMarkedContentReference},
 * {@link PDObjectReference}) are included.
 * <p>
 * <b>Eager:</b> the entire tree is traversed at construction time and all elements are collected
 * into a {@link LinkedHashSet} before the first call to {@link #next()}. This has two
 * consequences:
 * <ul>
 *   <li>Memory usage is proportional to the total number of nodes in the tree.</li>
 *   <li>Cycles in malformed documents are automatically prevented: {@link LinkedHashSet#add}
 *       returns {@code false} for a node that is already present, so its subtree is never
 *       recursed into a second time.</li>
 * </ul>
 *
 * @author Andrea Vacondio
 */
public class PDStructureTreeIterator implements Iterator<StructureElement>
{
    private final LinkedHashSet<StructureElement> elements = new LinkedHashSet<>();

    public PDStructureTreeIterator(PDStructureTreeRoot root)
    {
        if (nonNull(root))
        {
            enqueue(root.getKids());
        }
    }

    private void enqueue(List<StructureElement> kids)
    {
        for (StructureElement kid : kids)
        {
            if (elements.add(kid))
            {
                enqueue(kid.getKids());
            }
        }
    }

    @Override
    public boolean hasNext()
    {
        return !elements.isEmpty();
    }

    @Override
    public StructureElement next()
    {
        return elements.removeFirst();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
