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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSInteger;

/**
 * A test case for PDNameTreeNode.
 * 
 * @author Koch
 */
public class PDNameTreeNodeTest
{

    private PDIntegerNameTreeNode root;
    private PDIntegerNameTreeNode node2;
    private PDIntegerNameTreeNode node4;
    private PDIntegerNameTreeNode leaf1;
    private PDIntegerNameTreeNode leaf21;
    private PDIntegerNameTreeNode leaf22;

    @Before
    public void setUp()
    {
        this.leaf1 = new PDIntegerNameTreeNode();
        SortedMap<String, COSInteger> names = new TreeMap<>();
        names.put("Actinium", COSInteger.get(89));
        names.put("Aluminum", COSInteger.get(13));
        names.put("Americium", COSInteger.get(95));
        names.put("Antimony", COSInteger.get(51));
        names.put("Argon", COSInteger.get(18));
        names.put("Arsenic", COSInteger.get(33));
        names.put("Astatine", COSInteger.get(85));
        this.leaf1.setNames(names);

        this.leaf21 = new PDIntegerNameTreeNode();
        names = new TreeMap<>();
        names.put("Xenon", COSInteger.get(54));
        names.put("Ytterbium", COSInteger.get(70));
        names.put("Yttrium", COSInteger.get(39));
        names.put("Zebra", COSInteger.get(39));
        this.leaf21.setNames(names);

        this.leaf22 = new PDIntegerNameTreeNode();
        names = new TreeMap<>();
        names.put("Zavor", COSInteger.get(39));
        names.put("Zinc", COSInteger.get(30));
        names.put("Zirconium", COSInteger.get(40));
        names.put("Zlatan", COSInteger.get(44));
        this.leaf22.setNames(names);

        this.node2 = new PDIntegerNameTreeNode();
        List<PDNameTreeNode<COSInteger>> kids = this.node2.getKids();
        if (kids == null)
        {
            kids = new COSArrayList<>();
        }
        kids.add(this.leaf1);
        this.node2.setKids(kids);

        this.node4 = new PDIntegerNameTreeNode();
        kids = this.node4.getKids();
        if (kids == null)
        {
            kids = new COSArrayList<>();
        }
        kids.add(this.leaf21);
        kids.add(this.leaf22);
        this.node4.setKids(kids);

        this.root = new PDIntegerNameTreeNode();
        kids = this.root.getKids();
        if (kids == null)
        {
            kids = new COSArrayList<>();
        }
        kids.add(this.node2);
        kids.add(this.node4);
        this.root.setKids(kids);
    }

    @Test
    public void testUpperLimit()
    {
        Assert.assertEquals("Astatine", this.leaf1.getUpperLimit());
        Assert.assertEquals("Astatine", this.node2.getUpperLimit());

        Assert.assertEquals("Zebra", this.leaf21.getUpperLimit());
        Assert.assertEquals("Zlatan", this.leaf22.getUpperLimit());
        Assert.assertEquals("Zlatan", this.node4.getUpperLimit());

        Assert.assertEquals(null, this.root.getUpperLimit());
    }

    @Test
    public void testLowerLimit()
    {
        Assert.assertEquals("Actinium", this.leaf1.getLowerLimit());
        Assert.assertEquals("Actinium", this.node2.getLowerLimit());

        Assert.assertEquals("Xenon", this.leaf21.getLowerLimit());
        Assert.assertEquals("Zavor", this.leaf22.getLowerLimit());
        Assert.assertEquals("Xenon", this.node4.getLowerLimit());

        Assert.assertEquals(null, this.root.getLowerLimit());
    }

    @Test
    public void notFound()
    {
        assertNull(root.getValue("Aaaaaaa"));
        assertNull(root.getValue("Aooo"));
        assertNull(root.getValue("Bbbbbbb"));
        assertNull(root.getValue("Rrrrrrr"));
        assertNull(root.getValue("Zaaaaaaa"));
    }

    @Test
    public void found()
    {
        assertNotNull(root.getValue("Americium"));
        assertNotNull(root.getValue("Ytterbium"));
        assertNotNull(root.getValue("Zirconium"));
        assertNotNull(root.getValue("Zavor"));
    }

}
