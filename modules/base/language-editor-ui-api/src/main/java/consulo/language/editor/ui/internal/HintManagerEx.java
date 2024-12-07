/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.ui.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.VisualPosition;
import consulo.language.editor.hint.HintManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.LightweightHint;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2024-12-06
 */
public interface HintManagerEx extends HintManager {
    void showEditorHint(LightweightHint hint,
                        Editor editor,
                        @PositionFlags short constraint,
                        @HideFlags int flags,
                        int timeout,
                        boolean reviveOnEditorChange);

    void showEditorHint(
        @Nonnull final LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull Point p,
        @HideFlags int flags,
        int timeout,
        boolean reviveOnEditorChange
    );

    void showEditorHint(
        @Nonnull final LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull Point p,
        @HideFlags int flags,
        int timeout,
        boolean reviveOnEditorChange,
        @PositionFlags short position
    );

    public void showEditorHint(
        @Nonnull final LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull Point p,
        @HideFlags int flags,
        int timeout,
        boolean reviveOnEditorChange,
        HintHint hintInfo
    );

    HintHint createHintHint(Editor editor, Point p, LightweightHint hint, @PositionFlags short constraint);

    HintHint createHintHint(
        Editor editor,
        Point p,
        LightweightHint hint,
        @PositionFlags short constraint,
        boolean createInEditorComponent
    );

    void adjustEditorHintPosition(
        final LightweightHint hint,
        final Editor editor,
        final Point p,
        @PositionFlags short constraint
    );

    @RequiredUIAccess
    Point getHintPosition(@Nonnull LightweightHint hint, @Nonnull Editor editor, @PositionFlags short constraint);

    @RequiredUIAccess
    Point getHintPosition(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull VisualPosition pos,
        @PositionFlags short constraint
    );

    @RequiredUIAccess
    Point getHintPosition(
        @Nonnull LightweightHint hint,
        @Nonnull Editor editor,
        @Nonnull LogicalPosition pos,
        @PositionFlags short constraint
    );

    void updateLocation(final LightweightHint hint, final Editor editor, Point p);
}
