package org.sejda.sambox.pdmodel;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.sejda.sambox.cos.COSDictionary.of;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSName;

class PDResourcesTest
{

    @Test
    public void wrongFontType() throws IOException
    {
        var resources = of(COSName.FONT, of(COSName.getPDFName("F1"), COSName.EMBEDDED_FILES));
        var victim = new PDResources(resources);
        assertNull(victim.getFont(COSName.getPDFName("F1")));
    }

    @Test
    public void wrongExtGStateType() throws IOException
    {
        var state = of(COSName.getPDFName("es1"), COSName.EMBEDDED_FILES);
        var resources = of(COSName.EXT_G_STATE, state);
        var victim = new PDResources(resources);
        assertNull(victim.getExtGState(COSName.getPDFName("es1")));
    }

    @Test
    public void wrongPatternType() throws IOException
    {
        var patternDictionary = of(COSName.getPDFName("P1"), COSName.EMBEDDED_FILES);
        var resources = of(COSName.PATTERN, patternDictionary);
        var victim = new PDResources(resources);
        assertNull(victim.getPattern(COSName.getPDFName("P1")));
    }

    @Test
    public void wrongPropertiesType() throws IOException
    {
        var propertiesDictionary = of(COSName.getPDFName("P1"), COSName.EMBEDDED_FILES);
        var resources = of(COSName.PROPERTIES, propertiesDictionary);
        var victim = new PDResources(resources);
        assertNull(victim.getProperties(COSName.getPDFName("P1")));
    }

    @Test
    public void wrongShadingType() throws IOException
    {
        var shadingDictionary = of(COSName.getPDFName("S1"), COSName.EMBEDDED_FILES);
        var resources = of(COSName.SHADING, shadingDictionary);
        var victim = new PDResources(resources);
        assertNull(victim.getShading(COSName.getPDFName("S1")));
    }
}