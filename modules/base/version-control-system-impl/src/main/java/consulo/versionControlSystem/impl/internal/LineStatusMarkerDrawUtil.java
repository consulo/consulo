/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.impl.internal;

import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.diff.DiffColors;
import consulo.diff.internal.DiffImplUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.paint.RectanglePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.internal.VcsRange;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Colour lookups and gutter hit-testing for VCS line-status markers.
 * <p>
 * The drawing itself moved to the platform painters once markers became declarative; what is left
 * is the scheme mapping shared by every platform, plus the mouse-area test.
 */
public final class LineStatusMarkerDrawUtil {

    private LineStatusMarkerDrawUtil() {
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static boolean isInsideMarkerArea(MouseEvent e) {
        EditorGutterComponentEx gutter = (EditorGutterComponentEx) e.getComponent();
        return gutter.isInsideMarkerArea(e);
    }

    // -------------------------------------------------------------------------
    // Color helpers
    // -------------------------------------------------------------------------

    public static @Nullable ColorValue getGutterColor(VcsRange range, @Nullable Editor editor) {
        EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case VcsRange.INSERTED -> scheme.getColor(EditorColors.ADDED_LINES_COLOR);
            case VcsRange.DELETED -> scheme.getColor(EditorColors.DELETED_LINES_COLOR);
            case VcsRange.MODIFIED -> scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
            default -> throw new AssertionError("Unknown range type: " + range.getType());
        };
    }

    public static @Nullable ColorValue getGutterColor(VcsRange.@Nullable InnerRange range, @Nullable Editor editor) {
        EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case VcsRange.INSERTED -> scheme.getColor(EditorColors.ADDED_LINES_COLOR);
            case VcsRange.DELETED -> scheme.getColor(EditorColors.DELETED_LINES_COLOR);
            case VcsRange.MODIFIED -> scheme.getColor(EditorColors.MODIFIED_LINES_COLOR);
            case VcsRange.EQUAL -> scheme.getColor(EditorColors.WHITESPACES_MODIFIED_LINES_COLOR);
            default -> throw new AssertionError("Unknown inner range type: " + range.getType());
        };
    }

    public static @Nullable ColorValue getErrorStripeColor(VcsRange range, @Nullable Editor editor) {
        EditorColorsScheme scheme = getColorScheme(editor);
        return switch (range.getType()) {
            case VcsRange.INSERTED -> scheme.getAttributes(DiffColors.DIFF_INSERTED).getErrorStripeColor();
            case VcsRange.DELETED -> scheme.getAttributes(DiffColors.DIFF_DELETED).getErrorStripeColor();
            case VcsRange.MODIFIED -> scheme.getAttributes(DiffColors.DIFF_MODIFIED).getErrorStripeColor();
            default -> throw new AssertionError("Unknown range type: " + range.getType());
        };
    }

    public static @Nullable ColorValue getGutterBorderColor(@Nullable Editor editor) {
        return getColorScheme(editor).getColor(EditorColors.BORDER_LINES_COLOR);
    }

    public static EditorColorsScheme getColorScheme(@Nullable Editor editor) {
        return editor != null ? editor.getColorsScheme() : EditorColorsManager.getInstance().getGlobalScheme();
    }

}
