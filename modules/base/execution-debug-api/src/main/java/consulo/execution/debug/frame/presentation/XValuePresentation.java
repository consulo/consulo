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
package consulo.execution.debug.frame.presentation;

import consulo.annotation.DeprecationInfo;
import consulo.colorScheme.TextAttributesKey;
import consulo.execution.debug.frame.XValueNode;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Determines how a value is shown in debugger trees. Use one of the standard implementations (for {@link consulo.ide.impl.idea.xdebugger.frame.presentation.XStringValuePresentation strings},
 * {@link consulo.ide.impl.idea.xdebugger.frame.presentation.XNumericValuePresentation numbers}, {@link consulo.ide.impl.idea.xdebugger.frame.presentation.XKeywordValuePresentation keywords}
 * and for {@link consulo.ide.impl.idea.xdebugger.frame.presentation.XRegularValuePresentation other values}) or override this class if you need something special
 *
 * @see XValueNode#setPresentation(consulo.ui.image.Image, XValuePresentation, boolean)
 */
public abstract class XValuePresentation {
    protected static final String DEFAULT_SEPARATOR = " = ";

    /**
     * Renders value text by delegating to {@code renderer} methods
     *
     * @param renderer {@link XValueTextRenderer} instance which provides methods to
     */
    public abstract void renderValue(@Nonnull XValueTextRenderer renderer);

    /**
     * @return separator between name and value in a debugger tree
     */
    @Nonnull
    public String getSeparator() {
        return DEFAULT_SEPARATOR;
    }

    /**
     * @return optional type of the value, it is shown in gray color and surrounded by braces
     */
    @Nullable
    public String getType() {
        return null;
    }

    public interface XValueTextRenderer {
        /**
         * Appends {@code value} with to the node text. Invisible characters are shown in escaped form.
         */
        void renderValue(@Nonnull String value);

        /**
         * Appends {@code value} surrounded by quotes to the node text colored as a string
         */
        void renderStringValue(@Nonnull String value);

        /**
         * Appends {@code value} highlighted as a number
         */
        void renderNumericValue(@Nonnull String value);

        /**
         * Appends {@code value} surrounded by single quotes to the node text colored as a string
         */
        void renderCharValue(@Nonnull String value);

        /**
         * Appends {@code value} highlighted as a keyword
         */
        void renderKeywordValue(@Nonnull String value);

        void renderValue(@Nonnull String value, @Nonnull TextAttributesKey key);

        /**
         * Appends {@code value} surrounded by quotes to the node text colored as a string
         *
         * @param value                             value to be shown
         * @param additionalSpecialCharsToHighlight characters which should be highlighted in a special color
         * @param maxLength                         maximum number of characters to show
         */
        void renderStringValue(@Nonnull String value, @Nullable String additionalSpecialCharsToHighlight, int maxLength);

        /**
         * Appends {@code value} surrounded by quotes to the node text colored as a string
         *
         * @param value                             value to be shown
         * @param additionalSpecialCharsToHighlight characters which should be highlighted in a special color
         * @param maxLength                         maximum number of characters to show
         */
        void renderStringValue(@Nonnull String value, @Nullable String additionalSpecialCharsToHighlight, char quoteChar, int maxLength);

        /**
         * Appends gray colored {@code comment}
         */
        void renderComment(@Nonnull String comment);

        /**
         * Appends {@code symbol} which is not part of the value
         */
        void renderSpecialSymbol(@Nonnull String symbol);

        /**
         * Appends red colored {@code error}
         */
        @Deprecated
        @DeprecationInfo("Use #renderError(LocalizeValue)")
        default void renderError(@Nonnull String error) {
            renderError(LocalizeValue.of(error));
        }

        /**
         * Appends red colored {@code error}
         */
        void renderError(@Nonnull LocalizeValue errorValue);
    }
}