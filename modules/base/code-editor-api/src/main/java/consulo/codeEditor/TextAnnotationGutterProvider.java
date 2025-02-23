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
package consulo.codeEditor;

import consulo.annotation.DeprecationInfo;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorFontType;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnAction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Interface which should be implemented in order to draw custom text annotations in the
 * editor gutter.
 *
 * @author max
 * @author Konstantin Bulenkov
 * @see EditorGutter#registerTextAnnotation(TextAnnotationGutterProvider)
 */
public interface TextAnnotationGutterProvider {
    /**
     * Returns the text which should be drawn for the line with the specified number in the specified editor.
     *
     * @param line   the line for which the text is requested.
     * @param editor the editor in which the text will be drawn.
     * @return the text to draw, or null if no text should be drawn.
     */
    @Nullable
    String getLineText(int line, Editor editor);

    //TODO: rename into getToolTip() after deprecation deletion
    @Nonnull
    default LocalizeValue getToolTipValue(int line, Editor editor) {
        return LocalizeValue.ofNullable(getToolTip(line, editor));
    }

    @Deprecated
    @DeprecationInfo("Use getToolTipValue(int)")
    @Nullable
    default String getToolTip(int line, Editor editor) {
        LocalizeValue toolTipValue = getToolTipValue(line, editor);
        return toolTipValue == LocalizeValue.empty() ? null : toolTipValue.get();
    }

    EditorFontType getStyle(int line, Editor editor);

    @Nullable
    EditorColorKey getColor(int line, Editor editor);

    /**
     * Returns the background color for the text
     *
     * @param line   the line for which the background color is requested.
     * @param editor the editor in which the text will be drawn.
     * @return the text to draw, or null if no text should be drawn.
     */
    @Nullable
    ColorValue getBgColor(int line, Editor editor);

    /***
     * enables annotation view modifications
     */
    List<AnAction> getPopupActions(final int line, final Editor editor);

    /**
     * Called when the annotations are removed from the editor gutter.
     *
     * @see EditorGutter#closeAllAnnotations()
     */
    void gutterClosed();

    /**
     * If {@code true}, a couple of pixels will be added at both sides of displayed text (if it's not empty),
     * otherwise the width of annotation will be equal to the width of provided text.
     */
    default boolean useMargin() {
        return true;
    }

    default int getLeftMargin() {
        return -1;
    }
}
