package org.sejda.sambox.pdmodel.graphics.image;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.util.filetypedetector.FileType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
