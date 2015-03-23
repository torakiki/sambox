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
package org.apache.pdfbox.xref;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSDictionary;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefParser
{
    private static final Log LOG = LogFactory.getLog(XrefParser.class);

    private Xref xref = new Xref();
    private TrailerMerger trailerMerger = new TrailerMerger();

    public void parse()
    {
        // do something
    }

    public COSDictionary getTrailer()
    {
        return this.trailerMerger.getTrailer();
    }

    public Xref getXref()
    {
        return this.xref;
    }

}
