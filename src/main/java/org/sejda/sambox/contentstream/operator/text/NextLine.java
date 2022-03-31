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
package org.sejda.sambox.contentstream.operator.text;

import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.contentstream.operator.OperatorProcessor;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSFloat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * T*: Move to start of next text line.
 *
 * @author Laurent Huault
 */
public class NextLine extends OperatorProcessor
{
    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException
    {
        //move to start of next text line
        ArrayList<COSBase> args = new ArrayList<>(2);
        args.add(new COSFloat(0f));
        // this must be -leading instead of just leading as written in the
        // specification (p.369) the acrobat reader seems to implement it the same way
        args.add(new COSFloat(-getContext().getGraphicsState().getTextState().getLeading()));
        // use Td instead of repeating code
        getContext().processOperator(OperatorName.MOVE_TEXT, args);
    }

    @Override
    public String getName()
    {
        return OperatorName.NEXT_LINE;
    }
}
