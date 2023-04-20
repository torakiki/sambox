package org.sejda.sambox.pdmodel.font;

import org.apache.fontbox.FontBoxFont;
import org.apache.fontbox.ttf.NamingTable;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FontMapperImplTest {
    
    @Test
    public void courierNew_to_Courier() throws IOException {
        FontMapperImpl victim = createMapper("CourierNewPSMT");
        assertHasMatchingFont(victim, "Courier", "CourierNewPSMT");
    }

    @Test
    public void courier_to_Courier() throws IOException {
        FontMapperImpl victim = createMapper("CourierNew", "CourierNewPSMT", "LiberationMono");
        assertHasMatchingFont(victim, "Courier", "CourierNew");
    }

    @Test
    public void caseInsensitive_PDFBOX5514() throws IOException {
        FontMapperImpl victim = createMapper("3of9Barcode");
        assertHasMatchingFont(victim, "3Of9Barcode", "3of9Barcode");
    }
    
    private static void assertHasMatchingFont(FontMapperImpl victim, String search, String expected) throws IOException {
        FontMapping<FontBoxFont> result = victim.getFontBoxFont(search, new PDFontDescriptor());

        assertFalse(result.isFallback());
        assertEquals(expected, result.getFont().getName());
    }
    
    private static FontMapperImpl createMapper(String... psNames)
    {
        return new FontMapperImpl(createFontProvider(psNames));
    }
    
    private static FontProvider createFontProvider(String... psNames)
    {
        return new TestFontProvider(
                Arrays.stream(psNames).map(TestFontInfo::new).collect(Collectors.toList())
        );
    }

    private static TrueTypeFont createTTFWithName(String name)
    {
        try {
            TrueTypeFont ttf = FontMapperImpl.loadLastResortFont();
            NamingTable naming = ttf.getNaming();
            Field field = naming.getClass().getDeclaredField("psName");
            field.setAccessible(true);
            field.set(naming, name);

            return ttf;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class TestFontProvider extends FontProvider {

        public final List<TestFontInfo> fonts = new ArrayList<>();
        
        public TestFontProvider(List<TestFontInfo> fonts) {
            this.fonts.addAll(fonts);
        }

        @Override
        public List<? extends FontInfo> getFontInfo() {
            return fonts;
        }
    }
    
    public static class TestFontInfo extends FontInfo {
        
        public final String postscriptName;
        public final TrueTypeFont ttf;

        public TestFontInfo(String postscriptName) {
            this.postscriptName = postscriptName;
            this.ttf = createTTFWithName(postscriptName);
        }

        @Override
        public String getPostScriptName() {
            return postscriptName;
        }

        @Override
        public FontFormat getFormat() {
            return FontFormat.TTF;
        }

        @Override
        public CIDSystemInfo getCIDSystemInfo() {
            return null;
        }

        @Override
        public FontBoxFont getFont() {
            return ttf;
        }

        @Override
        public int getFamilyClass() {
            return 0;
        }

        @Override
        public int getWeightClass() {
            return 0;
        }

        @Override
        public int getCodePageRange1() {
            return 0;
        }

        @Override
        public int getCodePageRange2() {
            return 0;
        }

        @Override
        public int getMacStyle() {
            return 0;
        }

        @Override
        public PDPanoseClassification getPanose() {
            return null;
        }
    }
}
