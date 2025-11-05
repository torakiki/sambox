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
package org.sejda.sambox.contentstream.operator.graphics;

import static java.util.Objects.nonNull;
import static org.sejda.commons.util.RequireUtils.require;

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.MissingOperandException;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.graphics.form.PDTransparencyGroup;
import org.sejda.sambox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Do: Draws an XObject.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public final class DrawObject extends GraphicsOperatorProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(DrawObject.class);

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        require(!operands.isEmpty(), () -> new MissingOperandException(operator, operands));
        if (operands.get(0) instanceof COSName objectName)
        {

            PDXObject xobject = getContext().getResources().getXObject(objectName);

            require(nonNull(xobject), () -> new MissingOperandException(operator, operands));
            if (xobject instanceof PDImageXObject image)
            {
                getContext().drawImage(image);
            }
            else if (xobject instanceof PDFormXObject xform)
            {
                try
                {
                    getContext().increaseLevel();
                    if (getContext().getLevel() > 50)
                    {
                        LOG.error("Recursion is too deep, skipping form XObject");
                        return;
                    }
                    if (xform instanceof PDTransparencyGroup transparencyGroup)
                    {
                        getContext().showTransparencyGroup(transparencyGroup);
                    }
                    else
                    {
                        getContext().showForm(xform);
                    }
                }
                finally
                {
                    getContext().decreaseLevel();
                }
            }
        }
    }

    @Override
    public String getName()
    {
        return OperatorName.DRAW_OBJECT;
    }
}
