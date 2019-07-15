package org.sejda.sambox.util.filetypedetector;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class FileTypeDetectorTest {

    @Test
    public void detectFileType() throws IOException {
        assertEquals(FileTypeDetector.detectFileType(testFile("/sambox/images/sample.heic")), FileType.HEIF);
        assertEquals(FileTypeDetector.detectFileType(testFile("/sambox/images/sample.webp")), FileType.WEBP);
    }

    private File testFile(String resPath) {
        return new File(getClass().getResource(resPath).getFile());
    }
}