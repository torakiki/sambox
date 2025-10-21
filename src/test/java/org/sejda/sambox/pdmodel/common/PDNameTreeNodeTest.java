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

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
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
        List<PDNameTreeNode<COSInteger>> kids2 = ofNullable(this.node2.getKids()).map(
                ArrayList::new).orElseGet(ArrayList::new);
        kids2.add(this.leaf1);
        this.node2.setKids(kids2);

        this.node4 = new PDIntegerNameTreeNode();
        List<PDNameTreeNode<COSInteger>> kids4 = ofNullable(this.node4.getKids()).map(
                ArrayList::new).orElseGet(ArrayList::new);
        kids4.add(this.leaf21);
        kids4.add(this.leaf22);
        this.node4.setKids(kids4);

        this.root = new PDIntegerNameTreeNode();
        List<PDNameTreeNode<COSInteger>> kidsRoot = ofNullable(this.root.getKids()).map(
                ArrayList::new).orElseGet(ArrayList::new);
        kidsRoot.add(this.node2);
        kidsRoot.add(this.node4);
        this.root.setKids(kidsRoot);
    }

    @Test
    public void testUpperLimit()
    {
        assertEquals("Astatine", this.leaf1.getUpperLimit());
        assertEquals("Astatine", this.node2.getUpperLimit());

        assertEquals("Zebra", this.leaf21.getUpperLimit());
        assertEquals("Zlatan", this.leaf22.getUpperLimit());
        assertEquals("Zlatan", this.node4.getUpperLimit());

        assertEquals(null, this.root.getUpperLimit());
    }

    @Test
    public void testLowerLimit()
    {
        assertEquals("Actinium", this.leaf1.getLowerLimit());
        assertEquals("Actinium", this.node2.getLowerLimit());

        assertEquals("Xenon", this.leaf21.getLowerLimit());
        assertEquals("Zavor", this.leaf22.getLowerLimit());
        assertEquals("Xenon", this.node4.getLowerLimit());

        assertEquals(null, this.root.getLowerLimit());
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
