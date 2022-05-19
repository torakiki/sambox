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

import org.sejda.sambox.contentstream.PDFStreamEngine;
import org.sejda.sambox.cos.COSBase;

/**
 * Processes a PDF operator.
 *
 * @author Laurent Huault
 */
public abstract class OperatorProcessor
{
    private PDFStreamEngine context;

    /**
     * Creates a new OperatorProcessor.
     */
    protected OperatorProcessor()
    {
    }

    /**
     * @return the processing context
     */
    public PDFStreamEngine getContext()
    {
        return context;
    }

    /**
     * Sets the processing context.
     *
     * @param context the processing context.
     */
    public void setContext(PDFStreamEngine context)
    {
        this.context = context;
    }

    /**
     * Process the operator.
     *
     * @param operator the operator to process
     * @param operands the operands to use when processing
     * @throws IOException if the operator cannot be processed
     */
    public abstract void process(Operator operator, List<COSBase> operands) throws IOException;

    /**
     * @return the name of this operator, e.g. "BI".
     */
    public abstract String getName();

    /**
     * Check whether all operands list elements are an instance of a specific class.
     *
     * @param operands The operands list.
     * @param clazz    The expected class.
     * @return the boolean
     */
    public static boolean checkArrayTypesClass(List<COSBase> operands, Class<?> clazz)
    {
        for (COSBase base : operands)
        {
            if (!clazz.isInstance(base))
            {
                return false;
            }
        }
        return true;
    }
}
