/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.output;

import java.io.IOException;
import java.util.List;

import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.io.DevNullWritableByteChannel;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that tries to predict the size of a resulting document if existing pages are added to it. The component
 * does its best to return exact predicted values and it does that by simulating an actual write, despite that, the
 * predicted values should be considered rough estimations and not a byte precision ones.
 * 
 * @author Andrea Vacondio
 */
public class ExistingPagesSizePredictor extends AbstractPdfBodyWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(ExistingPagesSizePredictor.class);

    // stream, endstream and 2x CRLF
    private static final int STREAM_WRAPPING_SIZE = 19;

    private IndirectObjectsWriter writer;
    private CountingWritableByteChannel channel = CountingWritableByteChannel
            .from(new DevNullWritableByteChannel());
    private long streamsSize;
    private long pages;

    public ExistingPagesSizePredictor(List<WriteOption> opts)
    {
        super(opts);
        this.writer = new IndirectObjectsWriter(new DefaultCOSWriter(
                new BufferedCountingChannelWriter(channel)));
    }

    /**
     * Adds an existing, unmodified page to the predicted size. This component is intended to be used on existing and
     * unmodified pages, mainly for split purposes. It simulates the page write to a {@link DevNullWritableByteChannel}
     * and will release the page objects once written, this means that every modification made to the existing page will
     * be lost.
     * 
     * @param page
     * @throws IOException
     */
    public void addPage(PDPage page) throws IOException
    {
        pages++;
        COSDictionary pageCopy = new COSDictionary(page.getCOSObject());
        pageCopy.removeItem(COSName.PARENT);
        getOrCreateIndirectReferenceFor(pageCopy);
        startWriting();
        LOG.trace("Page {} addition simlated, now at {} body bytes and {} xref bytes", page,
                predictedPagesSize(), predictedXrefTableSize());
    }

    /**
     * Adds the object to the predicted size. The object is added as an indirect reference and is supposed to be written
     * as is, specifically, in case of {@link COSDictionary} or {@link COSArray}, no indirect reference is created for
     * their values.
     * 
     * @param object
     * @throws IOException
     */
    public void addCOSBase(COSBase object) throws IOException
    {
        writer.writeObject(referencesProvider().nextReferenceFor(object));
    }

    @Override
    public void visit(COSDocument document)
    {
        // nothing
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        if (opts().contains(WriteOption.COMPRESS_STREAMS))
        {
            value.addCompression();
        }
        // we don't simulate the write of the whole stream, we just save the expected size and simulate the dictionary
        // write
        streamsSize += value.getFilteredLength();
        streamsSize += STREAM_WRAPPING_SIZE;
        // a copy
        visit(new COSDictionary(value));
    }

    /**
     * @return the current predicted page size
     * @throws IOException
     */
    public long predictedPagesSize() throws IOException
    {
        writer.writer().writer().flush();
        return streamsSize + channel.count();
    }

    /**
     * @return the current predicted xref size
     * @throws IOException
     */
    public long predictedXrefTableSize()
    {
        // each entry is 21 bytes plus the xref keyword and section header
        return (21 * (writer.size() + 1)) + 10;
    }

    @Override
    void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        writer.writeObjectIfNotWritten(ref);
    }

    @Override
    void onCompletion()
    {
        // no op
    }

    @Override
    IndirectObjectsWriter writer()
    {
        return writer;
    }

    public boolean hasPages()
    {
        return pages > 0;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        IOUtils.close(writer);
    }
}
