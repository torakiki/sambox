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
package org.sejda.sambox.pdmodel.interactive.form;

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The field tree allowing a post-order iteration of the nodes.
 */
public class PDFieldTree implements Iterable<PDField>
{
    private final PDAcroForm acroForm;

    /**
     * @param acroForm the AcroForm containing the fields.
     */
    public PDFieldTree(PDAcroForm acroForm)
    {
        requireNotNullArg(acroForm, "root cannot be null");
        this.acroForm = acroForm;
    }

    /**
     * @return an iterator which walks all fields in the tree, post-order.
     */
    @Override
    public Iterator<PDField> iterator()
    {
        return new FieldIterator(acroForm);
    }

    /**
     * Iterator which walks all fields in the tree, post-order.
     */
    private static final class FieldIterator implements Iterator<PDField>
    {
        private final Queue<PDField> queue = new ArrayDeque<>();

        private FieldIterator(PDAcroForm form)
        {
            for (PDField field : form.getFields())
            {
                enqueueKids(field);
            }
        }

        @Override
        public boolean hasNext()
        {
            return !queue.isEmpty();
        }

        @Override
        public PDField next()
        {
            return queue.remove();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private void enqueueKids(PDField node)
        {
            if (node instanceof PDNonTerminalField)
            {
                List<PDField> kids = ((PDNonTerminalField) node).getChildren();
                for (PDField kid : kids)
                {
                    enqueueKids(kid);
                }
            }
            queue.add(node);
        }
    }

    /**
     * @return a pre order sequential {@code Stream} over the fields of this form.
     */
    public Stream<PDField> stream()
    {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new PreOrderIterator(acroForm), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    /**
     * Iterator which walks all the fields pre order
     */
    private final class PreOrderIterator implements Iterator<PDField>
    {
        private final Deque<PDField> queue = new ArrayDeque<>();

        private PreOrderIterator(PDAcroForm form)
        {
            for (PDField field : form.getFields())
            {
                enqueueKids(field);
            }
        }

        private void enqueueKids(PDField node)
        {
            queue.add(node);
            if (node instanceof PDNonTerminalField)
            {
                List<PDField> kids = ((PDNonTerminalField) node).getChildren();
                for (PDField kid : kids)
                {
                    enqueueKids(kid);
                }
            }
        }

        @Override
        public boolean hasNext()
        {
            return !queue.isEmpty();
        }

        @Override
        public PDField next()
        {
            return queue.remove();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
