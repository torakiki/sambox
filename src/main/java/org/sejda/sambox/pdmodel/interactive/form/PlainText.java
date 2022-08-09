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

import org.sejda.sambox.pdmodel.font.PDFont;

import java.io.IOException;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * A block of text.
 * <p>
 * A block of text can contain multiple paragraphs which will be treated individually within the
 * block placement.
 * </p>
 */
class PlainText
{
    private static final float FONTSCALE = 1000f;

    private final ArrayList<Paragraph> paragraphs = new ArrayList<>();

    /**
     * Construct the text block from a single value.
     * <p>
     * Constructs the text block from a single value splitting into individual {@link Paragraph}
     * when a new line character is encountered.
     *
     * @param textValue the text block string.
     */
    PlainText(String textValue)
    {
        String[] parts = textValue.replace("\t", " ").split("\\r\\n|\\n|\\r|\\u2028|\\u2029");
        for (String part : parts)
        {
            // Acrobat prints a space for an empty paragraph
            if (part.length() == 0)
            {
                part = " ";
            }
            paragraphs.add(new Paragraph(part));
        }
        if (paragraphs.isEmpty())
        {
            paragraphs.add(new Paragraph(""));
        }
    }

    /**
     * Construct the text block from a list of values.
     * <p>
     * Constructs the text block from a list of values treating each entry as an individual {@link
     * Paragraph}.
     *
     * @param listValue the text block string.
     */
    PlainText(List<String> listValue)
    {
        for (String part : listValue)
        {
            paragraphs.add(new Paragraph(part));
        }
    }

    /**
     * Get the list of paragraphs.
     *
     * @return the paragraphs.
     */
    List<Paragraph> getParagraphs()
    {
        return paragraphs;
    }

    /**
     * Attribute keys and attribute values used for text handling.
     * <p>
     * This is similar to {@link java.awt.font.TextAttribute} but handled individually as to avoid a
     * dependency on awt.
     */
    static class TextAttribute extends Attribute
    {
        /**
         * UID for serializing.
         */
        private static final long serialVersionUID = -3138885145941283005L;

        /**
         * Attribute width of the text.
         */
        public static final Attribute WIDTH = new TextAttribute("width");

        protected TextAttribute(String name)
        {
            super(name);
        }

    }

    /**
     * A block of text to be formatted as a whole.
     * <p>
     * A block of text can contain multiple paragraphs which will be treated individually within the
     * block placement.
     * </p>
     */
    static class Paragraph
    {
        private String textContent;

        Paragraph(String text)
        {
            textContent = text;
        }

        /**
         * Get the paragraph text.
         *
         * @return the text.
         */
        String getText()
        {
            return textContent;
        }

        /**
         * Break the paragraph into individual lines.
         *
         * @param font     the font used for rendering the text.
         * @param fontSize the fontSize used for rendering the text.
         * @param width    the width of the box holding the content.
         * @return the individual lines.
         * @throws IOException
         */
        List<Line> getLines(PDFont font, float fontSize, float width) throws IOException
        {
            BreakIterator iterator = BreakIterator.getLineInstance();
            iterator.setText(textContent);

            final float scale = fontSize / FONTSCALE;

            int start = iterator.first();
            int end = iterator.next();
            float lineWidth = 0;
            float wordWidth = 0f;
            float whitespaceWidth = 0f;

            List<Line> textLines = new ArrayList<>();
            Line textLine = new Line();

            while (end != BreakIterator.DONE)
            {
                whitespaceWidth = 0f;
                String word = textContent.substring(start, end);
                wordWidth = font.getStringWidth(word) * scale;

                boolean wordNeedsSplit = false;
                int splitOffset = end - start;

                lineWidth = lineWidth + wordWidth;

                // check if the last word would fit without the whitespace ending it
                if (lineWidth >= width && Character.isWhitespace(word.charAt(word.length() - 1)))
                {
                    whitespaceWidth =
                            font.getStringWidth(word.substring(word.length() - 1)) * scale;
                    lineWidth = lineWidth - whitespaceWidth;
                }

                if (lineWidth >= width && !textLine.getWords().isEmpty())
                {
                    textLine.setWidth(textLine.calculateWidth(font, fontSize));
                    textLines.add(textLine);
                    textLine = new Line();
                    lineWidth = font.getStringWidth(word) * scale;
                }

                if (wordWidth > width && textLine.getWords().isEmpty() && word.length() > 1 && width > 10)
                {
                    // single word does not fit into width
                    wordNeedsSplit = true;
                    while (true)
                    {
                        splitOffset--;
                        String substring = word.substring(0, splitOffset);
                        float substringWidth = font.getStringWidth(substring) * scale;
                        if (substringWidth < width)
                        {
                            word = substring;
                            wordWidth = font.getStringWidth(word) * scale;
                            lineWidth = wordWidth;
                            break;
                        }
                    }
                }

                AttributedString as = new AttributedString(word);
                as.addAttribute(TextAttribute.WIDTH, wordWidth);
                Word wordInstance = new Word(word);
                wordInstance.setAttributes(as);
                textLine.addWord(wordInstance);

                if (wordNeedsSplit)
                {
                    start = start + splitOffset;
                }
                else
                {
                    start = end;
                    end = iterator.next();
                }
            }
            textLine.setWidth(textLine.calculateWidth(font, fontSize));
            textLines.add(textLine);
            return textLines;
        }
    }

    /**
     * An individual line of text.
     */
    static class Line
    {
        private List<Word> words = new ArrayList<>();
        private float lineWidth;

        float getWidth()
        {
            return lineWidth;
        }

        void setWidth(float width)
        {
            lineWidth = width;
        }

        float calculateWidth(PDFont font, float fontSize) throws IOException
        {
            final float scale = fontSize / FONTSCALE;
            float calculatedWidth = 0f;
            int indexOfWord = 0;
            for (Word word : words)
            {
                calculatedWidth = calculatedWidth + (Float) word.getAttributes().getIterator()
                        .getAttribute(TextAttribute.WIDTH);
                String text = word.getText();
                if (indexOfWord == words.size() - 1 && Character.isWhitespace(
                        text.charAt(text.length() - 1)))
                {
                    float whitespaceWidth =
                            font.getStringWidth(text.substring(text.length() - 1)) * scale;
                    calculatedWidth = calculatedWidth - whitespaceWidth;
                }
                ++indexOfWord;
            }
            return calculatedWidth;
        }

        List<Word> getWords()
        {
            return words;
        }

        float getInterWordSpacing(float width)
        {
            return (width - lineWidth) / (words.size() - 1);
        }

        void addWord(Word word)
        {
            words.add(word);
        }
    }

    /**
     * An individual word.
     * <p>
     * A word is defined as a string which must be kept on the same line.
     */
    static class Word
    {
        private AttributedString attributedString;
        private String textContent;

        Word(String text)
        {
            textContent = text;
        }

        String getText()
        {
            return textContent;
        }

        AttributedString getAttributes()
        {
            return attributedString;
        }

        void setAttributes(AttributedString as)
        {
            this.attributedString = as;
        }
    }
}
