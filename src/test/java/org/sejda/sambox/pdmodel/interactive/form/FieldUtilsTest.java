package org.sejda.sambox.pdmodel.interactive.form;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSString;

public class FieldUtilsTest
{
    @Test
    public void mixedStringAndArrayPairItems() {
        COSArray base = new COSArray(
                COSString(""),
                new COSArray(COSString("27"), COSString("Option 1")),
                COSString("-"),
                new COSArray(COSString("28"), COSString("Option 2"))
        );
        List<String> resultIndex0 = FieldUtils.getPairableItems(base, 0);
        assertThat(resultIndex0, is(Arrays.asList("", "27", "-", "28")));

        List<String> resultIndex1 = FieldUtils.getPairableItems(base, 1);
        assertThat(resultIndex1, is(Arrays.asList("", "Option 1", "-", "Option 2")));
    }

    private static COSString COSString(String v) {
        return new COSString(v.getBytes());
    }
}