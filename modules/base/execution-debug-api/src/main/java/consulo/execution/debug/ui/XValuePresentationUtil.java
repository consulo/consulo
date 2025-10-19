/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.debug.ui;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class XValuePresentationUtil {
    public static void renderValue(@Nonnull String value, @Nonnull ColoredTextContainer text, @Nonnull SimpleTextAttributes attributes, int maxLength,
                                   @Nullable String additionalCharsToEscape) {
        SimpleTextAttributes escapeAttributes = null;
        int lastOffset = 0;
        int length = maxLength == -1 ? value.length() : Math.min(value.length(), maxLength);
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            int additionalCharIndex = -1;
            if (ch == '\n' || ch == '\r' || ch == '\t' || ch == '\b' || ch == '\f'
                || (additionalCharsToEscape != null && (additionalCharIndex = additionalCharsToEscape.indexOf(ch)) != -1)) {
                if (i > lastOffset) {
                    text.append(value.substring(lastOffset, i), attributes);
                }
                lastOffset = i + 1;

                if (escapeAttributes == null) {
                    TextAttributes fromHighlighter = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
                    if (fromHighlighter != null) {
                        escapeAttributes = TextAttributesUtil.fromTextAttributes(fromHighlighter);
                    }
                    else {
                        escapeAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.GRAY);
                    }
                }

                if (additionalCharIndex == -1) {
                    text.append("\\", escapeAttributes);
                }

                text.append(String.valueOf(getEscapingSymbol(ch)), escapeAttributes);
            }
        }

        if (lastOffset < length) {
            text.append(value.substring(lastOffset, length), attributes);
        }
    }

    private static char getEscapingSymbol(char ch) {
        switch (ch) {
            case '\n':
                return 'n';
            case '\r':
                return 'r';
            case '\t':
                return 't';
            case '\b':
                return 'b';
            case '\f':
                return 'f';
            default:
                return ch;
        }
    }

    public static void appendSeparator(@Nonnull ColoredTextContainer text, @Nonnull String separator) {
        if (!separator.isEmpty()) {
            text.append(separator, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    @Nonnull
    public static String computeValueText(@Nonnull XValuePresentation presentation) {
        XValuePresentationTextExtractor extractor = new XValuePresentationTextExtractor();
        presentation.renderValue(extractor);
        return extractor.getText();
    }

    private static class XValuePresentationTextExtractor extends XValueTextRendererBase {
        private final StringBuilder myBuilder;

        public XValuePresentationTextExtractor() {
            myBuilder = new StringBuilder();
        }

        @Override
        public void renderValue(@Nonnull String value) {
            myBuilder.append(value);
        }

        @Override
        protected void renderRawValue(@Nonnull String value, @Nonnull TextAttributesKey key) {
            myBuilder.append(value);
        }

        @Override
        public void renderStringValue(@Nonnull String value, @Nullable String additionalSpecialCharsToHighlight, char quoteChar, int maxLength) {
            myBuilder.append(quoteChar);
            myBuilder.append(value);
            myBuilder.append(quoteChar);
        }

        @Override
        public void renderComment(@Nonnull String comment) {
            myBuilder.append(comment);
        }

        @Override
        public void renderError(@Nonnull LocalizeValue errorValue) {
            myBuilder.append(errorValue.get());
        }

        @Override
        public void renderSpecialSymbol(@Nonnull String symbol) {
            myBuilder.append(symbol);
        }

        public String getText() {
            return myBuilder.toString();
        }
    }
}
