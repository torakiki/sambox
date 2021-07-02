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

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.io.CountingWritableByteChannel.from;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.input.ContentStreamParser;
import org.sejda.sambox.output.ContentStreamWriter;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.sejda.sambox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.sejda.sambox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create the AcroForms field appearance helper.
 * 
 * @author Stephan Gerhard
 * @author Ben Litchfield
 */
public class AppearanceGeneratorHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(AppearanceGeneratorHelper.class);

    private static final Operator BMC = Operator.getOperator("BMC");
    private static final Operator EMC = Operator.getOperator("EMC");

    private final PDVariableText field;

    private PDDefaultAppearanceString defaultAppearance;
    private String value;

    /**
     * The highlight color
     *
     * The color setting is used by Adobe to display the highlight box for selected entries in a list box.
     *
     * Regardless of other settings in an existing appearance stream Adobe will always use this value.
     */
    private static final float[] HIGHLIGHT_COLOR = { 153 / 255f, 193 / 255f, 215 / 255f };

    /**
     * The scaling factor for font units to PDF units
     */
    private static final int FONTSCALE = 1000;

    /**
     * The default font size used for multiline text
     */
    private static final float DEFAULT_FONT_SIZE = 12;

    /**
     * The default padding applied by Acrobat to the fields bbox.
     */
    private static final float DEFAULT_PADDING = 0.5f;

    /**
     * Constructs a COSAppearance from the given field.
     *
     * @param field the field which you wish to control the appearance of
     * @throws IOException
     */
    public AppearanceGeneratorHelper(PDVariableText field) throws IOException
    {
        this.field = field;
        validateAndEnsureAcroFormResources();
        try
        {
            this.defaultAppearance = field.getDefaultAppearanceString();
        }
        catch (IOException ex)
        {
            throw new IOException(
                    "Could not process default appearance string '" + field.getDefaultAppearance()
                            + "' for field '" + field.getFullyQualifiedName() + "'",
                    ex);
        }
    }

    /*
     * Adobe Reader/Acrobat are adding resources which are at the field/widget level to the AcroForm level.
     */
    private void validateAndEnsureAcroFormResources()
    {
        // add font resources which might be available at the field
        // level but are not at the AcroForm level to the AcroForm
        // to match Adobe Reader/Acrobat behavior
        if (field.getAcroForm().getDefaultResources() == null)
        {
            return;
        }

        PDResources acroFormResources = field.getAcroForm().getDefaultResources();

        for (PDAnnotationWidget widget : field.getWidgets())
        {
            if (widget.getNormalAppearanceStream() != null
                    && widget.getNormalAppearanceStream().getResources() != null)
            {
                PDResources widgetResources = widget.getNormalAppearanceStream().getResources();
                Map<COSName, PDFont> missingFonts = new HashMap<>();
                for (COSName fontResourceName : widgetResources.getFontNames())
                {
                    try
                    {
                        if (acroFormResources.getFont(fontResourceName) == null)
                        {
                            LOG.debug("Adding font resource " + fontResourceName
                                    + " from widget to AcroForm");
                            missingFonts.put(fontResourceName,
                                    widgetResources.getFont(fontResourceName));
                        }
                    }
                    catch (IOException e)
                    {
                        LOG.warn("Unable to match field level font with AcroForm font");
                    }
                }

                // add all missing font resources from widget to AcroForm
                for (COSName key : missingFonts.keySet())
                {
                    acroFormResources.put(key, missingFonts.get(key));
                }
            }
        }
    }

    /**
     * This is the public method for setting the appearance stream.
     *
     * @param apValue the String value which the appearance should represent
     * @throws IOException If there is an error creating the stream.
     */
    public void setAppearanceValue(String apValue) throws IOException
    {
        value = apValue;
        
        if(field instanceof PDTextField)
        {
            // avoid java.lang.IllegalArgumentException: U+00A0 ('nbspace') is not available in this font XYZ
            // TODO: generic way of handling any character which is not supported by the font?
            value = value.replaceAll("\\u00A0", " ");
        }

        // Treat multiline field values in single lines as single lime values.
        // This is in line with how Adobe Reader behaves when enetring text
        // interactively but NOT how it behaves when the field value has been
        // set programmatically and Reader is forced to generate the appearance
        // using PDAcroForm.setNeedAppearances
        // see PDFBOX-3911
        if (field instanceof PDTextField && !((PDTextField) field).isMultiline())
        {
            value = value.replaceAll(
                    "\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]", " ");
        }

        for (PDAnnotationWidget widget : field.getWidgets())
        {
            // some fields have the /Da at the widget level if the
            // widgets differ in layout.
            PDDefaultAppearanceString acroFormAppearance = defaultAppearance;

            if (widget.getCOSObject().getDictionaryObject(COSName.DA) != null)
            {
                defaultAppearance = getWidgetDefaultAppearanceString(widget);
            }

            PDRectangle rect = widget.getRectangle();
            if (rect == null)
            {
                widget.getCOSObject().removeItem(COSName.AP);
                LOG.warn("widget of field {} has no rectangle, no appearance stream created",
                        field.getFullyQualifiedName());
                continue;
            }

            PDFormFieldAdditionalActions actions = field.getActions();

            // in case all tests fail the field will be formatted by acrobat
            // when it is opened. See FreedomExpressions.pdf for an example of this.
            if (actions == null || actions.getF() == null
                    || widget.getCOSObject().getDictionaryObject(COSName.AP) != null)
            {
                PDAppearanceDictionary appearanceDict = widget.getAppearance();
                if (appearanceDict == null)
                {
                    appearanceDict = new PDAppearanceDictionary();
                    widget.setAppearance(appearanceDict);
                }

                PDAppearanceEntry appearance = appearanceDict.getNormalAppearance();
                // TODO support appearances other than "normal"

                PDAppearanceStream appearanceStream;
                if (isValidAppearanceStream(appearance))
                {
                    appearanceStream = appearance.getAppearanceStream();
                }
                else
                {
                    appearanceStream = prepareNormalAppearanceStream(widget);
                    appearanceDict.setNormalAppearance(appearanceStream);
                    // TODO support appearances other than "normal"
                }
                /*
                 * Adobe Acrobat always recreates the complete appearance stream if there is an appearance
                 * characteristics entry (the widget dictionaries MK entry). In addition if there is no content yet also
                 * create the apperance stream from the entries.
                 */
                if (widget.getAppearanceCharacteristics() != null
                        || appearanceStream.getContentStream().getLength() == 0)
                {
                    initializeAppearanceContent(widget, appearanceStream);
                }
                setAppearanceContent(widget, appearanceStream);
            }
            // restore the field level appearance;
            defaultAppearance = acroFormAppearance;
        }
    }

    private static boolean isValidAppearanceStream(PDAppearanceEntry appearance)
    {
        if (isNull(appearance) || !appearance.isStream())
        {
            return false;
        }
        PDRectangle bbox = appearance.getAppearanceStream().getBBox();
        if (bbox == null)
        {
            return false;
        }
        return Math.abs(bbox.getWidth()) > 0 && Math.abs(bbox.getHeight()) > 0;
    }

    private PDAppearanceStream prepareNormalAppearanceStream(PDAnnotationWidget widget)
    {
        PDAppearanceStream appearanceStream = new PDAppearanceStream();

        // Calculate the entries for the bounding box and the transformation matrix
        // settings for the appearance stream
        int rotation = resolveRotation(widget);
        PDRectangle rect = widget.getRectangle();
        Matrix matrix = Matrix.getRotateInstance(Math.toRadians(rotation), 0, 0);
        Point2D.Float point2D = matrix.transformPoint(rect.getWidth(), rect.getHeight());

        PDRectangle bbox = new PDRectangle(Math.abs((float) point2D.getX()),
                Math.abs((float) point2D.getY()));
        appearanceStream.setBBox(bbox);

        AffineTransform at = calculateMatrix(bbox, rotation);
        if (!at.isIdentity())
        {
            appearanceStream.setMatrix(at);
        }
        appearanceStream.setFormType(1);
        appearanceStream.setResources(new PDResources());
        return appearanceStream;
    }

    private PDDefaultAppearanceString getWidgetDefaultAppearanceString(PDAnnotationWidget widget)
            throws IOException
    {
        COSString da = DefaultAppearanceHelper.getDefaultAppearance(widget.getCOSObject().getDictionaryObject(COSName.DA));
        PDResources dr = field.getAcroForm().getDefaultResources();
        try
        {
            return new PDDefaultAppearanceString(da, dr);
        }
        catch (IOException ex)
        {
            LOG.warn(
                    "Failed to process default appearance string for widget {}, will use fallback default appearance",
                    widget);
            return new PDDefaultAppearanceString();
        }
    }

    private int resolveRotation(PDAnnotationWidget widget)
    {
        PDAppearanceCharacteristicsDictionary characteristicsDictionary = widget
                .getAppearanceCharacteristics();
        if (characteristicsDictionary != null)
        {
            // 0 is the default value if the R key doesn't exist
            return characteristicsDictionary.getRotation();
        }
        return 0;
    }

    /**
     * Initialize the content of the appearance stream.
     * 
     * Get settings like border style, border width and colors to be used to draw a rectangle and background color
     * around the widget
     * 
     * @param widget the field widget
     * @param appearanceStream the appearance stream to be used
     * @throws IOException in case we can't write to the appearance stream
     */
    private void initializeAppearanceContent(PDAnnotationWidget widget,
            PDAppearanceStream appearanceStream) throws IOException
    {
        try (PDPageContentStream contents = new PDPageContentStream(
                field.getAcroForm().getDocument(), appearanceStream))
        {
            PDAppearanceCharacteristicsDictionary appearanceCharacteristics = widget
                    .getAppearanceCharacteristics();

            // TODO: support more entries like patterns, etc.
            if (appearanceCharacteristics != null)
            {
                PDColor backgroundColour = appearanceCharacteristics.getBackground();
                if (backgroundColour != null)
                {
                    contents.setNonStrokingColor(backgroundColour);
                    PDRectangle bbox = resolveBoundingBox(widget, appearanceStream);
                    contents.addRect(bbox.getLowerLeftX(), bbox.getLowerLeftY(), bbox.getWidth(),
                            bbox.getHeight());
                    contents.fill();
                }

                float lineWidth = 0f;
                PDColor borderColour = appearanceCharacteristics.getBorderColour();
                if (borderColour != null)
                {
                    contents.setStrokingColor(borderColour);
                    lineWidth = 1f;
                }
                PDBorderStyleDictionary borderStyle = widget.getBorderStyle();
                if (borderStyle != null && borderStyle.getWidth() > 0)
                {
                    lineWidth = borderStyle.getWidth();
                }

                if (lineWidth > 0 && borderColour != null)
                {
                    if (lineWidth != 1)
                    {
                        contents.setLineWidth(lineWidth);
                    }
                    PDRectangle bbox = resolveBoundingBox(widget, appearanceStream);
                    PDRectangle clipRect = applyPadding(bbox,
                            Math.max(DEFAULT_PADDING, lineWidth / 2));
                    contents.addRect(clipRect.getLowerLeftX(), clipRect.getLowerLeftY(),
                            clipRect.getWidth(), clipRect.getHeight());
                    contents.closeAndStroke();
                }
            }
        }
    }

    /**
     * Parses an appearance stream into tokens.
     */
    private List<Object> tokenize(PDAppearanceStream appearanceStream) throws IOException
    {
        return new ContentStreamParser(appearanceStream).tokens();
    }

    /**
     * Constructs and sets new contents for given appearance stream.
     */
    private void setAppearanceContent(PDAnnotationWidget widget,
            PDAppearanceStream appearanceStream) throws IOException
    {
        // first copy any needed resources from the document’s DR dictionary into
        // the stream’s Resources dictionary
        defaultAppearance.copyNeededResourcesTo(appearanceStream);

        // then replace the existing contents of the appearance stream from /Tx BMC
        // to the matching EMC
        try (ContentStreamWriter writer = new ContentStreamWriter(
                from(appearanceStream.getCOSObject().createUnfilteredStream())))
        {

            List<Object> tokens = tokenize(appearanceStream);
            int bmcIndex = tokens.indexOf(BMC);
            if (bmcIndex == -1)
            {
                // append to existing stream
                writer.writeTokens(tokens);
                writer.writeTokens(asList(COSName.TX, BMC));
            }
            else
            {
                // prepend content before BMC
                writer.writeTokens(tokens.subList(0, bmcIndex + 1));
            }

            // insert field contents
            insertGeneratedAppearance(widget, appearanceStream, writer);

            int emcIndex = tokens.indexOf(EMC);
            if (emcIndex == -1)
            {
                // append EMC
                writer.writeTokens(EMC);
            }
            else
            {
                // append contents after EMC
                writer.writeTokens(tokens.subList(emcIndex, tokens.size()));
            }
        }
    }

    /**
     * Generate and insert text content and clipping around it.
     */
    private void insertGeneratedAppearance(PDAnnotationWidget widget,
            PDAppearanceStream appearanceStream, ContentStreamWriter writer) throws IOException
    {
        PDPageContentStream contents = new PDPageContentStream(field.getAcroForm().getDocument(),
                appearanceStream, writer);

        PDRectangle bbox = resolveBoundingBox(widget, appearanceStream);

        // Acrobat calculates the left and right padding dependent on the offset of the border edge
        // This calculation works for forms having been generated by Acrobat.
        // The minimum distance is always 1f even if there is no rectangle being drawn around.
        float borderWidth = 0;
        if (widget.getBorderStyle() != null)
        {
            borderWidth = widget.getBorderStyle().getWidth();
        }
        PDRectangle clipRect = applyPadding(bbox, Math.max(1f, borderWidth));
        PDRectangle contentRect = applyPadding(clipRect, Math.max(1f, borderWidth));

        contents.saveGraphicsState();

        // Acrobat always adds a clipping path
        contents.addRect(clipRect.getLowerLeftX(), clipRect.getLowerLeftY(), clipRect.getWidth(),
                clipRect.getHeight());
        contents.clip();

        // get the font
        // field's defined appearance font has priority
        // callers might have determined that the default font does not support rendering the field's value
        // so the font was substituted to another one, which has better unicode support
        // see PDVariableText.setAppearanceOverrideFont()
        PDFont font = ofNullable(field.getAppearanceFont())
                .orElseGet(() -> defaultAppearance.getFont());

        requireNotNullArg(font, "font is null, check whether /DA entry is incomplete or incorrect");
        if (font.getName().contains("+"))
        {
            LOG.warn("Font '" + defaultAppearance.getFontName().getName() + "' of field '"
                    + field.getFullyQualifiedName() + "' contains subsetted font '" + font.getName()
                    + "'");
            LOG.warn("This may bring trouble with PDField.setValue(), PDAcroForm.flatten() or "
                    + "PDAcroForm.refreshAppearances()");
            LOG.warn("You should replace this font with a non-subsetted font:");
            LOG.warn("PDFont font = PDType0Font.load(doc, new FileInputStream(fontfile), false);");
            LOG.warn("acroForm.getDefaultResources().put(COSName.getPDFName(\""
                    + defaultAppearance.getFontName().getName() + "\", font);");
        }

        float fontSize = defaultAppearance.getFontSize();

        // calculate the fontSize (because 0 = autosize)
        boolean recalculateFontSize = fontSize == 0;

        // always re-calculate the fontSize for text fields
        // because sometimes the field value changes to a longer string, which won't fit the field anymore when drawn
        // using the previous font size
        if (field instanceof PDTextField && !((PDTextField) field).isMultiline())
        {
            recalculateFontSize = true;
        }

        if (recalculateFontSize)
        {
            float newFontSize = calculateFontSize(font, contentRect);
            // avoid increasing the font size, if one was already specified
            if(newFontSize < fontSize || fontSize == 0) 
            {
                fontSize = newFontSize;
            }
        }

        // for a listbox generate the highlight rectangle for the selected
        // options
        if (field instanceof PDListBox)
        {
            insertGeneratedListboxSelectionHighlight(contents, appearanceStream, font, fontSize);
        }

        // start the text output
        contents.beginText();

        // write the /DA string
        defaultAppearance.writeTo(contents, fontSize);
        // calculate the y-position of the baseline
        float y;

        // calculate font metrics at font size
        float fontScaleY = fontSize / FONTSCALE;
        float fontBoundingBoxAtSize = font.getBoundingBox().getHeight() * fontScaleY;
        float fontCapAtSize = font.getFontDescriptor().getCapHeight() * fontScaleY;
        float fontDescentAtSize = font.getFontDescriptor().getDescent() * fontScaleY;

        if (field instanceof PDTextField && ((PDTextField) field).isMultiline())
        {
            y = contentRect.getUpperRightY() - calculateLineHeight(font, fontScaleY);
        }
        else
        {
            // Adobe shows the text 'shiftet up' in case the caps don't fit into the clipping area
            if (fontCapAtSize > clipRect.getHeight())
            {
                y = clipRect.getLowerLeftY() + -fontDescentAtSize;
            }
            else
            {
                // calculate the position based on the content rectangle
                y = clipRect.getLowerLeftY() + (clipRect.getHeight() - fontCapAtSize) / 2;

                // shift up slightly, so it does not cover any form field line underneath
                y += 1;

                // check to ensure that ascents and descents fit
                if (y - clipRect.getLowerLeftY() < -fontDescentAtSize)
                {

                    float fontDescentBased = -fontDescentAtSize + contentRect.getLowerLeftY();
                    float fontCapBased = contentRect.getHeight() - contentRect.getLowerLeftY()
                            - fontCapAtSize;

                    y = Math.min(fontDescentBased, Math.max(y, fontCapBased));
                }
            }
        }

        // show the text
        float x = contentRect.getLowerLeftX();

        // special handling for comb boxes as these are like table cells with individual
        // chars
        if (shallComb())
        {
            insertGeneratedCombAppearance(contents, bbox, font, fontSize);
        }
        else if (field instanceof PDListBox)
        {
            insertGeneratedListboxAppearance(contents, appearanceStream, contentRect, font,
                    fontSize);
        }
        else
        {
            PlainText textContent = new PlainText(value);
            AppearanceStyle appearanceStyle = new AppearanceStyle();
            appearanceStyle.setFont(font);
            appearanceStyle.setFontSize(fontSize);

            // Adobe Acrobat uses the font's bounding box for the leading between the lines
            appearanceStyle.setLeading(calculateLineHeight(font, fontScaleY));

            PlainTextFormatter formatter = new PlainTextFormatter.Builder(contents)
                    .style(appearanceStyle).text(textContent).width(contentRect.getWidth())
                    .wrapLines(isMultiLine()).initialOffset(x, y).textAlign(field.getQ()).build();
            formatter.format();
        }

        contents.endTextIfRequired();
        contents.restoreGraphicsState();
    }

    private AffineTransform calculateMatrix(PDRectangle bbox, int rotation)
    {
        if (rotation == 0)
        {
            return new AffineTransform();
        }
        float tx = 0, ty = 0;
        switch (rotation)
        {
        case 90:
            tx = bbox.getUpperRightY();
            break;
        case 180:
            tx = bbox.getUpperRightY();
            ty = bbox.getUpperRightX();
            break;
        case 270:
            ty = bbox.getUpperRightX();
            break;
        default:
            break;
        }
        Matrix matrix = Matrix.getRotateInstance(Math.toRadians(rotation), tx, ty);
        return matrix.createAffineTransform();

    }

    private boolean isMultiLine()
    {
        return field instanceof PDTextField && ((PDTextField) field).isMultiline();
    }

    /**
     * Determine if the appearance shall provide a comb output.
     * 
     * <p>
     * May be set only if the MaxLen entry is present in the text field dictionary and if the Multiline, Password, and
     * FileSelect flags are clear. If set, the field shall be automatically divided into as many equally spaced
     * positions, or combs, as the value of MaxLen, and the text is laid out into those combs.
     * </p>
     * 
     * @return the comb state
     */
    private boolean shallComb()
    {
        return field instanceof PDTextField && ((PDTextField) field).isComb()
                && !((PDTextField) field).isMultiline() && !((PDTextField) field).isPassword()
                && !((PDTextField) field).isFileSelect();
    }

    /**
     * Generate the appearance for comb fields.
     * 
     * @param contents the content stream to write to
     * @param bbox the bbox used
     * @param font the font to be used
     * @param fontSize the font size to be used
     * @throws IOException
     */
    private void insertGeneratedCombAppearance(PDPageContentStream contents, PDRectangle bbox,
            PDFont font, float fontSize) throws IOException
    {

        // TODO: Currently the quadding is not taken into account
        // so the comb is always filled from left to right.

        int maxLen = ((PDTextField) field).getMaxLen();
        int numChars = Math.min(value.length(), maxLen);

        PDRectangle paddingEdge = applyPadding(bbox, 1);

        float combWidth = bbox.getWidth() / maxLen;
        float ascentAtFontSize = font.getFontDescriptor().getAscent() / FONTSCALE * fontSize;
        float baselineOffset = paddingEdge.getLowerLeftY()
                + (bbox.getHeight() - ascentAtFontSize) / 2;

        float prevCharWidth = 0f;

        float xOffset = combWidth / 2;

        contents.saveGraphicsState();
        contents.setFont(font, fontSize);

        for (int i = 0; i < numChars; i++)
        {
            String combString = value.substring(i, i + 1);
            float currCharWidth = font.getStringWidth(combString) / FONTSCALE * fontSize / 2;

            xOffset = xOffset + prevCharWidth / 2 - currCharWidth / 2;

            contents.newLineAtOffset(xOffset, baselineOffset);
            contents.showText(combString);

            baselineOffset = 0;
            prevCharWidth = currCharWidth;
            xOffset = combWidth;
        }

        contents.restoreGraphicsState();
    }

    private void insertGeneratedListboxSelectionHighlight(PDPageContentStream contents,
            PDAppearanceStream appearanceStream, PDFont font, float fontSize) throws IOException
    {
        List<Integer> indexEntries = ((PDListBox) field).getSelectedOptionsIndex();
        List<String> values = ((PDListBox) field).getValue();
        List<String> options = ((PDListBox) field).getOptionsExportValues();

        if (!values.isEmpty() && !options.isEmpty() && indexEntries.isEmpty())
        {
            // create indexEntries from options
            indexEntries = new ArrayList<>();
            for (String v : values)
            {
                indexEntries.add(options.indexOf(v));
            }
        }

        // The first entry which shall be presented might be adjusted by the optional TI key
        // If this entry is present the first entry to be displayed is the keys value otherwise
        // display starts with the first entry in Opt.
        int topIndex = ((PDListBox) field).getTopIndex();

        float highlightBoxHeight = font.getBoundingBox().getHeight() * fontSize / FONTSCALE;

        // the padding area
        PDRectangle paddingEdge = applyPadding(appearanceStream.getBBox(), 1);

        for (int selectedIndex : indexEntries)
        {
            contents.setNonStrokingColor(HIGHLIGHT_COLOR[0], HIGHLIGHT_COLOR[1],
                    HIGHLIGHT_COLOR[2]);

            contents.addRect(paddingEdge.getLowerLeftX(),
                    paddingEdge.getUpperRightY()
                            - highlightBoxHeight * (selectedIndex - topIndex + 1) + 2,
                    paddingEdge.getWidth(), highlightBoxHeight);
            contents.fill();
        }
        contents.setNonStrokingColor(0f);
    }

    private void insertGeneratedListboxAppearance(PDPageContentStream contents,
            PDAppearanceStream appearanceStream, PDRectangle contentRect, PDFont font,
            float fontSize) throws IOException
    {
        contents.setNonStrokingColor(0f);

        int q = field.getQ();
        if (q == PDVariableText.QUADDING_CENTERED || q == PDVariableText.QUADDING_RIGHT)
        {
            float fieldWidth = appearanceStream.getBBox().getWidth();
            float stringWidth = (font.getStringWidth(value) / FONTSCALE) * fontSize;
            float adjustAmount = fieldWidth - stringWidth - 4;

            if (q == PDVariableText.QUADDING_CENTERED)
            {
                adjustAmount = adjustAmount / 2.0f;
            }

            contents.newLineAtOffset(adjustAmount, 0);
        }
        else if (q != PDVariableText.QUADDING_LEFT)
        {
            throw new IOException("Error: Unknown justification value:" + q);
        }

        List<String> options = ((PDListBox) field).getOptionsDisplayValues();
        int numOptions = options.size();

        float yTextPos = contentRect.getUpperRightY();

        int topIndex = ((PDListBox) field).getTopIndex();

        for (int i = topIndex; i < numOptions; i++)
        {

            if (i == topIndex)
            {
                yTextPos = yTextPos - font.getFontDescriptor().getAscent() / FONTSCALE * fontSize;
            }
            else
            {
                yTextPos = yTextPos - font.getBoundingBox().getHeight() / FONTSCALE * fontSize;
                contents.beginText();
            }

            contents.newLineAtOffset(contentRect.getLowerLeftX(), yTextPos);
            contents.setFont(font, fontSize);
            contents.showText(replaceTabChars(options.get(i)));

            if (i != (numOptions - 1))
            {
                contents.endText();
            }
        }
    }
    
    private String replaceTabChars(String input)
    {
        // avoid java.lang.IllegalArgumentException: U+0009 ('controlHT') is not available in this font XYZ
        return input.replace("\t", "    ");
    }

    private float calculateLineHeight(PDFont font, float fontScaleY) throws IOException
    {
        float fontBoundingBoxAtSize = font.getBoundingBox().getHeight() * fontScaleY;
        float fontCapAtSize = font.getFontDescriptor().getCapHeight() * fontScaleY;
        float fontDescentAtSize = font.getFontDescriptor().getDescent() * fontScaleY;

        float lineHeight = fontCapAtSize - fontDescentAtSize;
        if (lineHeight < 0)
        {
            lineHeight = fontBoundingBoxAtSize;
        }

        return lineHeight;
    }

    /**
     * My "not so great" method for calculating the fontsize. It does not work superb, but it handles ok.
     * 
     * @return the calculated font-size
     * @throws IOException If there is an error getting the font information.
     */
    float calculateFontSize(PDFont font, PDRectangle contentRect) throws IOException
    {
        float yScalingFactor = FONTSCALE * font.getFontMatrix().getScaleY();
        float xScalingFactor = FONTSCALE * font.getFontMatrix().getScaleX();

        if (isMultiLine())
        {
            // Acrobat defaults to 12 for multiline text with size 0
            // PDFBOX decided to just return that and finish with it
            // return DEFAULT_FONT_SIZE;

            // SAMBOX specifics below
            // We calculate a font size that fits at least 5 lines
            // We detect faux multiline fields (text fields flagged as multiline which have a small height to just fit
            // one line)

            float lineHeight = calculateLineHeight(font, font.getFontMatrix().getScaleY());
            float scaledContentHeight = contentRect.getHeight() * yScalingFactor;

            boolean looksLikeFauxMultiline = calculateLineHeight(font,
                    DEFAULT_FONT_SIZE / FONTSCALE) > scaledContentHeight;
            int userTypedLinesCount = new PlainText(value).getParagraphs().size();
            boolean userTypedMultipleLines = userTypedLinesCount > 1;

            if (looksLikeFauxMultiline && !userTypedMultipleLines)
            {
                // faux multiline detected
                // because 1 line written with the default font size would not fit the height
                // just continue to the non multiline part of the algorithm

                LOG.warn("Faux multiline field found: {}", field.getFullyQualifiedName());
            }
            else
            {
                // calculate a font size which fits at least x lines
                int minimumLinesToFitInAMultilineField = Math.max(2, userTypedLinesCount);
                float fontSize = scaledContentHeight
                        / (minimumLinesToFitInAMultilineField * lineHeight);
                // don't return a font size larger than the default
                return Math.min(fontSize, DEFAULT_FONT_SIZE);
            }
        }

        // fit width
        float width = font.getStringWidth(value) * font.getFontMatrix().getScaleX();
        float widthBasedFontSize = contentRect.getWidth() / width * xScalingFactor;

        // fit height
        float height = calculateLineHeight(font, font.getFontMatrix().getScaleY());

        float heightBasedFontSize = contentRect.getHeight() / height * yScalingFactor;

        return Math.min(heightBasedFontSize, widthBasedFontSize);
    }

    /**
     * Resolve the bounding box.
     * 
     * @param fieldWidget the annotation widget.
     * @param appearanceStream the annotations appearance stream.
     * @return the resolved boundingBox.
     */
    private PDRectangle resolveBoundingBox(PDAnnotationWidget fieldWidget,
            PDAppearanceStream appearanceStream)
    {
        PDRectangle boundingBox = appearanceStream.getBBox();
        if (boundingBox == null || hasZeroDimensions(boundingBox))
        {
            boundingBox = fieldWidget.getRectangle().createRetranslatedRectangle();
        }
        return boundingBox;
    }

    private boolean hasZeroDimensions(PDRectangle bbox)
    {
        return bbox.getHeight() == 0 || bbox.getWidth() == 0;
    }

    /**
     * Apply padding to a box.
     *
     * @param box box
     * @return the padded box.
     */
    private PDRectangle applyPadding(PDRectangle box, float padding)
    {
        return new PDRectangle(box.getLowerLeftX() + padding, box.getLowerLeftY() + padding,
                box.getWidth() - 2 * padding, box.getHeight() - 2 * padding);
    }
}
