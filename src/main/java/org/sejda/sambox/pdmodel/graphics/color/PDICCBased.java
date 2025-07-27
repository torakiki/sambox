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
package org.sejda.sambox.pdmodel.graphics.color;

import static org.sejda.commons.util.RequireUtils.requireIOCondition;

import java.awt.Transparency;
import java.awt.color.CMMException;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ProfileDataException;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRange;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ICCBased colour spaces are based on a cross-platform colour profile as defined by the
 * International Color Consortium (ICC).
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public final class PDICCBased extends PDCIEBasedColorSpace
{
    private static final Logger LOG = LoggerFactory.getLogger(PDICCBased.class);

    private final PDStream stream;
    private int numberOfComponents = -1;
    private ICC_Profile iccProfile;
    private PDColorSpace alternateColorSpace;
    private ICC_ColorSpace awtColorSpace;
    private PDColor initialColor;
    private boolean isRGB = false;
    // allows to force using alternate color space instead of ICC color space for performance
    // reasons with LittleCMS (LCMS), see PDFBOX-4309
    // WARNING: do not activate this in a conforming reader
    private boolean useOnlyAlternateColorSpace = Boolean.getBoolean(
            "org.sejda.sambox.rendering.UseAlternateInsteadOfICCColorSpace");

    private static final boolean IS_KCMS;

    static
    {
        String cmmProperty = System.getProperty("sun.java2d.cmm");
        boolean result = false;
        if (!isMinJdk8())
        {
            // always KCMS but class has different name
            result = true;
        }
        else if ("sun.java2d.cmm.kcms.KcmsServiceProvider".equals(cmmProperty))
        {
            try
            {
                Class.forName("sun.java2d.cmm.kcms.KcmsServiceProvider");
                result = true;
            }
            catch (ClassNotFoundException e)
            {
                // KCMS not available
            }
        }
        // else maybe KCMS was available, but not wished
        IS_KCMS = result;
    }

    /**
     * Creates a new ICC color space with an empty stream.
     */
    public PDICCBased()
    {
        array = new COSArray();
        array.add(COSName.ICCBASED);
        stream = new PDStream();
        array.add(stream);
    }

    /**
     * Creates a new ICC color space using the PDF array.
     *
     * @param iccArray the ICC stream object
     * @throws IOException if there is an error reading the ICC profile or if the parameter is
     *                     invalid
     * @deprecated This will be private in 3.0. Please use {@link PDICCBased#create(org.sejda.sambox.cos.COSArray,
     * org.sejda.sambox.pdmodel.PDResources)} instead, which supports caching.
     */
    @Deprecated
    public PDICCBased(COSArray iccArray) throws IOException
    {
        requireIOCondition(iccArray.size() >= 2,
                "ICCBased colorspace array must have two elements");
        requireIOCondition(iccArray.getObject(1) instanceof COSStream,
                "ICCBased colorspace array must have a stream as second element");
        array = iccArray;
        stream = new PDStream((COSStream) iccArray.getObject(1));
        loadICCProfile();
    }

    /**
     * Creates a new ICC color space using the PDF array, optionally using a resource cache.
     *
     * @param iccArray  the ICC stream object.
     * @param resources resources to use as cache, or null for no caching.
     * @return an ICC color space.
     * @throws IOException if there is an error reading the ICC profile or if the parameter is
     *                     invalid.
     */
    public static PDICCBased create(COSArray iccArray, PDResources resources) throws IOException
    {
        requireIOCondition(iccArray.size() >= 2,
                "ICCBased colorspace array must have two elements");
        requireIOCondition(iccArray.getObject(1) instanceof COSStream,
                "ICCBased colorspace array must have a stream as second element");

        COSBase base = iccArray.get(1);
        boolean canCache =
                base.hasId() && resources != null && resources.getResourceCache() != null;

        if (canCache)
        {
            PDColorSpace space = resources.getResourceCache()
                    .getColorSpace(base.id().objectIdentifier);
            if (space instanceof PDICCBased)
            {
                return (PDICCBased) space;
            }
        }
        PDICCBased space = new PDICCBased(iccArray);
        if (canCache)
        {
            resources.getResourceCache().put(base.id().objectIdentifier, space);
        }
        return space;
    }

    @Override
    public String getName()
    {
        return COSName.ICCBASED.getName();
    }

    /**
     * Get the underlying ICC profile stream.
     *
     * @return the underlying ICC profile stream
     */
    public PDStream getPDStream()
    {
        return stream;
    }

    /**
     * Load the ICC profile, or init alternateColorSpace color space.
     */
    private void loadICCProfile() throws IOException
    {
        if (useOnlyAlternateColorSpace)
        {
            try
            {
                fallbackToAlternateColorSpace(null);
                return;
            }
            catch (IOException e)
            {
                LOG.warn("Error initializing alternate color space: " + e.getLocalizedMessage());
            }
        }
        InputStream input = null;
        try
        {
            input = this.stream.createInputStream();

            // if the embedded profile is sRGB then we can use Java's built-in profile, which
            // results in a large performance gain as it's our native color space, see PDFBOX-2587
            ICC_Profile profile;
            synchronized (LOG)
            {
                profile = ICC_Profile.getInstance(input);
                if (is_sRGB(profile))
                {
                    isRGB = true;
                    awtColorSpace = (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
                    iccProfile = awtColorSpace.getProfile();
                }
                else
                {
                    profile = ensureDisplayProfile(profile);
                    awtColorSpace = new ICC_ColorSpace(profile);
                    iccProfile = profile;
                }

                // set initial colour
                int numOfComponents = getNumberOfComponents();
                float[] initial = new float[numOfComponents];
                for (int c = 0; c < initial.length; c++)
                {
                    initial[c] = Math.max(0, getRangeForComponent(c).getMin());
                }
                initialColor = new PDColor(initial, this);

                // do things that trigger a ProfileDataException
                // or CMMException due to invalid profiles, see PDFBOX-1295 and PDFBOX-1740 (ü-file)
                // or ArrayIndexOutOfBoundsException, see PDFBOX-3610
                // also triggers a ProfileDataException for PDFBOX-3549 with KCMS
                // also triggers "CMMException: LCMS error 13" for PDFBOX-5563 with LCMS, but
                // calling "new ComponentColorModel" doesn't
                awtColorSpace.toRGB(new float[numOfComponents]);
                if(!IS_KCMS)
                {
                    // PDFBOX-4015: this one triggers "CMMException: LCMS error 13" with LCMS
                    new ComponentColorModel(awtColorSpace, false, false, Transparency.OPAQUE,
                            DataBuffer.TYPE_BYTE);
                }
            }
        }
        catch (ProfileDataException | IOException | ArrayIndexOutOfBoundsException | IllegalArgumentException | CMMException e)
        {
            fallbackToAlternateColorSpace(e);
        }
        finally
        {
            IOUtils.closeQuietly(input);
        }
    }

    private void fallbackToAlternateColorSpace(Exception e) throws IOException
    {
        awtColorSpace = null;
        alternateColorSpace = getAlternateColorSpace();
        if (alternateColorSpace.equals(PDDeviceRGB.INSTANCE))
        {
            isRGB = true;
        }
        if (e != null)
        {
            LOG.warn("Can't read embedded ICC profile (" + e.getLocalizedMessage()
                    + "), using alternate color space: " + alternateColorSpace.getName());
        }
        initialColor = alternateColorSpace.getInitialColor();
    }

    /**
     * Returns true if the given profile is represents sRGB.
     */
    private boolean is_sRGB(ICC_Profile profile)
    {
        byte[] bytes = Arrays.copyOfRange(profile.getData(ICC_Profile.icSigHead),
                ICC_Profile.icHdrModel, ICC_Profile.icHdrModel + 7);
        String deviceModel = new String(bytes, StandardCharsets.US_ASCII).trim();
        return deviceModel.equals("sRGB");
    }

    // PDFBOX-4114: fix profile that has the wrong display class,
    // as done by Harald Kuhr in twelvemonkeys JPEGImageReader.ensureDisplayProfile()
    private static ICC_Profile ensureDisplayProfile(ICC_Profile profile)
    {
        if (profile.getProfileClass() != ICC_Profile.CLASS_DISPLAY)
        {
            byte[] profileData = profile.getData(); // Need to clone entire profile, due to a OpenJDK bug

            if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual)
            {
                LOG.debug("ICC profile is Perceptual, ignoring, treating as Display class");
                intToBigEndian(ICC_Profile.icSigDisplayClass, profileData,
                        ICC_Profile.icHdrDeviceClass);
                return ICC_Profile.getInstance(profileData);
            }
        }
        return profile;
    }

    private static void intToBigEndian(int value, byte[] array, int index)
    {
        array[index] = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >> 8);
        array[index + 3] = (byte) (value);
    }

    @Override
    public float[] toRGB(float[] value) throws IOException
    {
        if (isRGB)
        {
            return value;
        }
        if (awtColorSpace != null)
        {
            // PDFBOX-2142: clamp bad values
            // WARNING: toRGB is very slow when used with LUT-based ICC profiles
            return awtColorSpace.toRGB(clampColors(awtColorSpace, value));
        }
        return alternateColorSpace.toRGB(value);
    }

    private float[] clampColors(ICC_ColorSpace cs, float[] value)
    {
        float[] result = new float[value.length];
        for (int i = 0; i < value.length; ++i)
        {
            float minValue = cs.getMinValue(i);
            float maxValue = cs.getMaxValue(i);
            result[i] =
                    value[i] < minValue ? minValue : (value[i] > maxValue ? maxValue : value[i]);
        }
        return result;
    }

    @Override
    public BufferedImage toRGBImage(WritableRaster raster) throws IOException
    {
        if (awtColorSpace != null)
        {
            try
            {
                return toRGBImageAWT(raster, awtColorSpace);
            }
            catch (CMMException e)
            {
                LOG.error(
                        "Failure using embedded ICC profile ({}), using alternate color space: {}",
                        e.getMessage(), alternateColorSpace);
            }
        }
        return alternateColorSpace.toRGBImage(raster);
    }

    @Override
    public BufferedImage toRawImage(WritableRaster raster) throws IOException
    {
        if (awtColorSpace == null)
        {
            return alternateColorSpace.toRawImage(raster);
        }
        return toRawImage(raster, awtColorSpace);
    }

    @Override
    public int getNumberOfComponents()
    {
        if (numberOfComponents < 0)
        {
            numberOfComponents = stream.getCOSObject().getInt(COSName.N);
            // PDFBOX-4801 correct wrong /N values
            if (iccProfile != null)
            {
                int numIccComponents = iccProfile.getNumComponents();
                if (numIccComponents != numberOfComponents)
                {
                    LOG.warn(
                            "Using {} components from ICC profile info instead of {} components from /N entry",
                            numIccComponents, numberOfComponents);
                    numberOfComponents = numIccComponents;
                }
            }
        }
        return numberOfComponents;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent)
    {
        if (awtColorSpace != null)
        {
            int n = getNumberOfComponents();
            float[] decode = new float[n * 2];
            for (int i = 0; i < n; i++)
            {
                decode[i * 2] = awtColorSpace.getMinValue(i);
                decode[i * 2 + 1] = awtColorSpace.getMaxValue(i);
            }
            return decode;
        }
        return alternateColorSpace.getDefaultDecode(bitsPerComponent);
    }

    @Override
    public PDColor getInitialColor()
    {
        return initialColor;
    }

    /**
     * Returns a list of alternate color spaces for non-conforming readers. WARNING: Do not use the
     * information in a conforming reader.
     *
     * @return A list of alternateColorSpace color spaces.
     * @throws IOException If there is an error getting the alternateColorSpace color spaces.
     */
    public PDColorSpace getAlternateColorSpace() throws IOException
    {
        COSBase alternate = stream.getCOSObject().getDictionaryObject(COSName.ALTERNATE);
        COSArray alternateArray;
        if (alternate == null)
        {
            alternateArray = new COSArray();
            int numComponents = getNumberOfComponents();
            COSName csName;
            switch (numComponents)
            {
            case 1:
                csName = COSName.DEVICEGRAY;
                break;
            case 3:
                csName = COSName.DEVICERGB;
                break;
            case 4:
                csName = COSName.DEVICECMYK;
                break;
            default:
                throw new IOException("Unknown color space number of components:" + numComponents);
            }
            alternateArray.add(csName);
        }
        else
        {
            if (alternate instanceof COSArray)
            {
                alternateArray = (COSArray) alternate;
            }
            else if (alternate instanceof COSName)
            {
                alternateArray = new COSArray();
                alternateArray.add(alternate);
            }
            else
            {
                throw new IOException(
                        "Error: expected COSArray or COSName and not " + alternate.getClass()
                                .getName());
            }
        }
        return PDColorSpace.create(alternateArray);
    }

    /**
     * Returns the range for a certain component number. This will never return null. If it is not
     * present then the range 0..1 will be returned.
     *
     * @param n the component number to get the range for
     * @return the range for this component
     */
    public PDRange getRangeForComponent(int n)
    {
        COSArray rangeArray = (COSArray) stream.getCOSObject().getDictionaryObject(COSName.RANGE);
        if (rangeArray == null || rangeArray.size() < getNumberOfComponents() * 2)
        {
            return new PDRange(); // 0..1
        }
        return new PDRange(rangeArray, n);
    }

    /**
     * Returns the metadata stream for this object, or null if there is no metadata stream.
     *
     * @return the metadata stream, or null if there is none
     */
    public COSStream getMetadata()
    {
        return stream.getCOSObject().getDictionaryObject(COSName.METADATA, COSStream.class);
    }

    /**
     * Returns the type of the color space in the ICC profile. Will be one of {@code TYPE_GRAY},
     * {@code TYPE_RGB}, or {@code TYPE_CMYK}.
     *
     * @return an ICC color space type
     */
    public int getColorSpaceType()
    {
        if (iccProfile != null)
        {
            return iccProfile.getColorSpaceType();
        }

        // if the ICC Profile could not be read
        switch (alternateColorSpace.getNumberOfComponents())
        {
        case 1:
            return ColorSpace.TYPE_GRAY;
        case 3:
            return ColorSpace.TYPE_RGB;
        case 4:
            return ColorSpace.TYPE_CMYK;
        default:
            // should not happen as all ICC color spaces in PDF must have 1,3, or 4 components
            return -1;
        }
    }

    /**
     * Sets the number of color components.
     *
     * @param n the number of color components
     */
    // TODO it's probably not safe to use this
    @Deprecated
    public void setNumberOfComponents(int n)
    {
        numberOfComponents = n;
        stream.getCOSObject().setInt(COSName.N, n);
    }

    /**
     * Sets the list of alternateColorSpace color spaces.
     *
     * @param list the list of color space objects
     */
    public void setAlternateColorSpaces(List<PDColorSpace> list)
    {
        COSArray altArray = null;
        if (list != null)
        {
            altArray = COSArrayList.converterToCOSArray(list);
        }
        stream.getCOSObject().setItem(COSName.ALTERNATE, altArray);
    }

    /**
     * Sets the range for this color space.
     *
     * @param range the new range for the a component
     * @param n     the component to set the range for
     */
    public void setRangeForComponent(PDRange range, int n)
    {
        COSArray rangeArray = (COSArray) stream.getCOSObject().getDictionaryObject(COSName.RANGE);
        if (rangeArray == null)
        {
            rangeArray = new COSArray();
            stream.getCOSObject().setItem(COSName.RANGE, rangeArray);
        }
        // extend range array with default values if needed
        while (rangeArray.size() < (n + 1) * 2)
        {
            rangeArray.add(new COSFloat(0));
            rangeArray.add(new COSFloat(1));
        }
        rangeArray.set(n * 2, new COSFloat(range.getMin()));
        rangeArray.set(n * 2 + 1, new COSFloat(range.getMax()));
    }

    /**
     * Sets the metadata stream that is associated with this color space.
     *
     * @param metadata the new metadata stream
     */
    public void setMetadata(COSStream metadata)
    {
        stream.getCOSObject().setItem(COSName.METADATA, metadata);
    }

    /**
     * Internal accessor to support indexed raw images.
     *
     * @return true if this colorspace is sRGB.
     */
    boolean isSRGB()
    {
        return isRGB;
    }

    @Override
    public String toString()
    {
        return getName() + "{numberOfComponents: " + getNumberOfComponents() + "}";
    }

    private static boolean isMinJdk8()
    {
        // strategy from lucene-solr/lucene/core/src/java/org/apache/lucene/util/Constants.java
        String version = System.getProperty("java.specification.version");
        final StringTokenizer st = new StringTokenizer(version, ".");
        try
        {
            int major = Integer.parseInt(st.nextToken());
            int minor = 0;
            if (st.hasMoreTokens())
            {
                minor = Integer.parseInt(st.nextToken());
            }
            return major > 1 || (major == 1 && minor >= 8);
        }
        catch (NumberFormatException nfe)
        {
            // maybe some new numbering scheme in the 22nd century
            return true;
        }
    }
}
