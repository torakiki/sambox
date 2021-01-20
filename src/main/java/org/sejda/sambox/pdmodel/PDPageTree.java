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
package org.sejda.sambox.pdmodel;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The page tree, which defines the ordering of pages in the document in an efficient manner.
 *
 * @author John Hewson
 */
public class PDPageTree implements COSObjectable, Iterable<PDPage>
{
    private static final Logger LOG = LoggerFactory.getLogger(PDPageTree.class);

    private final COSDictionary root;
    private final PDDocument document;

    /**
     * Constructor for embedding.
     */
    public PDPageTree()
    {
        root = new COSDictionary();
        root.setItem(COSName.TYPE, COSName.PAGES);
        root.setItem(COSName.KIDS, new COSArray());
        root.setItem(COSName.COUNT, COSInteger.ZERO);
        document = null;
    }

    /**
     * Constructor for reading.
     *
     * @param root A page tree root.
     */
    public PDPageTree(COSDictionary root)
    {
        this(root, null);
    }

    /**
     * Constructor for reading.
     *
     * @param root A page tree root.
     * @param document The document which contains "root".
     */
    PDPageTree(COSDictionary root, PDDocument document)
    {
        requireNotNullArg(root, "Page tree root cannot be null");
        // repair bad PDFs which contain a Page dict instead of a page tree, see PDFBOX-3154
        if (COSName.PAGE.equals(root.getCOSName(COSName.TYPE)))
        {
            COSArray kids = new COSArray();
            kids.add(root);
            this.root = new COSDictionary();
            this.root.setItem(COSName.KIDS, kids);
            this.root.setInt(COSName.COUNT, 1);
        }
        else
        {
            this.root = root;
        }
        root.setItem(COSName.TYPE, COSName.PAGES);
        this.document = document;
    }

    /**
     * Similar to {@link #getInheritableAttribute(COSDictionary, COSName)} but also checks the returned element 
     * matches the expected type class
     * @param node
     * @param key
     * @param clazz expected type class
     * @param <T>
     * @return
     */
    public static <T extends COSBase> COSBase getInheritableAttribute(COSDictionary node, COSName key, Class<T> clazz) {
        COSBase result = getInheritableAttribute(node, key);
        if(clazz.isInstance(result)) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Returns the given attribute, inheriting from parent tree nodes if necessary.
     *
     * @param node page object
     * @param key the key to look up
     * @return COS value for the given key
     */
    public static COSBase getInheritableAttribute(COSDictionary node, COSName key)
    {
        COSBase value = node.getDictionaryObject(key);
        if (value != null)
        {
            return value;
        }

        COSDictionary parent = node.getDictionaryObject(COSName.PARENT, COSName.P,
                COSDictionary.class);
        if (parent != null)
        {
            return getInheritableAttribute(parent, key);
        }

        return null;
    }

    /**
     * Returns an iterator which walks all pages in the tree, in order.
     */
    @Override
    public Iterator<PDPage> iterator()
    {
        return new PageIterator(root);
    }

    /**
     * @return a sequential {@code Stream} over the pages of this page tree.
     */
    public Stream<PDPage> stream()
    {
        return StreamSupport.stream(Spliterators.spliterator(iterator(), getCount(),
                Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    /**
     * @return a sequential {@code Stream} over the nodes of this page tree.
     */
    public Stream<COSDictionary> streamNodes()
    {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new NodesIterator(root),
                Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    /**
     * Helper to get kids from malformed PDFs.
     * 
     * @param node page tree node
     * @return list of kids
     */
    private List<COSDictionary> getKids(COSDictionary node)
    {
        COSArray kids = node.getDictionaryObject(COSName.KIDS, COSArray.class);
        if (nonNull(kids))
        {
            // we collect only non null, non COSNull COSDictionary kids
            return kids.stream().map(COSBase::getCOSObject).filter(i -> i != COSNull.NULL)
                    .filter(Objects::nonNull).filter(n -> n instanceof COSDictionary)
                    .map(n -> (COSDictionary) n).collect(toList());
        }
        return new ArrayList<>();
    }

    /**
     * Iterator which walks all pages in the tree, in order.
     */
    private final class PageIterator implements Iterator<PDPage>
    {
        private final Queue<COSDictionary> queue = new ArrayDeque<>();

        private PageIterator(COSDictionary node)
        {
            enqueueKids(node);
        }

        private void enqueueKids(COSDictionary node)
        {
            if (isPageTreeNode(node))
            {
                getKids(node).forEach(this::enqueueKids);
            }
            else
            {
                queue.add(node);
            }
        }

        @Override
        public boolean hasNext()
        {
            return !queue.isEmpty();
        }

        @Override
        public PDPage next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            COSDictionary next = queue.poll();

            sanitizeType(next);

            ResourceCache resourceCache = document != null ? document.getResourceCache() : null;
            return new PDPage(next, resourceCache);
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Iterator which walks all the nodes in the tree.
     */
    private final class NodesIterator implements Iterator<COSDictionary>
    {
        private final Queue<COSDictionary> queue = new ArrayDeque<>();

        private NodesIterator(COSDictionary node)
        {
            enqueueKids(node);
        }

        private void enqueueKids(COSDictionary node)
        {
            queue.add(node);
            if (isPageTreeNode(node))
            {
                getKids(node).forEach(this::enqueueKids);
            }
        }

        @Override
        public boolean hasNext()
        {
            return !queue.isEmpty();
        }

        @Override
        public COSDictionary next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            return queue.poll();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the page at the given index.
     *
     * @param index zero-based index
     */
    public PDPage get(int index)
    {
        PageAndPageTreeParent res = get(index + 1, root, 0, null);
        COSDictionary dict = res.node;

        sanitizeType(dict);

        ResourceCache resourceCache = document != null ? document.getResourceCache() : null;
        return new PDPage(dict, resourceCache, res.parent);
    }

    private static void sanitizeType(COSDictionary dictionary)
    {
        if (isNull(dictionary.getCOSName(COSName.TYPE)))
        {
            LOG.warn("Missing required 'Page' type for page");
            dictionary.setName(COSName.TYPE, COSName.PAGE.getName());
        }
        COSName type = dictionary.getCOSName(COSName.TYPE);
        if (!COSName.PAGE.equals(type))
        {
            LOG.error("Expected 'Page' but found '{}'", type.getName());
            dictionary.setName(COSName.TYPE, COSName.PAGE.getName());
        }
    }

    /**
     * Returns the given COS page using a depth-first search.
     *
     * @param pageNum 1-based page number
     * @param node page tree node to search
     * @param encountered number of pages encountered so far
     * @param pageTreeParent the parent node, as determined traversing the page tree top -> down
     * @return COS dictionary of the Page object
     */
    private PageAndPageTreeParent get(int pageNum, COSDictionary node, int encountered, COSDictionary pageTreeParent)
    {
        if (pageNum < 0)
        {
            throw new PageNotFoundException(
                    "Index out of bounds: " + pageNum + " in " + getSourcePath(), pageNum,
                    getSourcePath());
        }

        if (isPageTreeNode(node))
        {
            int count = node.getInt(COSName.COUNT, 0);
            if (pageNum <= encountered + count)
            {
                // it's a kid of this node
                for (COSDictionary kid : getKids(node))
                {
                    // which kid?
                    if (isPageTreeNode(kid))
                    {
                        int kidCount = kid.getInt(COSName.COUNT, 0);
                        if (pageNum <= encountered + kidCount)
                        {
                            // it's this kid
                            return get(pageNum, kid, encountered, node);
                        }
                        encountered += kidCount;
                    }
                    else
                    {
                        // single page
                        encountered++;
                        if (pageNum == encountered)
                        {
                            // it's this page
                            return get(pageNum, kid, encountered, node);
                        }
                    }
                }

                throw new PageNotFoundException(
                        "Unable to find page " + pageNum + " in " + getSourcePath(), pageNum,
                        getSourcePath());
            }
            throw new PageNotFoundException(
                    "Index out of bounds: " + pageNum + " in " + getSourcePath(), pageNum,
                    getSourcePath());
        }
        if (encountered == pageNum)
        {
            return new PageAndPageTreeParent(node, pageTreeParent);
        }

        throw new PageNotFoundException("Unable to find page " + pageNum + " in " + getSourcePath(),
                pageNum, getSourcePath());
    }

    private String getSourcePath()
    {
        return ofNullable(getCOSObject().id()).map(i -> i.ownerIdentifier).orElse("Unknown");
    }

    /**
     * @return true if the node is a page tree node (i.e. and intermediate).
     */
    public static boolean isPageTreeNode(COSDictionary node)
    {
        // some files such as PDFBOX-2250-229205.pdf don't have Pages set as the Type, so we have
        // to check for the presence of Kids too
        return nonNull(node) && (node.getCOSName(COSName.TYPE) == COSName.PAGES
                || node.containsKey(COSName.KIDS));
    }

    /**
     * Returns the index of the given page, or -1 if it does not exist.
     *
     * @param page The page to search for.
     * @return the zero-based index of the given page, or -1 if the page is not found.
     */
    public int indexOf(PDPage page)
    {
        SearchContext context = new SearchContext(page);
        if (findPage(context, root))
        {
            return context.index;
        }
        return -1;
    }

    private boolean findPage(SearchContext context, COSDictionary node)
    {
        for (COSDictionary kid : getKids(node))
        {
            if (context.found)
            {
                break;
            }
            if (isPageTreeNode(kid))
            {
                findPage(context, kid);
            }
            else
            {
                context.visitPage(kid);
            }
        }
        return context.found;
    }

    private static final class SearchContext
    {
        private final COSDictionary searched;
        private int index = -1;
        private boolean found;

        private SearchContext(PDPage page)
        {
            this.searched = page.getCOSObject();
        }

        private void visitPage(COSDictionary current)
        {
            index++;
            found = searched.equals(current);
        }
    }

    /**
     * Returns the number of leaf nodes (page objects) that are descendants of this root within the page tree.
     */
    public int getCount()
    {
        return root.getInt(COSName.COUNT, 0);
    }

    @Override
    public COSDictionary getCOSObject()
    {
        return root;
    }

    /**
     * Removes the page with the given index from the page tree.
     * 
     * @param index zero-based page index
     */
    public void remove(int index)
    {
        PageAndPageTreeParent res = get(index + 1, root, 0, null);
        remove(res.node);
    }

    /**
     * Removes the given page from the page tree.
     *
     * @param page The page to remove.
     */
    public void remove(PDPage page)
    {
        remove(page.getCOSObject());
    }

    /**
     * Removes the given COS page.
     */
    private void remove(COSDictionary node)
    {
        // remove from parent's kids
        COSDictionary parent = node.getDictionaryObject(COSName.PARENT, COSName.P,
                COSDictionary.class);
        COSArray kids = parent.getDictionaryObject(COSName.KIDS, COSArray.class);
        if (kids.removeObject(node))
        {
            // update ancestor counts
            do
            {
                node = node.getDictionaryObject(COSName.PARENT, COSName.P, COSDictionary.class);
                if (node != null)
                {
                    node.setInt(COSName.COUNT, node.getInt(COSName.COUNT) - 1);
                }
            } while (node != null);
        }
    }

    /**
     * Adds the given page to this page tree.
     * 
     * @param page The page to add.
     */
    public void add(PDPage page)
    {
        // set parent
        COSDictionary node = page.getCOSObject();
        node.setItem(COSName.PARENT, root);

        // todo: re-balance tree? (or at least group new pages into tree nodes of e.g. 20)

        // add to parent's kids
        COSArray kids = root.getDictionaryObject(COSName.KIDS, COSArray.class);
        kids.add(node);

        // update ancestor counts
        do
        {
            node = (COSDictionary) node.getDictionaryObject(COSName.PARENT, COSName.P);
            if (node != null)
            {
                node.setInt(COSName.COUNT, node.getInt(COSName.COUNT) + 1);
            }
        } while (node != null);
    }

    /**
     * Insert a page before another page within a page tree.
     *
     * @param newPage the page to be inserted.
     * @param nextPage the page that is to be after the new page.
     * @throws IllegalArgumentException if one attempts to insert a page that isn't part of a page tree.
     */
    public void insertBefore(PDPage newPage, PDPage nextPage)
    {
        COSDictionary nextPageDict = nextPage.getCOSObject();
        COSDictionary parentDict = nextPageDict.getDictionaryObject(COSName.PARENT, COSDictionary.class);
        if(nextPage.getPageTreeParent() != null)
        {
            parentDict = nextPage.getPageTreeParent();
        }
        
        COSArray kids = parentDict.getDictionaryObject(COSName.KIDS, COSArray.class);
        boolean found = false;
        for (int i = 0; i < kids.size(); ++i)
        {
            COSDictionary pageDict = (COSDictionary) kids.getObject(i);
            if (pageDict.equals(nextPage.getCOSObject()))
            {
                kids.add(i, newPage.getCOSObject());
                newPage.getCOSObject().setItem(COSName.PARENT, parentDict);
                found = true;
                break;
            }
        }
        if (!found)
        {
            throw new IllegalArgumentException("attempted to insert before orphan page");
        }
        increaseParents(parentDict);
    }

    /**
     * Insert a page after another page within a page tree.
     *
     * @param newPage the page to be inserted.
     * @param prevPage the page that is to be before the new page.
     * @throws IllegalArgumentException if one attempts to insert a page that isn't part of a page tree.
     */
    public void insertAfter(PDPage newPage, PDPage prevPage)
    {
        COSDictionary prevPageDict = prevPage.getCOSObject();
        COSDictionary parentDict = prevPageDict.getDictionaryObject(COSName.PARENT,
                COSDictionary.class);
        if(prevPage.getPageTreeParent() != null)
        {
            parentDict = prevPage.getPageTreeParent();
        }
        
        COSArray kids = parentDict.getDictionaryObject(COSName.KIDS, COSArray.class);
        boolean found = false;
        for (int i = 0; i < kids.size(); ++i)
        {
            COSDictionary pageDict = (COSDictionary) kids.getObject(i);
            if (pageDict.equals(prevPage.getCOSObject()))
            {
                kids.add(i + 1, newPage.getCOSObject());
                newPage.getCOSObject().setItem(COSName.PARENT, parentDict);
                found = true;
                break;
            }
        }
        if (!found)
        {
            throw new IllegalArgumentException("attempted to insert before orphan page");
        }
        increaseParents(parentDict);
    }

    private void increaseParents(COSDictionary parentDict)
    {
        do
        {
            int cnt = parentDict.getInt(COSName.COUNT);
            parentDict.setInt(COSName.COUNT, cnt + 1);
            parentDict = (COSDictionary) parentDict.getDictionaryObject(COSName.PARENT);
        } while (parentDict != null);
    }

    /**
     * A tuple of a page and the parent found when traversing the page tree top down.
     * NOTE: The page tree parent can be different from page.PARENT (for invalid documents)
     * hence the need for this class when adding new pages and having to increment kids counters in the page tree
     */
    public static class PageAndPageTreeParent {
        public final COSDictionary node; 
        public final COSDictionary parent;

        public PageAndPageTreeParent(COSDictionary node, COSDictionary parent) {
            this.node = node;
            this.parent = parent;
        }
    }
}
