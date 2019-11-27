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

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.pdmodel.graphics.image.PDImage;
import org.sejda.sambox.pdmodel.graphics.image.PDInlineImage;

/**
 * BI Begins an inline image.
 *
 * @author Ben Litchfield
 */
public final class BeginInlineImage extends GraphicsOperatorProcessor
{
    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        if (nonNull(operator.getImageData()) && operator.getImageData().length > 0)
        {
            PDImage image = new PDInlineImage(operator.getImageParameters(),
                    operator.getImageData(), getContext().getResources());
            getContext().drawImage(image);
        }
    }

    @Override
    public String getName()
    {
        return OperatorName.BEGIN_INLINE_IMAGE;
    }
}
