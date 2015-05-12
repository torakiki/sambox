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
package org.apache.pdfbox.input.source;

import static java.util.Objects.requireNonNull;
import static org.apache.pdfbox.util.RequireUtils.requireNotBlank;

import java.io.File;
import java.io.IOException;

/**
 * @author Andrea Vacondio
 *
 */
public abstract class BaseSeekableSource implements SeekableSource
{

    private boolean open = true;
    private String id;

    public BaseSeekableSource(String id)
    {
        requireNotBlank(id, "SeekableSource id cannot be blank");
        this.id = id;
    }

    public BaseSeekableSource(File file)
    {
        requireNonNull(file);
        this.id = file.getAbsolutePath();
    }

    @Override
    public boolean isOpen()
    {
        return open;
    }

    @Override
    public void close() throws IOException
    {
        this.open = false;
    }

    /**
     * @return the unique id for this source
     */
    @Override
    public String id()
    {
        return id;
    }
}
