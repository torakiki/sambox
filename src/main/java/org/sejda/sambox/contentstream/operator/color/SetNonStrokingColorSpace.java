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
package org.sejda.sambox.contentstream.operator.color;

import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.contentstream.operator.OperatorProcessor;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * cs: Sets the non-stroking color space.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public class SetNonStrokingColorSpace extends OperatorProcessor
{

    private static final Logger LOG = LoggerFactory.getLogger(SetNonStrokingColorSpace.class);
    
    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException
    {
        if(arguments == null || arguments.size() == 0)
        {
            LOG.warn("Ignoring SetNonStrokingColorSpace operator without operands");
            return;
        }
        
        COSBase base = arguments.get(0);

        if (base instanceof COSName name)
        {
            try
            {
                PDColorSpace cs = getContext().getResources().getColorSpace(name);
                getContext().getGraphicsState().setNonStrokingColorSpace(cs);
                getContext().getGraphicsState().setNonStrokingColor(cs.getInitialColor());
            }
            catch (IOException ex)
            {
                LOG.warn("Ignoring SetNonStrokingColorSpace operator, parsing colorspace caused an error", ex);
            }
        }
    }

    @Override
    public String getName()
    {
        return OperatorName.NON_STROKING_COLORSPACE;
    }
}
