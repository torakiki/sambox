package org.sejda.sambox.pdmodel.graphics.image;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

}
