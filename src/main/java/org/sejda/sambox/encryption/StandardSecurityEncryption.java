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
package org.sejda.sambox.encryption;

/**
 * Available standard security encryptions
 * 
 * @author Andrea Vacondio
 *
 */
public enum StandardSecurityEncryption
{
    ARC4_128(StandardSecurityHandlerRevision.R3, 2), AES_128(StandardSecurityHandlerRevision.R4,
            4), AES_256(StandardSecurityHandlerRevision.R6, 5);

    public final int version;
    public final StandardSecurityHandlerRevision revision;

    private StandardSecurityEncryption(StandardSecurityHandlerRevision revision, int version)
    {
        this.revision = revision;
        this.version = version;
    }
}
