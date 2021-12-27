/*
 * Copyright 2019 Sober Lemur S.a.s. di Vacondio Andrea and Sejda BV
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
package org.sejda.sambox;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * @author Andrea Vacondio
 *
 */
@AnalyzeClasses(packages = "org.sejda.sambox")
public class TestCycles
{

    @ArchTest
    @ArchIgnore(reason = "There are cycles and it needs to be investigated")
    public static final ArchRule myRule = SlicesRuleDefinition.slices()
            .matching("org.sejda.sambox.(*)..").should().beFreeOfCycles();

}
