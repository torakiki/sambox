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
package org.sejda.sambox.pdmodel.interactive.form;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;

import java.io.IOException;

public class DefaultAppearanceHelper {

    public static COSString getDefaultAppearance(PDField field) throws IOException {
        return getDefaultAppearance(field.getInheritableAttribute(COSName.DA));
    }

    public static COSString getDefaultAppearance(COSBase defaultAppearance) throws IOException {
        if (defaultAppearance == null) {
            return null;
        }

        if(defaultAppearance instanceof COSString) {
            return (COSString) defaultAppearance;
        } else if(defaultAppearance instanceof COSName) {
            String value = ((COSName) defaultAppearance).getName();
            return COSString.parseLiteral(value);
        } else {
            // IOException is probably not the best choice here, RuntimeException would likely be better
            // but existing code seems to expect IOException in a lot of places
            throw new IOException("Expected DA to be COSString, got: " + defaultAppearance.getClass().getSimpleName());
        }
    }
}
