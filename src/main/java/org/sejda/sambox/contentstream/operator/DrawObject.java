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
package org.sejda.sambox.contentstream.operator;

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.graphics.form.PDTransparencyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Do: Draws an XObject.
 *
 * @author Ben Litchfield
 * @author Mario Ivankovits
 */
public class DrawObject extends OperatorProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(DrawObject.class);

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException
    {
        if (arguments.isEmpty())
        {
            throw new MissingOperandException(operator, arguments);
        }
        COSBase base0 = arguments.get(0);
        if (!(base0 instanceof COSName))
        {
            return;
        }
        COSName name = (COSName) base0;
        if (getContext().getResources().isImageXObject(name))
        {
            // we're done here, don't decode images when doing text extraction
            return;
        }

        PDXObject xobject = getContext().getResources().getXObject(name);
        if (xobject instanceof PDFormXObject)
        {
            try
            {
                getContext().increaseLevel();
                if (getContext().getLevel() > 25)
                {
                    LOG.error("recursion is too deep, skipping form XObject");
                    return;
                }
                PDFormXObject form = (PDFormXObject) xobject;
                if (form instanceof PDTransparencyGroup)
                {
                    getContext().showTransparencyGroup((PDTransparencyGroup) form);
                }
                else
                {
                    getContext().showForm(form);
                }
            }
            finally
            {
                getContext().decreaseLevel();
            }
        }
    }

    @Override
    public String getName()
    {
        return OperatorName.DRAW_OBJECT;
    }
}
