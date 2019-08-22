package org.sejda.sambox.pdmodel.graphics.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.util.filetypedetector.FileType;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PDImageXObjectTest
{
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
        try {
            PDImageXObject.createFromFile(outFile.getPath());
            fail("Expected an exception");
        } catch (UnsupportedImageFormatException e) {
            assertThat(e.getFilename(), is("broken.png"));
            assertThat(e.getFileType(), is(FileType.UNKNOWN));
        }
    }

    @Test
    public void testImageFormatWithoutImageIOSupport() throws IOException
    {
        File outFile = tempFileFromResource("/org/sejda/sambox/resources/images/sample.psd");
        try {
            PDImageXObject.createFromFile(outFile.getPath());
            fail("Expected an exception");
        } catch (UnsupportedImageFormatException e) {
            assertThat(e.getFilename(), is("sample.psd"));
            assertThat(e.getFileType(), is(FileType.PSD));
        }
    }

    private File tempFileFromResource(String name) throws IOException {
        InputStream in = getClass().getResourceAsStream(name);
        String filename = FilenameUtils.getName(name);

        File outDir = Files.createTempDirectory("test" + getClass().getName()).toFile();
        File outFile = new File(outDir, filename);

        outFile.deleteOnExit();
        outDir.deleteOnExit();

        FileOutputStream out = new FileOutputStream(outFile);
        in.transferTo(out);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        System.out.println(outFile);

        return outFile;
    }

}
