package org.sejda.sambox.util.filetypedetector;

import org.junit.Test;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class FileTypeDetectorTest {

    @Test
    public void detectFileType() throws IOException {
        assertEquals(FileTypeDetector.detectFileType(testFile("/sambox/images/sample.heic")), FileType.HEIF);
        assertEquals(FileTypeDetector.detectFileType(testFile("/sambox/images/sample.webp")), FileType.WEBP);
    }

    @Test
    public void detectFileType_multiple_times_same_seekable_source() throws IOException {
        SeekableSource seekableSource = SeekableSources.seekableSourceFrom(testFile("/sambox/images/sample.heic"));
        assertEquals(FileTypeDetector.detectFileType(seekableSource), FileType.HEIF);
        assertEquals(FileTypeDetector.detectFileType(seekableSource), FileType.HEIF);
    }

    private File testFile(String resPath) {
        return new File(getClass().getResource(resPath).getFile());
    }
}