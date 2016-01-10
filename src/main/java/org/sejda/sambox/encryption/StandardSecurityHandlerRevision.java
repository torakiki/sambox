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
 * Possible Standard Security Handler revisions with bytes length of the encryption key associated to that revision
 * 
 * @author Andrea Vacondio
 */
public enum StandardSecurityHandlerRevision
{
    R2(5, 2), R3(16, 3), R4(16, 4), R5(32, 5), R6(32, 6);

    /**
     * Length of the encryption key in bytes
     */
    public final int length;
    public final int revisionNumber;

    private StandardSecurityHandlerRevision(int length, int revisionNumber)
    {
        this.length = length;
        this.revisionNumber = revisionNumber;
    }

    /**
     * Requires this revision to be the same version as the given one. Throws an {@link EncryptionException} otherwise.
     * 
     * @param rev
     * @param message the exception message
     * @throws EncryptionException if rev is different
     */
    public void require(StandardSecurityHandlerRevision rev, String message)
    {
        require(this == rev, message);
    }

    /**
     * Requires this revision to be at least the same version as the given one. Throws an {@link EncryptionException}
     * otherwise.
     * 
     * @param rev
     * @param message
     */
    public void requireAtLeast(StandardSecurityHandlerRevision rev, String message)
    {
        require(rev.compareTo(this) <= 0, message);
    }

    private static void require(boolean condition, String message)
    {
        if (!condition)
        {
            throw new EncryptionException(message);
        }
    }
}
