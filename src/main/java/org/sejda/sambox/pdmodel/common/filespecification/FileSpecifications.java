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
package org.sejda.sambox.pdmodel.common.filespecification;

import static java.util.Objects.nonNull;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory methods for {@link PDFileSpecification}
 * 
 * @author Andrea Vacondio
 *
 */
public final class FileSpecifications
{
    private static final Logger LOG = LoggerFactory.getLogger(PDFileSpecification.class);

    private FileSpecifications()
    {
        // hide
    }

    /**
     * Factory method for a a file specification that can be either a COSString or a COSDictionary.
     *
     * @param base The cos object that describes the fs.
     * @return The file specification for the COSBase object or null in case of invalid input.
     */
    public static PDFileSpecification fileSpecificationFor(COSBase base)
    {
        if (nonNull(base))
        {
            if (base instanceof COSString)
            {
                return new PDSimpleFileSpecification((COSString) base);
            }
            if (base instanceof COSDictionary)
            {
                return new PDComplexFileSpecification((COSDictionary) base);
            }
            LOG.warn("Invalid file specification type {}", base.getClass());
        }
        return null;
    }
}
