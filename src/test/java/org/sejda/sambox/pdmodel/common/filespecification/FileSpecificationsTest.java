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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSString;

/**
 * @author Andrea Vacondio
 *
 */
public class FileSpecificationsTest
{

    @Test
    public void fileSpecificationFor()
    {
        assertNotNull(FileSpecifications.fileSpecificationFor(new COSDictionary()));
        assertNotNull(FileSpecifications.fileSpecificationFor(COSString.parseLiteral("Chuck")));
        assertNull(FileSpecifications.fileSpecificationFor(null));
        assertNull(FileSpecifications.fileSpecificationFor(COSInteger.ONE));
    }

}
