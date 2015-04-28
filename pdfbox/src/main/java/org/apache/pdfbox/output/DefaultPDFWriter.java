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
package org.apache.pdfbox.output;

import static org.apache.pdfbox.cos.DirectCOSObject.asDirectObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.DirectCOSObject;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.cos.LazyIndirectCOSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;

/**
 * @author Andrea Vacondio
 *
 */
public class DefaultPDFWriter implements Closeable
{

    private static final Log LOG = LogFactory.getLog(DefaultPDFWriter.class);
    /**
     * To be used when 2 byte sequence is enforced.
     */
    public static final byte[] CRLF = { '\r', '\n' };
    public static final byte[] LF = { '\n' };
    public static final byte[] EOL = { '\n' };
    public static final byte[] COMMENT = { '%' };
    public static final byte[] GARBAGE = new byte[] { (byte) 0x9d, (byte) 0xe3, (byte) 0xf1,
            (byte) 0xf1 };

    private Map<COSObjectKey, IndirectCOSObjectReference> existingIndirectToNewXref = new HashMap<>();
    private Map<COSBase, IndirectCOSObjectReference> newObjects = new HashMap<>();
    private AtomicInteger objectsCounter = new AtomicInteger(0);
    private COSWriter writer;
    private Set<IndirectCOSObjectReference> toWrite = new HashSet<>();

    public DefaultPDFWriter(OutputStream os)
    {
    }

    public void write(PDDocument document) throws IOException
    {
        if (document.getEncryption() != null)
        {
            // TODO refactor the encrypt/decrypt
            SecurityHandler securityHandler = document.getEncryption().getSecurityHandler();
            if (!securityHandler.hasProtectionPolicy())
            {
                throw new IllegalStateException(
                        "PDF contains an encryption dictionary, please remove it with "
                                + "setAllSecurityToBeRemoved() or set a protection policy with protect()");
            }
            securityHandler.prepareDocumentForEncryption(document);
        }
        DirectCOSObject id = asDirectObject(document.generateFileIdentifier());
        document.getDocument().getTrailer()
                .setItem(COSName.ID, asDirectObject(new COSArray(id, id)));
        writeHeader(document.getDocument().getHeaderVersion());
        COSBase catalog = document.getDocument().getTrailer().getItem(COSName.ROOT);
        process(nextReferenceFor(base));
        // TODO finish write catalog
        // TODO write xref
        close();
    }


    private IndirectCOSObjectReference nextReferenceFor(COSBase baseObject)
    {
        return new IndirectCOSObjectReference(objectsCounter.incrementAndGet(), 0, baseObject);
    }

    private void writeHeader(float version) throws IOException
    {
        writer.write("%PDF-" + Float.toString(version));
        writer.write(EOL);
        writer.write(COMMENT);
        writer.write(GARBAGE);
        writer.write(EOL);
    }

    @Override
    public void close() throws IOException
    {
        writer.close();
        existingIndirectToNewXref.clear();
        newObjects.clear();
    }

    void process(COSBase base){
        IndirectCOSObjectReference ref = nextReferenceFor(base);
        process(ref);
        if(base instanceof LazyIndirectCOSObject){
            existingIndirectToNewXref.put(((LazyIndirectCOSObject)base).key(), ref);
        }else{
            newObjects.put(base, ref);
        }
        toWrite.add(ref);
    }

    void process(IndirectCOSObjectReference ref)
    {

    }
}
