package org.sejda.sambox.pdmodel.graphics.image;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.util.filetypedetector.FileType;

public class PDImageXObjectTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testJpegWithPngExtension() throws IOException
    {
        File outFile = tempFileFromResource("/org/sejda/sambox/resources/images/sample.png");
        PDImageXObject.createFromFile(outFile.getPath());
    }

    @Test
    public void testTiffWithLZWCompression() throws IOException
    {
        File outFile = tempFileFromResource("/org/sejda/sambox/resources/images/lzw.tif");
        PDImageXObject object = PDImageXObject.createFromFile(outFile.getPath());
        assertEquals(object.getHeight(), 287);
    }

    @Test
    public void testSeekableSource() throws IOException
    {
        PDImageXObject object = PDImageXObject.createFromSeekableSource(
                tempSeekableSourceFromResource("/org/sejda/sambox/resources/images/sample.png"),
                "sample.png");
        assertEquals(object.getHeight(), 395);
    }

    @Test
    public void testBrokenImage() throws IOException
    {
        File outFile = tempFileFromResource("/org/sejda/sambox/resources/images/broken.png");
        try
        {
            PDImageXObject.createFromFile(outFile.getPath());
            fail("Expected an exception");
        }
        catch (UnsupportedImageFormatException e)
        {
            assertThat(e.getFilename(), is(outFile.getName()));
            assertThat(e.getFileType(), is(FileType.UNKNOWN));
        }
    }

    @Test
    public void testImageFormatWithoutImageIOSupport() throws IOException
    {
        File outFile = tempFileFromResource("/org/sejda/sambox/resources/images/sample.psd");
        try
        {
            PDImageXObject.createFromFile(outFile.getPath());
            fail("Expected an exception");
        }
        catch (UnsupportedImageFormatException e)
        {
            assertThat(e.getFilename(), is(outFile.getName()));
            assertThat(e.getFileType(), is(FileType.PSD));
        }
    }
    
    @Test
    public void tiffMultiImage() throws IOException {
        PDImageXObject p0 = PDImageXObject.createFromSeekableSource(
                tempSeekableSourceFromResource("/org/sejda/sambox/resources/images/multipage.tiff"),
                "multipage.tiff", 0);
        
        PDImageXObject p1 = PDImageXObject.createFromSeekableSource(
                tempSeekableSourceFromResource("/org/sejda/sambox/resources/images/multipage.tiff"), 
                "multipage.tiff", 1);
        
        assertFalse(compareImages(p0.getImage(), p1.getImage()));
    }

    private boolean compareImages(BufferedImage img1, BufferedImage img2) 
    {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) 
        {
            return false;
        }

        int width = img1.getWidth();
        int height = img1.getHeight();

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) 
                {
                    return false;
                }
            }
        }

        return true;
    }

    private File tempFileFromResource(String name) throws IOException
    {
        File outFile = folder.newFile();
        try (InputStream input = getClass().getResourceAsStream(name))
        {
            Files.copy(input, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return outFile;
    }

    private SeekableSource tempSeekableSourceFromResource(String name) throws IOException
    {
        return SeekableSources.onTempFileSeekableSourceFrom(getClass().getResourceAsStream(name));
    }

}
