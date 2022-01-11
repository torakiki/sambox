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

import junit.framework.TestCase;
import org.junit.Assert;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSObjectable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A test case for PDNumberTreeNode. Based on TestPDNameTreeNode.
 * 
 * @author Dominic Tubach
 */
public class TestPDNumberTreeNode extends TestCase
{

    private PDNumberTreeNode node1;
    private PDNumberTreeNode node2;
    private PDNumberTreeNode node4;
    private PDNumberTreeNode node5;
    private PDNumberTreeNode node24;

    public static class PDTest implements COSObjectable
    {
        private final int value;

        public PDTest(int value)
        {
            this.value = value;
        }

        public PDTest(COSInteger cosInt)
        {
            this.value = cosInt.intValue();
        }

        @Override
        public COSInteger getCOSObject()
        {
            return COSInteger.get(value);
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + value;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            PDTest other = (PDTest) obj;
            return value == other.value;
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        this.node5 = new PDNumberTreeNode(PDTest.class);
        Map<Integer, PDTest> Numbers = new TreeMap<Integer, PDTest>();
        Numbers.put(1, new PDTest(89));
        Numbers.put(2, new PDTest(13));
        Numbers.put(3, new PDTest(95));
        Numbers.put(4, new PDTest(51));
        Numbers.put(5, new PDTest(18));
        Numbers.put(6, new PDTest(33));
        Numbers.put(7, new PDTest(85));
        this.node5.setNumbers(Numbers);

        this.node24 = new PDNumberTreeNode(PDTest.class);
        Numbers = new TreeMap<Integer, PDTest>();
        Numbers.put(8, new PDTest(54));
        Numbers.put(9, new PDTest(70));
        Numbers.put(10, new PDTest(39));
        Numbers.put(11, new PDTest(30));
        Numbers.put(12, new PDTest(40));
        this.node24.setNumbers(Numbers);

        this.node2 = new PDNumberTreeNode(PDTest.class);
        List<PDNumberTreeNode> kids = this.node2.getKids();
        if (kids == null)
        {
            kids = new COSArrayList<PDNumberTreeNode>();
        }
        kids.add(this.node5);
        this.node2.setKids(kids);

        this.node4 = new PDNumberTreeNode(PDTest.class);
        kids = this.node4.getKids();
        if (kids == null)
        {
            kids = new COSArrayList<PDNumberTreeNode>();
        }
        kids.add(this.node24);
        this.node4.setKids(kids);

        this.node1 = new PDNumberTreeNode(PDTest.class);
        kids = this.node1.getKids();
        if (kids == null)
        {
            kids = new COSArrayList<PDNumberTreeNode>();
        }
        kids.add(this.node2);
        kids.add(this.node4);
        this.node1.setKids(kids);
    }

    public void testGetValue() throws IOException
    {
        Assert.assertEquals(new PDTest(51), this.node5.getValue(4));
        Assert.assertEquals(new PDTest(70), this.node1.getValue(9));

        this.node1.setKids(null);
        this.node1.setNumbers(null);
        Assert.assertNull(this.node1.getValue(0));
    }

    public void testUpperLimit() throws IOException
    {
        Assert.assertEquals(7, (int) this.node5.getUpperLimit());
        Assert.assertEquals(7, (int) this.node2.getUpperLimit());

        Assert.assertEquals(12, (int) this.node24.getUpperLimit());
        Assert.assertEquals(12, (int) this.node4.getUpperLimit());

        Assert.assertEquals(12, (int) this.node1.getUpperLimit());

        this.node24.setNumbers(new HashMap<Integer, COSObjectable>());
        Assert.assertNull(this.node24.getUpperLimit());

        this.node5.setNumbers(null);
        Assert.assertNull(this.node5.getUpperLimit());

        this.node1.setKids(null);
        Assert.assertNull(this.node1.getUpperLimit());
    }

    public void testLowerLimit() throws IOException
    {
        Assert.assertEquals(1, (int) this.node5.getLowerLimit());
        Assert.assertEquals(1, (int) this.node2.getLowerLimit());

        Assert.assertEquals(8, (int) this.node24.getLowerLimit());
        Assert.assertEquals(8, (int) this.node4.getLowerLimit());

        Assert.assertEquals(1, (int) this.node1.getLowerLimit());

        this.node24.setNumbers(new HashMap<Integer, COSObjectable>());
        Assert.assertNull(this.node24.getLowerLimit());

        this.node5.setNumbers(null);
        Assert.assertNull(this.node5.getLowerLimit());

        this.node1.setKids(null);
        Assert.assertNull(this.node1.getLowerLimit());
    }

}
