package org.sejda.sambox.output;
/*
 * Copyright 2024 Sober Lemur S.r.l.
 * Copyright 2024 Sejda BV
 *
 * Created 20/06/24
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.sejda.sambox.cos.COSVisitor;

/**
 * Component visiting {@link org.sejda.sambox.cos.COSBase} objects right before they are written
 * down. It can be used to audit or transform objects before they are written.
 *
 * @author Andrea Vacondio
 */
public interface PreSaveCOSTransformer extends COSVisitor
{
}
