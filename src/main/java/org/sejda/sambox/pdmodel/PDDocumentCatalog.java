/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.sambox.util.SpecVersionUtils.V1_5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.common.PDDestinationOrAction;
import org.sejda.sambox.pdmodel.common.PDMetadata;
import org.sejda.sambox.pdmodel.common.PDPageLabels;
import org.sejda.sambox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.sejda.sambox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.sejda.sambox.pdmodel.graphics.color.PDOutputIntent;
import org.sejda.sambox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.sejda.sambox.pdmodel.interactive.action.PDActionFactory;
import org.sejda.sambox.pdmodel.interactive.action.PDDocumentCatalogAdditionalActions;
import org.sejda.sambox.pdmodel.interactive.action.PDURIDictionary;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.sejda.sambox.pdmodel.interactive.form.PDAcroForm;
import org.sejda.sambox.pdmodel.interactive.pagenavigation.PDThread;
import org.sejda.sambox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Document Catalog of a PDF.
 *
 * @author Ben Litchfield
 */
public class PDDocumentCatalog implements COSObjectable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDDocumentCatalog.class);

    private final COSDictionary root;
    private final PDDocument document;
    private PDAcroForm cachedAcroForm;

    /**
     * Constructor. AcroForm.
     *
     * @param doc The document that this catalog is part of.
     */
    public PDDocumentCatalog(PDDocument doc)
    {
        document = doc;
        root = new COSDictionary();
        root.setItem(COSName.TYPE, COSName.CATALOG);
        document.getDocument().getTrailer().getCOSObject().setItem(COSName.ROOT, root);
    }

    /**
     * Constructor.
     *
     * @param doc The document that this catalog is part of.
     * @param rootDictionary The root dictionary that this object wraps.
     */
    public PDDocumentCatalog(PDDocument doc, COSDictionary rootDictionary)
    {
        document = doc;
        root = rootDictionary;
    }

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos object that matches this Java object.
     */
    @Override
    public COSDictionary getCOSObject()
    {
        return root;
    }

    /**
     * Get the documents AcroForm. This will return null if no AcroForm is part of the document.
     *
     * @return The document's AcroForm.
     */
    public PDAcroForm getAcroForm()
    {
        if (cachedAcroForm == null)
        {
            COSDictionary dict = root.getDictionaryObject(COSName.ACRO_FORM, COSDictionary.class);
            cachedAcroForm = dict == null ? null : new PDAcroForm(document, dict);
        }
        return cachedAcroForm;
    }

    /**
     * Sets the AcroForm for this catalog.
     *
     * @param acroForm The new AcroForm.
     */
    public void setAcroForm(PDAcroForm acroForm)
    {
        root.setItem(COSName.ACRO_FORM, acroForm);
        cachedAcroForm = null;
    }

    /**
     * Returns all pages in the document, as a page tree.
     */
    public PDPageTree getPages()
    {
        // TODO cache this since it's probably going to be called often and we want to reduce GC
        return new PDPageTree((COSDictionary) root.getDictionaryObject(COSName.PAGES), document);
    }

    /**
     * Get the viewer preferences associated with this document or null if they do not exist.
     *
     * @return The document's viewer preferences.
     */
    public PDViewerPreferences getViewerPreferences()
    {
        return ofNullable(root.getDictionaryObject(COSName.VIEWER_PREFERENCES, COSDictionary.class))
                .map(PDViewerPreferences::new).orElse(null);
    }

    /**
     * Sets the viewer preferences.
     *
     * @param prefs The new viewer preferences.
     */
    public void setViewerPreferences(PDViewerPreferences prefs)
    {
        root.setItem(COSName.VIEWER_PREFERENCES, prefs);
    }

    /**
     * Get the outline associated with this document or null if it does not exist.
     *
     * @return The document's outline.
     */
    public PDDocumentOutline getDocumentOutline()
    {
        return ofNullable(root.getDictionaryObject(COSName.OUTLINES, COSDictionary.class))
                .map(PDDocumentOutline::new).orElse(null);
    }

    /**
     * Sets the document outlines.
     *
     * @param outlines The new document outlines.
     */
    public void setDocumentOutline(PDDocumentOutline outlines)
    {
        root.setItem(COSName.OUTLINES, outlines);
    }

    /**
     * Returns the document's article threads.
     */
    public List<PDThread> getThreads()
    {
        COSArray array = (COSArray) root.getDictionaryObject(COSName.THREADS);
        if (array == null)
        {
            array = new COSArray();
            root.setItem(COSName.THREADS, array);
        }
        List<PDThread> pdObjects = new ArrayList<>();
        for (int i = 0; i < array.size(); i++)
        {
            pdObjects.add(new PDThread((COSDictionary) array.getObject(i)));
        }
        return new COSArrayList<>(pdObjects, array);
    }

    /**
     * Sets the list of threads for this pdf document.
     *
     * @param threads The list of threads, or null to clear it.
     */
    public void setThreads(List<PDThread> threads)
    {
        root.setItem(COSName.THREADS, COSArrayList.converterToCOSArray(threads));
    }

    /**
     * Get the metadata that is part of the document catalog. This will return null if there is no meta data for this
     * object.
     *
     * @return The metadata for this object.
     */
    public PDMetadata getMetadata()
    {
        return ofNullable(root.getDictionaryObject(COSName.METADATA, COSStream.class))
                .map(PDMetadata::new).orElse(null);
    }

    /**
     * Sets the metadata for this object. This can be null.
     *
     * @param meta The meta data for this object.
     */
    public void setMetadata(PDMetadata meta)
    {
        root.setItem(COSName.METADATA, meta);
    }

    /**
     * Sets the Document Open Action for this object.
     *
     * @param action The action you want to perform.
     */
    public void setOpenAction(PDDestinationOrAction action)
    {
        root.setItem(COSName.OPEN_ACTION, action);
    }

    /**
     * Get the Document Open Action for this object.
     *
     * @return The action to perform when the document is opened.
     */
    public PDDestinationOrAction getOpenAction() throws IOException
    {
        COSBase openAction = root.getDictionaryObject(COSName.OPEN_ACTION);
        if (nonNull(openAction))
        {
            if (openAction instanceof COSDictionary)
            {
                return PDActionFactory.createAction((COSDictionary) openAction);
            }
            else if (openAction instanceof COSArray)
            {
                return PDDestination.create(openAction);
            }
            LOG.warn("Invalid OpenAction {}", openAction);
        }
        return null;
    }

    /**
     * @return The Additional Actions for this Document
     */
    public PDDocumentCatalogAdditionalActions getActions()
    {
        COSDictionary addAction = root.getDictionaryObject(COSName.AA, COSDictionary.class);
        if (addAction == null)
        {
            addAction = new COSDictionary();
            root.setItem(COSName.AA, addAction);
        }
        return new PDDocumentCatalogAdditionalActions(addAction);
    }

    /**
     * Sets the additional actions for the document.
     *
     * @param actions The actions that are associated with this document.
     */
    public void setActions(PDDocumentCatalogAdditionalActions actions)
    {
        root.setItem(COSName.AA, actions);
    }

    /**
     * @return The names dictionary for this document or null if none exist.
     */
    public PDDocumentNameDictionary getNames()
    {
        return ofNullable(root.getDictionaryObject(COSName.NAMES, COSDictionary.class))
                .map(PDDocumentNameDictionary::new).orElse(null);
    }

    /**
     * @return The named destinations dictionary for this document or null if none exists.
     */
    public PDDocumentNameDestinationDictionary getDests()
    {
        return ofNullable(root.getDictionaryObject(COSName.DESTS, COSDictionary.class))
                .map(PDDocumentNameDestinationDictionary::new).orElse(null);
    }

    /**
     * Sets the names dictionary for the document.
     *
     * @param names The names dictionary that is associated with this document.
     */
    public void setNames(PDDocumentNameDictionary names)
    {
        root.setItem(COSName.NAMES, names);
    }

    /**
     * Get info about doc's usage of tagged features. This will return null if there is no information.
     *
     * @return The new mark info.
     */
    public PDMarkInfo getMarkInfo()
    {
        return ofNullable(root.getDictionaryObject(COSName.MARK_INFO, COSDictionary.class))
                .map(PDMarkInfo::new).orElse(null);
    }

    /**
     * Set information about the doc's usage of tagged features.
     *
     * @param markInfo The new MarkInfo data.
     */
    public void setMarkInfo(PDMarkInfo markInfo)
    {
        root.setItem(COSName.MARK_INFO, markInfo);
    }

    /**
     * Get the list of OutputIntents defined in the document.
     *
     * @return The list of PDOutputIntent
     */
    public List<PDOutputIntent> getOutputIntents()
    {
        List<PDOutputIntent> retval = new ArrayList<>();
        COSArray array = root.getDictionaryObject(COSName.OUTPUT_INTENTS, COSArray.class);
        if (array != null)
        {
            for (COSBase cosBase : array)
            {
                PDOutputIntent oi = new PDOutputIntent((COSDictionary) cosBase.getCOSObject());
                retval.add(oi);
            }
        }
        return retval;
    }

    /**
     * Add an OutputIntent to the list. If there is not OutputIntent, the list is created and the first element added.
     *
     * @param outputIntent the OutputIntent to add.
     */
    public void addOutputIntent(PDOutputIntent outputIntent)
    {
        COSArray array = root.getDictionaryObject(COSName.OUTPUT_INTENTS, COSArray.class);
        if (array == null)
        {
            array = new COSArray();
            root.setItem(COSName.OUTPUT_INTENTS, array);
        }
        array.add(outputIntent.getCOSObject());
    }

    /**
     * Replace the list of OutputIntents of the document.
     *
     * @param outputIntents the list of OutputIntents, if the list is empty all OutputIntents are removed.
     */
    public void setOutputIntents(List<PDOutputIntent> outputIntents)
    {
        COSArray array = new COSArray();
        for (PDOutputIntent intent : outputIntents)
        {
            array.add(intent.getCOSObject());
        }
        root.setItem(COSName.OUTPUT_INTENTS, array);
    }

    /**
     * Returns the page display mode.
     */
    public PageMode getPageMode()
    {
        String mode = root.getNameAsString(COSName.PAGE_MODE);
        if (mode != null)
        {
            try
            {
                return PageMode.fromString(mode);
            }
            catch (IllegalArgumentException ex)
            {
                LOG.debug(String.format("Unrecognized page mode %s", mode));
            }
        }
        return PageMode.USE_NONE;
    }

    /**
     * Sets the page mode.
     *
     * @param mode The new page mode.
     */
    public void setPageMode(PageMode mode)
    {
        root.setName(COSName.PAGE_MODE, mode.stringValue());
    }

    /**
     * Returns the page layout.
     */
    public PageLayout getPageLayout()
    {
        String mode = root.getNameAsString(COSName.PAGE_LAYOUT);
        if (mode != null)
        {
            try
            {
                return PageLayout.fromString(mode);
            }
            catch (IllegalArgumentException ex)
            {
                LOG.debug(String.format("Unrecognized page layout %s", mode));
            }
        }
        return PageLayout.SINGLE_PAGE;
    }

    /**
     * Sets the page layout.
     *
     * @param layout The new page layout.
     */
    public void setPageLayout(PageLayout layout)
    {
        root.setName(COSName.PAGE_LAYOUT, layout.stringValue());
    }

    /**
     * Returns the document-level URI.
     */
    public PDURIDictionary getURI()
    {
        return ofNullable(root.getDictionaryObject(COSName.URI, COSDictionary.class))
                .map(PDURIDictionary::new).orElse(null);
    }

    /**
     * Sets the document level URI.
     *
     * @param uri The new document level URI.
     */
    public void setURI(PDURIDictionary uri)
    {
        root.setItem(COSName.URI, uri);
    }

    /**
     * Get the document's structure tree root, or null if none exists.
     */
    public PDStructureTreeRoot getStructureTreeRoot()
    {
        return ofNullable(root.getDictionaryObject(COSName.STRUCT_TREE_ROOT, COSDictionary.class))
                .map(PDStructureTreeRoot::new).orElse(null);
    }

    /**
     * Sets the document's structure tree root.
     *
     * @param treeRoot The new structure tree.
     */
    public void setStructureTreeRoot(PDStructureTreeRoot treeRoot)
    {
        root.setItem(COSName.STRUCT_TREE_ROOT, treeRoot);
    }

    /**
     * Returns the language for the document, or null.
     */
    public String getLanguage()
    {
        return root.getString(COSName.LANG);
    }

    /**
     * Sets the Language for the document.
     *
     * @param language The new document language.
     */
    public void setLanguage(String language)
    {
        root.setString(COSName.LANG, language);
    }

    /**
     * Returns the PDF specification version this document conforms to.
     *
     * @return the PDF version (e.g. "1.4")
     */
    public String getVersion()
    {
        return root.getNameAsString(COSName.VERSION);
    }

    /**
     * Sets the PDF specification version this document conforms to.
     *
     * @param version the PDF version
     */
    public void setVersion(String version)
    {
        root.setName(COSName.VERSION, version);
    }

    /**
     * Returns the page labels descriptor of the document.
     *
     * @return the page labels descriptor of the document.
     * @throws IOException If there is a problem retrieving the page labels.
     */
    public PDPageLabels getPageLabels() throws IOException
    {
        COSDictionary dict = (COSDictionary) root.getDictionaryObject(COSName.PAGE_LABELS);
        return dict == null ? null : new PDPageLabels(dict);
    }

    /**
     * Sets the page label descriptor for the document.
     *
     * @param labels the new page label descriptor to set.
     */
    public void setPageLabels(PDPageLabels labels)
    {
        root.setItem(COSName.PAGE_LABELS, labels);
    }

    /**
     * Get the optional content properties dictionary associated with this document.
     *
     * @return the optional properties dictionary or null if it is not present
     */
    public PDOptionalContentProperties getOCProperties()
    {
        return ofNullable(root.getDictionaryObject(COSName.OCPROPERTIES, COSDictionary.class))
                .map(PDOptionalContentProperties::new).orElse(null);
    }

    /**
     * Sets the optional content properties dictionary.
     *
     * @param ocProperties the optional properties dictionary
     */
    public void setOCProperties(PDOptionalContentProperties ocProperties)
    {
        root.setItem(COSName.OCPROPERTIES, ocProperties);

        // optional content groups require PDF 1.5
        if (ocProperties != null)
        {
            document.requireMinVersion(V1_5);
        }
    }

    /**
     * Looks up for the {@link PDPageDestination} referenced by the given {@link PDNamedDestination}
     * 
     * @param namedDest
     * @return the destination or null if nothing is fond
     * @throws IOException
     */
    public PDPageDestination findNamedDestinationPage(PDNamedDestination namedDest)
            throws IOException
    {
        PDPageDestination namesDest = ofNullable(getNames()).map(PDDocumentNameDictionary::getDests)
                .map(tree -> tree.getValue(namedDest.getNamedDestination())).orElse(null);
        if (namesDest == null)
        {
            // Look up /Dests dictionary from catalog
            PDDocumentNameDestinationDictionary nameDestDict = getDests();
            if (nameDestDict != null)
            {
                return (PDPageDestination) nameDestDict
                        .getDestination(namedDest.getNamedDestination());
            }
        }
        return namesDest;
    }
}
