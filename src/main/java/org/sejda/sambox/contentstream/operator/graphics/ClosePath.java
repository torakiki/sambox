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

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * h Close the path.
 *
 * @author Ben Litchfield
 */
public final class ClosePath extends GraphicsOperatorProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(ClosePath.class);
    
    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        if (getContext().getCurrentPoint() == null)
        {
            LOG.warn("ClosePath without initial MoveTo");
            return;
        }
        getContext().closePath();
    }

    @Override
    public String getName()
    {
        return OperatorName.CLOSE_PATH;
    }
}
