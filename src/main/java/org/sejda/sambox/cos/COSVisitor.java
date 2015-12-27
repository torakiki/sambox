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
package org.sejda.sambox.cos;

import java.io.IOException;

/**
 * Visitor interface to visit COS objects.
 * 
 * @author Andrea Vacondio
 *
 */
public interface COSVisitor
{
    default void visit(COSDocument value) throws IOException
    {
        // nothings
    }

    default void visit(COSArray value) throws IOException
    {
        // nothings
    }

    default void visit(COSBoolean value) throws IOException
    {
        // nothings
    }

    default void visit(COSDictionary value) throws IOException
    {
        // nothings
    }

    default void visit(COSFloat value) throws IOException
    {
        // nothings
    }

    default void visit(COSInteger value) throws IOException
    {
        // nothings
    }

    default void visit(COSName value) throws IOException
    {
        // nothings
    }

    default void visit(COSNull value) throws IOException
    {
        // nothings
    }

    default void visit(COSStream value) throws IOException
    {
        // nothings
    }

    default void visit(COSString value) throws IOException
    {
        // nothings
    }

    default void visit(IndirectCOSObjectReference value) throws IOException
    {
        // nothings
    }

}
