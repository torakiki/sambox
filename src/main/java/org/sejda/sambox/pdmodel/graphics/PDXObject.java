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
package org.sejda.sambox.pdmodel.graphics;

import java.io.IOException;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.ResourceCache;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.graphics.form.PDTransparencyGroup;
import org.sejda.sambox.pdmodel.graphics.image.PDImageXObject;

/**
 * An external object, or "XObject".
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public class PDXObject implements COSObjectable
{
    private PDStream stream;

    /**
     * Creates a new XObject instance of the appropriate type for the COS stream.
     *
     * @param stream The stream which is wrapped by this XObject.
     * @return A new XObject instance.
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public static PDXObject createXObject(COSStream stream, PDResources resources)
            throws IOException
    {
        if (stream == null)
        {
            // TODO throw an exception?
            return null;
        }

        String subtype = stream.getNameAsString(COSName.SUBTYPE);

        if (COSName.IMAGE.getName().equals(subtype))
        {
            return new PDImageXObject(new PDStream(stream), resources);
        }
        if (COSName.FORM.getName().equals(subtype))
        {
            ResourceCache cache = resources != null ? resources.getResourceCache() : null;
            COSDictionary group = stream.getDictionaryObject(COSName.GROUP, COSDictionary.class);
            if (group != null && COSName.TRANSPARENCY.equals(group.getCOSName(COSName.S)))
            {
                return new PDTransparencyGroup(stream, cache);
            }
            return new PDFormXObject(stream, cache);
        }
        if (COSName.PS.getName().equals(subtype))
        {
            return new PDPostScriptXObject(stream);
        }
        throw new IOException("Invalid XObject Subtype: " + subtype);
    }

    /**
     * Creates a new XObject from the given stream and subtype.
     * 
     * @param stream The stream to read.
     * @param subtype
     */
    protected PDXObject(COSStream stream, COSName subtype)
    {
        this.stream = new PDStream(stream);
        // could be used for writing:
        stream.setName(COSName.TYPE, COSName.XOBJECT.getName());
        stream.setName(COSName.SUBTYPE, subtype.getName());
    }

    /**
     * Creates a new XObject from the given stream and subtype.
     * 
     * @param stream The stream to read.
     * @param subtype
     */
    protected PDXObject(PDStream stream, COSName subtype)
    {
        this.stream = stream;
        stream.getCOSObject().setName(COSName.TYPE, COSName.XOBJECT.getName());
        stream.getCOSObject().setName(COSName.SUBTYPE, subtype.getName());
    }

    /**
     * Creates a new XObject from the given stream and subtype.
     */
    protected PDXObject(COSName subtype)
    {
        this.stream = new PDStream();
        stream.getCOSObject().setName(COSName.TYPE, COSName.XOBJECT.getName());
        stream.getCOSObject().setName(COSName.SUBTYPE, subtype.getName());
    }

    @Override
    public final COSStream getCOSObject()
    {
        return stream.getCOSObject();
    }

    /**
     * @return The stream for this object.
     */
    public final PDStream getStream()
    {
        return stream;
    }

    public final void setStream(PDStream stream) {
        stream.getCOSObject().setName(COSName.TYPE, COSName.XOBJECT.getName());
        stream.getCOSObject().setName(COSName.SUBTYPE, this.stream.getCOSObject().getCOSName(COSName.SUBTYPE).getName());
        this.stream = stream;
    }
}
