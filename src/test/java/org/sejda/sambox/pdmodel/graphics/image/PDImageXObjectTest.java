package org.sejda.sambox.pdmodel.graphics.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class PDImageXObjectTest
{
    @Test
    public void testJpegWithPngExtension() throws IOException
    {
        InputStream in = getClass()
                .getResourceAsStream("/org/sejda/sambox/resources/images/sample.png");
        File outFile = File.createTempFile("sample", ".jpeg");
        FileOutputStream out = new FileOutputStream(outFile);
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);

        PDImageXObject.createFromFile(outFile.getPath());
    }

}
