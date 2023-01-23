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

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

import java.util.Iterator;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * Base class for a node in the outline of a PDF document.
 *
 * @author Ben Litchfield
 */
public abstract class PDOutlineNode extends PDDictionaryWrapper
{

    public PDOutlineNode()
    {
        super();
    }

    /**
     * @param dict The dictionary storage.
     */
    public PDOutlineNode(COSDictionary dict)
    {
        super(dict);
    }

    /**
     * @return The parent of this node or null if there is no parent.
     */
    PDOutlineNode getParent()
    {
        COSDictionary item = getCOSObject().getDictionaryObject(COSName.PARENT,
                COSDictionary.class);
        if (item != null)
        {
            if (COSName.OUTLINES.equals(item.getCOSName(COSName.TYPE)))
            {
                return new PDDocumentOutline(item);
            }
            return new PDOutlineItem(item);
        }
        return null;
    }

    void setParent(PDOutlineNode parent)
    {
        getCOSObject().setItem(COSName.PARENT, parent);
    }

    /**
     * Adds the given node to the bottom of the children list.
     *
     * @param newChild The node to add.
     * @throws IllegalArgumentException if the given node is part of a list (i.e. if it has a
     *                                  previous or a next sibling)
     */
    public void addLast(PDOutlineItem newChild)
    {
        requireSingleNode(newChild);
        append(newChild);
        updateParentOpenCountForAddedChild(newChild);
    }
    
    public void addAtPosition(PDOutlineItem newChild, int index)
    {
        requireSingleNode(newChild);
        insert(newChild, index);
        updateParentOpenCountForAddedChild(newChild);
    }

    /**
     * Adds the given node to the top of the children list.
     *
     * @param newChild The node to add.
     * @throws IllegalArgumentException if the given node is part of a list (i.e. if it has a
     *                                  previous or a next sibling)
     */
    public void addFirst(PDOutlineItem newChild)
    {
        requireSingleNode(newChild);
        prepend(newChild);
        updateParentOpenCountForAddedChild(newChild);
    }

    /**
     * @param node
     * @throws IllegalArgumentException if the given node is part of a list (i.e. if it has a
     *                                  previous or a next sibling)
     */
    void requireSingleNode(PDOutlineItem node)
    {
        if (node.getNextSibling() != null || node.getPreviousSibling() != null)
        {
            throw new IllegalArgumentException("A single node with no siblings is required");
        }
    }

    /**
     * Appends the child to the linked list of children. This method only adjust pointers but
     * doesn't take care of the Count key in the parent hierarchy.
     *
     * @param newChild
     */
    private void append(PDOutlineItem newChild)
    {
        newChild.setParent(this);
        if (!hasChildren())
        {
            setFirstChild(newChild);
        }
        else
        {
            PDOutlineItem previousLastChild = getLastChild();
            previousLastChild.setNextSibling(newChild);
            newChild.setPreviousSibling(previousLastChild);
        }
        setLastChild(newChild);
    }

    /**
     * Prepends the child to the linked list of children. This method only adjust pointers but
     * doesn't take care of the Count key in the parent hierarchy.
     *
     * @param newChild
     */
    private void prepend(PDOutlineItem newChild)
    {
        newChild.setParent(this);
        if (!hasChildren())
        {
            setLastChild(newChild);
        }
        else
        {
            PDOutlineItem previousFirstChild = getFirstChild();
            newChild.setNextSibling(previousFirstChild);
            previousFirstChild.setPreviousSibling(newChild);
        }
        setFirstChild(newChild);
    }

    private void insert(PDOutlineItem newChild, int index)
    {
        newChild.setParent(this);
        
        if(hasChildren()) {
            int currentIndex = 0;
            Iterator<PDOutlineItem> iterator = children().iterator();
            PDOutlineItem prev = null;
            PDOutlineItem current = iterator.next();

            while (currentIndex < index && iterator.hasNext()) {
                currentIndex++;
                prev = current;
                current = iterator.next();
            }
            
            if(currentIndex == index)
            {
                // newChild is inserted before current
                // newChild becomes index
                // current becomes index + 1
                
                // Unreliable, some outlines are broken, use the iterated reference 
                //PDOutlineItem prev = current.getPreviousSibling();

                current.setPreviousSibling(newChild);

                newChild.setNextSibling(current);
                newChild.setPreviousSibling(prev);

                if (prev != null) 
                {
                    prev.setNextSibling(newChild);
                } 
                else 
                {
                    setFirstChild(newChild);
                }    
            }
            else if (currentIndex + 1 <= index)
            {
                // newChild is inserted after last item, current
                newChild.setPreviousSibling(current);
                current.setNextSibling(newChild);
                setLastChild(newChild);
            } 
            else
            {
                throw new RuntimeException("Cannot insert at: currentIndex: " + currentIndex + " index: " + index);
            }
        }
        else
        {
            setFirstChild(newChild);
            setLastChild(newChild);
        }
    }

    void updateParentOpenCountForAddedChild(PDOutlineItem newChild)
    {
        int delta = 1;
        if (newChild.isNodeOpen())
        {
            delta += newChild.getOpenCount();
        }
        newChild.updateParentOpenCount(delta);
    }

    /**
     * @return true if the node has at least one child
     */
    public boolean hasChildren()
    {
        return nonNull(getCOSObject().getDictionaryObject(COSName.FIRST, COSDictionary.class));
    }

    PDOutlineItem getOutlineItem(COSName name)
    {
        return ofNullable(getCOSObject().getDictionaryObject(name, COSDictionary.class)).map(
                PDOutlineItem::new).orElse(null);
    }

    /**
     * @return The first child or null if there is no child.
     */
    public PDOutlineItem getFirstChild()
    {
        return getOutlineItem(COSName.FIRST);
    }

    /**
     * Set the first child, this will be maintained by this class.
     *
     * @param outlineNode The new first child.
     */
    void setFirstChild(PDOutlineNode outlineNode)
    {
        getCOSObject().setItem(COSName.FIRST, outlineNode);
    }

    /**
     * @return The last child or null if there is no child.
     */
    public PDOutlineItem getLastChild()
    {
        return getOutlineItem(COSName.LAST);
    }

    /**
     * Set the last child, this will be maintained by this class.
     *
     * @param outlineNode The new last child.
     */
    void setLastChild(PDOutlineNode outlineNode)
    {
        getCOSObject().setItem(COSName.LAST, outlineNode);
    }

    /**
     * Get the number of open nodes or a negative number if this node is closed. See PDF Reference
     * 32000-1:2008 table 152 and 153 for more details. This value is updated as you append children
     * and siblings.
     *
     * @return The Count attribute of the outline dictionary.
     */
    public int getOpenCount()
    {
        return getCOSObject().getInt(COSName.COUNT, 0);
    }

    /**
     * Set the open count. This number is automatically managed for you when you add items to the
     * outline.
     *
     * @param openCount The new open count.
     */
    void setOpenCount(int openCount)
    {
        getCOSObject().setInt(COSName.COUNT, openCount);
    }

    /**
     * This will set this node to be open when it is shown in the viewer. By default, when a new
     * node is created it will be closed. This will do nothing if the node is already open.
     */
    public void openNode()
    {
        // if the node is already open then do nothing.
        if (!isNodeOpen())
        {
            switchNodeCount();
        }
    }

    /**
     * Close this node.
     */
    public void closeNode()
    {
        if (isNodeOpen())
        {
            switchNodeCount();
        }
    }

    private void switchNodeCount()
    {
        int openCount = getOpenCount();
        setOpenCount(-openCount);
        updateParentOpenCount(-openCount);
    }

    /**
     * @return true if this node count is greater than zero, false otherwise.
     */
    public boolean isNodeOpen()
    {
        return getOpenCount() > 0;
    }

    /**
     * The count parameter needs to be updated when you add, remove, open or close outline items.
     *
     * @param delta The amount to update by.
     */
    void updateParentOpenCount(int delta)
    {
        PDOutlineNode parent = getParent();
        if (parent != null)
        {
            if (parent.isNodeOpen())
            {
                parent.setOpenCount(parent.getOpenCount() + delta);
                parent.updateParentOpenCount(delta);
            }
            else
            {
                parent.setOpenCount(parent.getOpenCount() - delta);
            }
        }
    }

    /**
     * @return An {@link Iterable} view of the items children
     */
    public Iterable<PDOutlineItem> children()
    {
        return () -> new PDOutlineItemIterator(getFirstChild());
    }
}
