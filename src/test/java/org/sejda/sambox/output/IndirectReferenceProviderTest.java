package org.sejda.sambox.output;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSInteger;

public class IndirectReferenceProviderTest
{

    private IndirectReferenceProvider victim;

    @Before
    public void setUp()
    {
        victim = new IndirectReferenceProvider();
    }

    @Test
    public void nextRef()
    {
        assertEquals(1, victim.nextReferenceFor(COSInteger.ONE).xrefEntry().getObjectNumber());
        assertEquals(2, victim.nextReferenceFor(COSInteger.ONE).xrefEntry().getObjectNumber());
    }

    @Test
    public void nextRefFromSeed()
    {
        victim = new IndirectReferenceProvider(5);
        assertEquals(6, victim.nextReferenceFor(COSInteger.ONE).xrefEntry().getObjectNumber());
        assertEquals(7, victim.nextReferenceFor(COSInteger.ONE).xrefEntry().getObjectNumber());
    }

    @Test
    public void nullRef()
    {
        assertEquals(1, victim.nextReferenceFor(null).xrefEntry().getObjectNumber());
        assertEquals(2, victim.nextReferenceFor(null).xrefEntry().getObjectNumber());
    }

}
