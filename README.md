SAMBox PDF processor
=====================
[![Build Status](https://travis-ci.org/torakiki/sambox.png)](https://travis-ci.org/torakiki/sambox)
[![License](http://img.shields.io/badge/license-APLv2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

An [Apache PDFBox](https://github.com/apache/pdfbox) fork intended to be used as PDF processor for [Sejda](https://github.com/torakiki/sejda) and [PDFsam](https://github.com/torakiki/pdfsam) related projects

What's different from PDFBox?
---------
+ Requires JDK8
+ Lazy loading/parsing of PDF objects. Only the document xref table(s)/stream(s) is(are) initially parsed and information to lookup objects are retrieved, when later a PDF object is requested, the object is retrieve/parsed using the lookup information. This allows minimal memory footprint when you only need part of the document (Ex. you only need the information dictionary or the number of pages of the document).
+ Multiple I/O implementations to read from. SAMBox uses [Sejda-io](https://github.com/torakiki/sejda-io) allowing to use one of the provided implementation based on `java.nio.channels.FileChannel`, `java.io.InputStream` and `java.nio.MappedByteBuffer` (buffered or not).
+ Minimized GC through the use of a pool of `java.lang.StringBuilder`.
+ PDF streams are read directly from the underlying source through the concept of bounded views.
+ Use and discard of lazy PDF objects. PDF objects can be written (sync/async) and discarded as soon as they have been written, this is particularly useful with existing documents where objects are lazy loaded, written and then discarded, keeping a low memory footprint.
+ All the I/O and parsing logic has been refactored to smaller classes which are nearly 100% unit tested. 
+ Some of the PDFBox features are currently of no use for Sejda or PDFsam and they have been removed from SAMBox (preflight validator, fdf, digital signature... ).
+ Documents can be saved using objects stream to store PDF objects.

Are PDFBox commits merged to SAMBox?
---------
SAMBox is a fork of a SNAPSHOT of PDFBox 2.0.0 and we try to keep it aligned with it. We performed massive changes on the original codebase and the same did the PDFBox guys since the time of the forking so merging back stuff from the PDFBox trunk is sometime challenging, we do our best.

How do I load a document?
---------
Here is a snippet to load a document from a file and write it back to a newFile. 
``` 
		try(PDDocument document = PDFParser.parse(SeekableSources.seekableSourceFrom(file))){
            document.writeTo(newFile, WriteOption.XREF_STREAM);
        }
``` 

Tuning
---------
Some system properties are available to modify SAMBox default behaviour. Take a look at `org.sejda.io.SeekableSources` and `org.sejda.sambox.SAMBox` to find out which are currently available.