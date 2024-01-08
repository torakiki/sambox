/*
 * Created on 23/lug/2015
 * Copyright 2015 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sejda.sambox.input;

import static org.junit.Assert.assertEquals;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSource;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;

/**
 * @author Andrea Vacondio
 *
 */
public class ContentStreamParserTest
{
    private ContentStreamParser victim;

    @After
    public void tearDown() throws Exception
    {
        IOUtils.close(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument()
    {
        new ContentStreamParser((SeekableSource) null);
    }

    @Test
    public void tokens() throws IOException
    {
        victim = new ContentStreamParser(
                inMemorySeekableSourceFrom("q\n27.27\n/a0\nBT\n(something)\nET".getBytes()));
        List<Object> tokens = victim.tokens();
        assertEquals(Operator.getOperator("q"), tokens.get(0));
        assertEquals(COSNumber.get("27.27"), tokens.get(1));
        assertEquals(COSName.getPDFName("a0"), tokens.get(2));
    }

    @Test
    public void nextParsedToken() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom("q\n27.27\n/a0".getBytes()));
        assertEquals(Operator.getOperator("q"), victim.nextParsedToken());
        assertEquals(COSNumber.get("27.27"), victim.nextParsedToken());
        assertEquals(COSName.getPDFName("a0"), victim.nextParsedToken());
    }

    @Test
    public void nextInlineImage() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_stream.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals(5, operator.getImageParameters().size());
        assertEquals(14, operator.getImageData().length);
        operator = (Operator) victim.nextParsedToken();
        assertEquals("Q", operator.getName());
    }

    @Test
    public void nextInlineImage00Q() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_stream_0Q.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals(5, operator.getImageParameters().size());
        assertEquals(14, operator.getImageData().length);
        operator = (Operator) victim.nextParsedToken();
        assertEquals("Q", operator.getName());
    }

    @Test
    public void nextInlineImageNoSpace() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_stream_no_space.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals(5, operator.getImageParameters().size());
        assertEquals(14, operator.getImageData().length);
    }

    @Test
    public void nextInlineImageEndOfStream() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_end_of_stream.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals(5, operator.getImageParameters().size());
        assertEquals(14, operator.getImageData().length);
    }

    @Test
    public void nextInlineImageIDNoSpace() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_ID_no_space.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals(5, operator.getImageParameters().size());
        assertEquals(14, operator.getImageData().length);
    }

    @Test
    public void nextInlineImageEINULL() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_EI_NULL_in_stream.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals(5, operator.getImageParameters().size());
        assertEquals(17, operator.getImageData().length);
        operator = (Operator) victim.nextParsedToken();
        assertEquals("q", operator.getName());
    }

    @Test
    public void nextInlineImageEISpace() throws IOException
    {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_EI_SPACE_in_stream.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals(5, operator.getImageParameters().size());
        assertEquals(17, operator.getImageData().length);
        operator = (Operator) victim.nextParsedToken();
        assertEquals("q", operator.getName());
    }
    
    @Test
    public void nextInlineImageEIInStreamWithoutSpace() throws IOException {
        victim = new ContentStreamParser(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/inline_image_EI_inside_stream.txt")));
        Operator operator = (Operator) victim.nextParsedToken();
        assertEquals("BI", operator.getName());
        assertEquals(768, operator.getImageData().length);
        operator = (Operator) victim.nextParsedToken();
        assertEquals("Q", operator.getName());
    }
}
