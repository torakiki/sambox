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

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.io.DevNullWritableByteChannel;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.encryption.GeneralEncryptionAlgorithm;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that tries to predict the size of a resulting document if {@link PDPage}s and {@link COSObjectable}s are
 * added to it. The component does its best to return exact predicted values and it does that by simulating an actual
 * write, despite that, the predicted values should be considered rough estimations and not a byte precision ones.
 * 
 * @author Andrea Vacondio
 */
public class ExistingPagesSizePredictor extends AbstractPDFBodyWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(ExistingPagesSizePredictor.class);

    // stream, endstream and 2x CRLF
    private static final int STREAM_WRAPPING_SIZE = 19;

    private IndirectObjectsWriter writer;
    private CountingWritableByteChannel channel = CountingWritableByteChannel
            .from(new DevNullWritableByteChannel());
    private long streamsSize;
    private long pages;

    public ExistingPagesSizePredictor(WriteOption... opts)
    {
        super(new PDFWriteContext(GeneralEncryptionAlgorithm.IDENTITY, opts));
        this.writer = new IndirectObjectsWriter(channel, context())
        {
            @Override
            protected void onWritten(IndirectCOSObjectReference ref)
            {
                // don't release
            }
        };
    }

    /**
     * Adds a {@link PDPage} to the predicted size. This component simulates the page write to a
     * {@link DevNullWritableByteChannel} and does not release the page objects once written.
     * 
     * @param page
     * @throws IOException
     */
    public void addPage(PDPage page) throws IOException
    {
        if (page != null)
        {
            pages++;
            COSDictionary pageCopy = page.getCOSObject().duplicate();
            pageCopy.removeItem(COSName.PARENT);
            createIndirectReferenceIfNeededFor(pageCopy);
            startWriting();
            LOG.debug("Page {} addition simulated, now at {} body bytes and {} xref bytes", page,
                    predictedPagesSize(), predictedXrefTableSize());
        }
    }

    /**
     * Adds the {@link COSObjectable} to the predicted size. The object is added as an indirect reference and is
     * processed, specifically, in case of {@link COSDictionary} or {@link COSArray}, indirect reference might be
     * created for their values.
     * 
     * @param value
     * @throws IOException
     */
    public void addIndirectReferenceFor(COSObjectable value) throws IOException
    {
        if (value != null)
        {
            createIndirectReferenceIfNeededFor(value.getCOSObject());
            startWriting();
            LOG.debug("{} addition simulated, now at {} body bytes and {} xref bytes",
                    value.getCOSObject(), predictedPagesSize(), predictedXrefTableSize());
        }
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
        return (21 * (context().written() + 1)) + 10;
    }

    @Override
    void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        if (!context().hasWritten(ref.xrefEntry()))
        {
            COSBase wrapped = ref.getCOSObject().getCOSObject();
            if (wrapped instanceof COSStream)
            {
                COSStream stream = (COSStream) wrapped;
                // we don't simulate the write of the whole stream, we just save the expected size and simulate the
                // dictionary write
                streamsSize += stream.getFilteredLength();
                streamsSize += STREAM_WRAPPING_SIZE;
                ref.setValue(stream.duplicate());
            }
        }
        writer.writeObject(ref);
    }

    @Override
    void onCompletion()
    {
        // no op
    }

    /**
     * @return true if some page has been written
     */
    public boolean hasPages()
    {
        return pages > 0;
    }

    /**
     * @return the current number of written pages
     */
    public long pages()
    {
        return pages;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        IOUtils.close(writer);
    }
}
