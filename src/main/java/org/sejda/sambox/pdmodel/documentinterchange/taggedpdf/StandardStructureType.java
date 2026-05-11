/*
 * Created on 11/05/26
 * Copyright 2025 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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
package org.sejda.sambox.pdmodel.documentinterchange.taggedpdf;

/**
 * Standard structure types as defined in ISO 32000-2, Table 364.
 */
public enum StandardStructureType
{
    // Document-level structure
    DOCUMENT("Document"),
    DOCUMENT_FRAGMENT("DocumentFragment"),
    PART("Part"),
    SECT("Sect"),
    DIV("Div"),
    ASIDE("Aside"),
    NON_STRUCT("NonStruct"),

    // Paragraph-like block elements
    P("P"),
    H("H"),
    H1("H1"),
    H2("H2"),
    H3("H3"),
    H4("H4"),
    H5("H5"),
    H6("H6"),
    TITLE("Title"),
    FE_NOTE("FENote"),

    // Inline elements
    SUB("Sub"),
    LBL("Lbl"),
    SPAN("Span"),
    EM("Em"),
    STRONG("Strong"),

    // Linking and annotation
    LINK("Link"),
    ANNOT("Annot"),
    FORM("Form"),

    // Ruby and Warichu (East Asian typography)
    RUBY("Ruby"),
    RB("RB"),
    RT("RT"),
    RP("RP"),
    WARICHU("Warichu"),
    WT("WT"),
    WP("WP"),

    // List structure
    L("L"),
    LI("LI"),
    L_BODY("LBody"),

    // Table structure
    TABLE("Table"),
    TR("TR"),
    TH("TH"),
    TD("TD"),
    T_HEAD("THead"),
    T_BODY("TBody"),
    T_FOOT("TFoot"),

    // Illustration and miscellaneous
    CAPTION("Caption"),
    FIGURE("Figure"),
    FORMULA("Formula"),
    ARTIFACT("Artifact"),

    // Deprecated types (PDF 1.x, superseded in ISO 32000-2)
    @Deprecated ART("Art"),
    @Deprecated BLOCK_QUOTE("BlockQuote"),
    @Deprecated TOC("TOC"),
    @Deprecated TOCI("TOCI"),
    @Deprecated INDEX("Index"),
    @Deprecated PRIVATE("Private"),
    @Deprecated QUOTE("Quote"),
    @Deprecated NOTE("Note"),
    @Deprecated REFERENCE("Reference"),
    @Deprecated BIB_ENTRY("BibEntry"),
    @Deprecated CODE("Code");

    private final String type;

    StandardStructureType(String type)
    {
        this.type = type;
    }

    public String type()
    {
        return this.type;
    }
}
