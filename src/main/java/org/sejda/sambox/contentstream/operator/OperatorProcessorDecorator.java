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

import static org.sejda.sambox.contentstream.operator.OperatorConsumer.NO_OP;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.sejda.sambox.contentstream.PDFStreamEngine;
import org.sejda.sambox.cos.COSBase;

/**
 * decorator for an {@link OperatorProcessor}
 * 
 * @author Andrea Vacondio
 */
public class OperatorProcessorDecorator extends OperatorProcessor
{
    private OperatorProcessor delegate;
    private Optional<OperatorConsumer> consumer;

    /**
     * Decorates the given {@link OperatorProcessor} with the given {@link OperatorConsumer} function
     * 
     * @param delegate
     * @param consumer
     */
    public OperatorProcessorDecorator(OperatorProcessor delegate, OperatorConsumer consumer)
    {
        this.delegate = delegate;
        this.consumer = Optional.ofNullable(consumer);
    }

    public OperatorProcessorDecorator(OperatorProcessor delegate)
    {
        this.delegate = delegate;
        this.consumer = Optional.empty();
    }

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        delegate.process(operator, operands);
        consumer.orElse(NO_OP).apply(operator, operands);
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    @Override
    public PDFStreamEngine getContext()
    {
        return delegate.getContext();
    }

    @Override
    public void setContext(PDFStreamEngine context)
    {
        delegate.setContext(context);
    }

    /**
     * Set the consumer that decorates this OperatorProcessor
     * 
     * @param consumer
     */
    public void setConsumer(OperatorConsumer consumer)
    {
        this.consumer = Optional.ofNullable(consumer);
    }
}
