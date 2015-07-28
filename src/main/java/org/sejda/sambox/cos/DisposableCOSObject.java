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

/**
 * A {@link COSObjectable} that can be disposed once no longer need. What "dispose" actually means depends on the
 * implementation, the idea is to be able to signal that an object is no longer needed (EX. because it has already been
 * written to the output) and can be freed.
 * 
 * @author Andrea Vacondio
 *
 */
public interface DisposableCOSObject extends COSObjectable
{
    /**
     * release the object wrapped by this {@link COSObjectable}
     */
    void releaseCOSObject();
}
