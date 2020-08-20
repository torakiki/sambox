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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDFontFactory;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDPattern;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.graphics.image.PDImageXObject;
import org.sejda.sambox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.sejda.sambox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.sejda.sambox.pdmodel.graphics.shading.PDShading;
import org.sejda.sambox.pdmodel.graphics.state.PDExtendedGraphicsState;

/**
 * A set of resources available at the page/pages/stream level.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public final class PDResources implements COSObjectable
{
    private final COSDictionary resources;
    private final ResourceCache cache;

    // PDFBOX-3442 cache fonts that are not indirect objects, as these aren't cached in ResourceCache
    // and this would result in huge memory footprint in text extraction
    private final Map<COSName, SoftReference<PDFont>> directFontCache = new HashMap<>();

    /**
     * Constructor for embedding.
     */
    public PDResources()
    {
        resources = new COSDictionary();
        cache = null;
    }

    /**
     * Constructor for reading.
     *
     * @param resourceDictionary The cos dictionary for this resource.
     */
    public PDResources(COSDictionary resourceDictionary)
    {
        if (resourceDictionary == null)
        {
            throw new IllegalArgumentException("resourceDictionary is null");
        }
        resources = resourceDictionary;
        cache = null;
    }

    /**
     * Constructor for reading.
     *
     * @param resourceDictionary The cos dictionary for this resource.
     * @param resourceCache The document's resource cache, may be null.
     */
    public PDResources(COSDictionary resourceDictionary, ResourceCache resourceCache)
    {
        if (resourceDictionary == null)
        {
            throw new IllegalArgumentException("resourceDictionary is null");
        }
        resources = resourceDictionary;
        cache = resourceCache;
    }

    /**
     * Returns the underlying dictionary.
     */
    @Override
    public COSDictionary getCOSObject()
    {
        return resources;
    }

    /**
     * Returns the font resource with the given name, or null if none exists.
     *
     * @param name Name of the font resource.
     * @throws java.io.IOException if something went wrong.
     */
    public PDFont getFont(COSName name) throws IOException
    {
        COSObjectKey key = getIndirectKey(COSName.FONT, name);
        if (cache != null && key != null)
        {
            PDFont cached = cache.getFont(key);
            if (cached != null)
            {
                return cached;
            }
        }
        else if (key == null)
        {
            SoftReference<PDFont> ref = directFontCache.get(name);
            if (ref != null)
            {
                PDFont cached = ref.get();
                if (cached != null)
                {
                    return cached;
                }
            }
        }

        PDFont font = null;
        COSDictionary dict = (COSDictionary) get(COSName.FONT, name);
        if (dict != null)
        {
            font = PDFontFactory.createFont(dict, cache);
        }

        if (cache != null && key != null)
        {
            cache.put(key, font);
        }
        else if (key == null)
        {
            directFontCache.put(name, new SoftReference<>(font));
        }

        return font;
    }

    /**
     * Returns the color space resource with the given name, or null if none exists.
     *
     * @param name Name of the color space resource.
     * @throws java.io.IOException if something went wrong.
     */
    public PDColorSpace getColorSpace(COSName name) throws IOException
    {
        return getColorSpace(name, false);
    }

    /**
     * Returns the color space resource with the given name, or null if none exists. This method is for PDFBox internal
     * use only, others should use {@link getColorSpace(COSName)}.
     *
     * @param name Name of the color space resource.
     * @param wasDefault if current color space was used by a default color space. This parameter is to
     * @return a new color space.
     * @throws IOException if something went wrong.
     */
    public PDColorSpace getColorSpace(COSName name, boolean wasDefault) throws IOException
    {
        COSObjectKey key = getIndirectKey(COSName.COLORSPACE, name);
        if (cache != null && key != null)
        {
            PDColorSpace cached = cache.getColorSpace(key);
            if (cached != null)
            {
                return cached;
            }
        }

        // get the instance
        PDColorSpace colorSpace;
        COSBase object = get(COSName.COLORSPACE, name);
        if (object != null)
        {
            colorSpace = PDColorSpace.create(object, this, wasDefault);
        }
        else
        {
            colorSpace = PDColorSpace.create(name, this, wasDefault);
        }

        // we can't cache PDPattern, because it holds page resources, see PDFBOX-2370
        if (cache != null && !(colorSpace instanceof PDPattern))
        {
            cache.put(key, colorSpace);
        }
        return colorSpace;
    }

    /**
     * Returns true if the given color space name exists in these resources.
     *
     * @param name Name of the color space resource.
     */
    public boolean hasColorSpace(COSName name)
    {
        return get(COSName.COLORSPACE, name) != null;
    }

    /**
     * Returns the extended graphics state resource with the given name, or null if none exists.
     *
     * @param name Name of the graphics state resource.
     */
    public PDExtendedGraphicsState getExtGState(COSName name)
    {
        COSObjectKey key = getIndirectKey(COSName.EXT_G_STATE, name);
        if (cache != null && key != null)
        {
            PDExtendedGraphicsState cached = cache.getExtGState(key);
            if (cached != null)
            {
                return cached;
            }
        }

        // get the instance
        PDExtendedGraphicsState extGState = null;
        COSBase base = get(COSName.EXT_G_STATE, name);
        COSDictionary dict = null;
        
        if (base instanceof COSDictionary)
        {
            dict = (COSDictionary) base;    
        }
        
        if (dict != null)
        {
            extGState = new PDExtendedGraphicsState(dict);
        }

        if (cache != null)
        {
            cache.put(key, extGState);
        }
        return extGState;
    }

    /**
     * Returns the shading resource with the given name, or null if none exists.
     *
     * @param name Name of the shading resource.
     * @throws java.io.IOException if something went wrong.
     */
    public PDShading getShading(COSName name) throws IOException
    {
        COSObjectKey key = getIndirectKey(COSName.SHADING, name);
        if (cache != null && key != null)
        {
            PDShading cached = cache.getShading(key);
            if (cached != null)
            {
                return cached;
            }
        }

        // get the instance
        PDShading shading = null;
        COSDictionary dict = (COSDictionary) get(COSName.SHADING, name);
        if (dict != null)
        {
            shading = PDShading.create(dict);
        }

        if (cache != null)
        {
            cache.put(key, shading);
        }
        return shading;
    }

    /**
     * Returns the pattern resource with the given name, or null if none exists.
     *
     * @param name Name of the pattern resource.
     * @throws java.io.IOException if something went wrong.
     */
    public PDAbstractPattern getPattern(COSName name) throws IOException
    {
        COSObjectKey key = getIndirectKey(COSName.PATTERN, name);
        if (cache != null && key != null)
        {
            PDAbstractPattern cached = cache.getPattern(key);
            if (cached != null)
            {
                return cached;
            }
        }

        // get the instance
        PDAbstractPattern pattern = null;
        COSDictionary dict = (COSDictionary) get(COSName.PATTERN, name);
        if (dict != null)
        {
            pattern = PDAbstractPattern.create(dict);
        }

        if (cache != null)
        {
            cache.put(key, pattern);
        }
        return pattern;
    }

    /**
     * Returns the property list resource with the given name, or null if none exists.
     *
     * @param name Name of the property list resource.
     */
    public PDPropertyList getProperties(COSName name)
    {
        COSObjectKey key = getIndirectKey(COSName.PROPERTIES, name);
        if (cache != null && key != null)
        {
            PDPropertyList cached = cache.getProperties(key);
            if (cached != null)
            {
                return cached;
            }
        }

        // get the instance
        PDPropertyList propertyList = null;
        COSDictionary dict = (COSDictionary) get(COSName.PROPERTIES, name);
        if (dict != null)
        {
            propertyList = PDPropertyList.create(dict);
        }

        if (cache != null)
        {
            cache.put(key, propertyList);
        }
        return propertyList;
    }

    /**
     * Tells whether the XObject resource with the given name is an image.
     *
     * @param name Name of the XObject resource.
     * @return true if it is an image XObject, false if not.
     */
    public boolean isImageXObject(COSName name)
    {
        // get the instance
        return Optional.ofNullable(get(COSName.XOBJECT, name)).map(COSBase::getCOSObject)
                .filter(s -> s instanceof COSStream).map(s -> (COSStream) s)
                .map(s -> COSName.IMAGE.equals(s.getCOSName(COSName.SUBTYPE))).orElse(false);
    }

    /**
     * Tells whether the XObject resource with the given name is an form.
     *
     * @param name Name of the XObject resource.
     * @return true if it is an form XObject, false if not.
     */
    public boolean isFormXObject(COSName name)
    {
        // get the instance
        return Optional.ofNullable(get(COSName.XOBJECT, name)).map(COSBase::getCOSObject)
                .map(s -> (COSStream) s)
                .map(s -> COSName.FORM.equals(s.getCOSName(COSName.SUBTYPE))).orElse(false);
    }

    /**
     * Returns the XObject resource with the given name, or null if none exists.
     *
     * @param name Name of the XObject resource.
     * @throws java.io.IOException if something went wrong.
     */
    public PDXObject getXObject(COSName name) throws IOException
    {
        COSObjectKey key = getIndirectKey(COSName.XOBJECT, name);
        if (cache != null && key != null)
        {
            PDXObject cached = cache.getXObject(key);
            if (cached != null)
            {
                return cached;
            }
        }

        // get the instance
        PDXObject xobject;
        COSBase value = get(COSName.XOBJECT, name);
        if (value == null)
        {
            xobject = null;
        }
        else
        {
            xobject = PDXObject.createXObject(value.getCOSObject(), this);
        }

        if (cache != null && isAllowedCache(xobject))
        {
            cache.put(key, xobject);
        }
        return xobject;
    }

    private boolean isAllowedCache(PDXObject xobject)
    {
        if (xobject instanceof PDImageXObject)
        {
            COSBase colorSpace = xobject.getCOSObject().getDictionaryObject(COSName.COLORSPACE);
            if (colorSpace instanceof COSName)
            {
                // don't cache if it might use page resources, see PDFBOX-2370 and PDFBOX-3484
                COSName colorSpaceName = (COSName) colorSpace;
                if (colorSpaceName.equals(COSName.DEVICECMYK)
                        && hasColorSpace(COSName.DEFAULT_CMYK))
                {
                    return false;
                }
                if (colorSpaceName.equals(COSName.DEVICERGB) && hasColorSpace(COSName.DEFAULT_RGB))
                {
                    return false;
                }
                if (colorSpaceName.equals(COSName.DEVICEGRAY)
                        && hasColorSpace(COSName.DEFAULT_GRAY))
                {
                    return false;
                }
                if (hasColorSpace(colorSpaceName))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the resource with the given name and kind as an indirect object, or null.
     */
    public COSObjectKey getIndirectKey(COSName kind, COSName name)
    {
        COSBase found = Optional
                .ofNullable(resources.getDictionaryObject(kind, COSDictionary.class))
                .map(d -> d.getItem(name)).orElse(null);
        if (found instanceof ExistingIndirectCOSObject)
        {
            return ((ExistingIndirectCOSObject) found).id().objectIdentifier;
        }

        if (found != null && found.id() != null)
        {
            return found.id().objectIdentifier;
        }

        return null;
    }

    /**
     * Returns the resource with the given name and kind, or null.
     */
    private COSBase get(COSName kind, COSName name)
    {
        return ofNullable(resources.getDictionaryObject(kind, COSDictionary.class))
                .map(d -> d.getDictionaryObject(name)).orElse(null);
    }

    /**
     * Returns the names of the color space resources, if any.
     */
    public Iterable<COSName> getColorSpaceNames()
    {
        return getNames(COSName.COLORSPACE);
    }

    /**
     * Returns the names of the XObject resources, if any.
     */
    public Iterable<COSName> getXObjectNames()
    {
        return getNames(COSName.XOBJECT);
    }

    /**
     * Returns the names of the font resources, if any.
     */
    public Iterable<COSName> getFontNames()
    {
        return getNames(COSName.FONT);
    }

    /**
     * Returns the names of the property list resources, if any.
     */
    public Iterable<COSName> getPropertiesNames()
    {
        return getNames(COSName.PROPERTIES);
    }

    /**
     * Returns the names of the shading resources, if any.
     */
    public Iterable<COSName> getShadingNames()
    {
        return getNames(COSName.SHADING);
    }

    /**
     * Returns the names of the pattern resources, if any.
     */
    public Iterable<COSName> getPatternNames()
    {
        return getNames(COSName.PATTERN);
    }

    /**
     * Returns the names of the extended graphics state resources, if any.
     */
    public Iterable<COSName> getExtGStateNames()
    {
        return getNames(COSName.EXT_G_STATE);
    }

    /**
     * Returns the resource names of the given kind.
     */
    private Iterable<COSName> getNames(COSName kind)
    {
        return ofNullable(resources.getDictionaryObject(kind, COSDictionary.class))
                .map(COSDictionary::keySet).orElseGet(Collections::emptySet);
    }

    /**
     * Adds the given font to the resources of the current page and returns the name for the new resources. Returns the
     * existing resource name if the given item already exists.
     *
     * @param font the font to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDFont font)
    {
        return add(COSName.FONT, "F", font);
    }

    /**
     * Adds the given color space to the resources of the current page and returns the name for the new resources.
     * Returns the existing resource name if the given item already exists.
     *
     * @param colorSpace the color space to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDColorSpace colorSpace)
    {
        return add(COSName.COLORSPACE, "cs", colorSpace);
    }

    /**
     * Adds the given extended graphics state to the resources of the current page and returns the name for the new
     * resources. Returns the existing resource name if the given item already exists.
     *
     * @param extGState the extended graphics state to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDExtendedGraphicsState extGState)
    {
        return add(COSName.EXT_G_STATE, "gs", extGState);
    }

    /**
     * Adds the given shading to the resources of the current page and returns the name for the new resources. Returns
     * the existing resource name if the given item already exists.
     *
     * @param shading the shading to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDShading shading)
    {
        return add(COSName.SHADING, "sh", shading);
    }

    /**
     * Adds the given pattern to the resources of the current page and returns the name for the new resources. Returns
     * the existing resource name if the given item already exists.
     *
     * @param pattern the pattern to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDAbstractPattern pattern)
    {
        return add(COSName.PATTERN, "p", pattern);
    }

    /**
     * Adds the given property list to the resources of the current page and returns the name for the new resources.
     * Returns the existing resource name if the given item already exists.
     *
     * @param properties the property list to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDPropertyList properties)
    {
        if (properties instanceof PDOptionalContentGroup)
        {
            return add(COSName.PROPERTIES, "oc", properties);
        }
        return add(COSName.PROPERTIES, "Prop", properties);
    }

    /**
     * Adds the given image to the resources of the current page and returns the name for the new resources. Returns the
     * existing resource name if the given item already exists.
     *
     * @param image the image to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDImageXObject image)
    {
        return add(COSName.XOBJECT, "Im", image);
    }

    /**
     * Adds the given form to the resources of the current page and returns the name for the new resources. Returns the
     * existing resource name if the given item already exists.
     *
     * @param form the form to add
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDFormXObject form)
    {
        return add(COSName.XOBJECT, "Form", form);
    }

    /**
     * Adds the given XObject to the resources of the current page and returns the name for the new resources. Returns
     * the existing resource name if the given item already exists.
     *
     * @param xobject the XObject to add
     * @param prefix the prefix to be used when creating the resource name
     * @return the name of the resource in the resources dictionary
     */
    public COSName add(PDXObject xobject, String prefix)
    {
        return add(COSName.XOBJECT, prefix, xobject);
    }

    /**
     * Adds the given resource if it does not already exist.
     */
    private COSName add(COSName kind, String prefix, COSObjectable object)
    {
        // return the existing key if the item exists already
        COSDictionary dict = resources.getDictionaryObject(kind, COSDictionary.class);
        return Optional.ofNullable(dict).map(d -> d.getKeyForValue(object.getCOSObject()))
                .orElseGet(() -> {
                    COSName name = createKey(kind, prefix);
                    put(kind, name, object);
                    return name;
                });
    }

    /**
     * Returns a unique key for a new resource.
     */
    private COSName createKey(COSName kind, String prefix)
    {
        COSDictionary dict = resources.getDictionaryObject(kind, COSDictionary.class);
        if (dict == null)
        {
            return COSName.getPDFName(prefix + 1);
        }

        // find a unique key
        String key;
        int n = dict.keySet().size();
        do
        {
            ++n;
            key = prefix + n;
        } while (dict.containsKey(key));
        return COSName.getPDFName(key);
    }

    /**
     * Sets the value of a given named resource.
     */
    private void put(COSName kind, COSName name, COSObjectable object)
    {
        COSDictionary dict = resources.getDictionaryObject(kind, COSDictionary.class);
        if (dict == null)
        {
            dict = new COSDictionary();
            resources.setItem(kind, dict);
        }
        dict.setItem(name, object);
    }

    /**
     * Sets the font resource with the given name.
     *
     * @param name the name of the resource
     * @param font the font to be added
     */
    public void put(COSName name, PDFont font)
    {
        put(COSName.FONT, name, font);
    }

    /**
     * Sets the color space resource with the given name.
     *
     * @param name the name of the resource
     * @param colorSpace the color space to be added
     */
    public void put(COSName name, PDColorSpace colorSpace)
    {
        put(COSName.COLORSPACE, name, colorSpace);
    }

    /**
     * Sets the extended graphics state resource with the given name.
     *
     * @param name the name of the resource
     * @param extGState the extended graphics state to be added
     */
    public void put(COSName name, PDExtendedGraphicsState extGState)
    {
        put(COSName.EXT_G_STATE, name, extGState);
    }

    /**
     * Sets the shading resource with the given name.
     *
     * @param name the name of the resource
     * @param shading the shading to be added
     */
    public void put(COSName name, PDShading shading)
    {
        put(COSName.SHADING, name, shading);
    }

    /**
     * Sets the pattern resource with the given name.
     *
     * @param name the name of the resource
     * @param pattern the pattern to be added
     */
    public void put(COSName name, PDAbstractPattern pattern)
    {
        put(COSName.PATTERN, name, pattern);
    }

    /**
     * Sets the property list resource with the given name.
     *
     * @param name the name of the resource
     * @param properties the property list to be added
     */
    public void put(COSName name, PDPropertyList properties)
    {
        put(COSName.PROPERTIES, name, properties);
    }

    /**
     * Sets the XObject resource with the given name.
     *
     * @param name the name of the resource
     * @param xobject the XObject to be added
     */
    public void put(COSName name, PDXObject xobject)
    {
        put(COSName.XOBJECT, name, xobject);
    }

    /**
     * Returns the resource cache associated with the Resources, or null if there is none.
     */
    public ResourceCache getResourceCache()
    {
        return cache;
    }

}
