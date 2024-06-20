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
package org.sejda.sambox.pdmodel;

import static java.util.Optional.ofNullable;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectable;

/**
 * This class holds all of the name trees that are available at the document level.
 *
 * @author Ben Litchfield
 */
public class PDDocumentNameDictionary implements COSObjectable
{
    private final COSDictionary nameDictionary;

    /**
     * @param catalog The document catalog that this dictionary is part of.
     */
    public PDDocumentNameDictionary(PDDocumentCatalog catalog)
    {
        nameDictionary = ofNullable(catalog.getCOSObject()
                .getDictionaryObject(COSName.NAMES, COSDictionary.class)).orElseGet(
                COSDictionary::new);
        catalog.getCOSObject().putIfAbsent(COSName.NAMES, nameDictionary);
    }

    public PDDocumentNameDictionary(COSDictionary names)
    {
        nameDictionary = names;
    }

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos dictionary for this object.
     */
    @Override
    public COSDictionary getCOSObject()
    {
        return nameDictionary;
    }

    /**
     * Get the destination named tree node. The value in this name tree will be PDDestination
     * objects.
     *
     * @return The destination name tree node.
     */
    public PDDestinationNameTreeNode getDests()
    {
        return ofNullable(
                nameDictionary.getDictionaryObject(COSName.DESTS, COSDictionary.class)).map(
                PDDestinationNameTreeNode::new).orElse(null);
    }

    /**
     * Set the named destinations that are associated with this document.
     *
     * @param dests The destination names.
     */
    public void setDests(PDDestinationNameTreeNode dests)
    {
        nameDictionary.setItem(COSName.DESTS, dests);
    }

    /**
     * Get the embedded files named tree node. The value in this name tree will be
     * PDComplexFileSpecification objects.
     *
     * @return The embedded files name tree node.
     */
    public PDEmbeddedFilesNameTreeNode getEmbeddedFiles()
    {
        return ofNullable(nameDictionary.getDictionaryObject(COSName.EMBEDDED_FILES,
                COSDictionary.class)).map(PDEmbeddedFilesNameTreeNode::new).orElse(null);

    }

    /**
     * Set the named embedded files that are associated with this document.
     *
     * @param ef The new embedded files
     */
    public void setEmbeddedFiles(PDEmbeddedFilesNameTreeNode ef)
    {
        nameDictionary.setItem(COSName.EMBEDDED_FILES, ef);
    }

    /**
     * Get the document level javascript entries. The value in this name tree will be PDTextStream.
     *
     * @return The document level named javascript.
     */
    public PDJavascriptNameTreeNode getJavaScript()
    {
        return ofNullable(
                nameDictionary.getDictionaryObject(COSName.JAVA_SCRIPT, COSDictionary.class)).map(
                PDJavascriptNameTreeNode::new).orElse(null);
    }

    /**
     * Set the named javascript entries that are associated with this document.
     *
     * @param js The new Javascript entries.
     */
    public void setJavascript(PDJavascriptNameTreeNode js)
    {
        nameDictionary.setItem(COSName.JAVA_SCRIPT, js);
    }
}
