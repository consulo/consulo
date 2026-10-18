/*
 * Copyright 2013-2026 consulo.io
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
package consulo.review.impl.internal.editor;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * One review control of a gutter line. Two renderers of the same line, kind and icon are equal, so the gutter keeps
 * the mark when the controls are laid out again unchanged.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewGutterActionIconRenderer extends GutterIconRenderer {
    private final int myLine;
    private final CodeReviewGutterActionType myType;
    private final Image myIcon;
    private final LocalizeValue myTooltip;
    private final Runnable myAction;

    public CodeReviewGutterActionIconRenderer(int line,
                                              CodeReviewGutterActionType type,
                                              Image icon,
                                              LocalizeValue tooltip,
                                              @RequiredUIAccess Runnable action) {
        myLine = line;
        myType = type;
        myIcon = icon;
        myTooltip = tooltip;
        myAction = action;
    }

    public int getLine() {
        return myLine;
    }

    public CodeReviewGutterActionType getType() {
        return myType;
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    @Override
    public LocalizeValue getTooltipValue() {
        return myTooltip;
    }

    @Override
    public AnAction getClickAction() {
        return new DumbAwareAction(myTooltip) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                myAction.run();
            }
        };
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.LEFT;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof CodeReviewGutterActionIconRenderer that
            && myLine == that.myLine
            && myType == that.myType
            && myIcon.equals(that.myIcon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myLine, myType, myIcon);
    }
}
