/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorFontType;
import consulo.ide.impl.idea.openapi.vcs.annotate.TextAnnotationPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnAction;
import consulo.util.lang.Couple;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * @author Irina Chernushina
 * @author Konstantin Bulenkov
 */
public abstract class AnnotationFieldGutter implements ActiveAnnotationGutter {
    @Nonnull
    protected final FileAnnotation myAnnotation;
    @Nonnull
    private final TextAnnotationPresentation myPresentation;
    @Nullable
    private Couple<Map<VcsRevisionNumber, ColorValue>> myColorScheme;

    AnnotationFieldGutter(
        @Nonnull FileAnnotation annotation,
        @Nonnull TextAnnotationPresentation presentation,
        @Nullable Couple<Map<VcsRevisionNumber, ColorValue>> colorScheme
    ) {
        myAnnotation = annotation;
        myPresentation = presentation;
        myColorScheme = colorScheme;
    }

    public boolean isGutterAction() {
        return false;
    }

    @Nullable
    @Override
    public String getToolTip(final int line, final Editor editor) {
        return null;
    }

    @Override
    public void doAction(int line) {
    }

    @Override
    public Cursor getCursor(final int line) {
        return Cursor.getDefaultCursor();
    }

    @Override
    public EditorFontType getStyle(final int line, final Editor editor) {
        return myPresentation.getFontType(line);
    }

    @Nullable
    @Override
    public EditorColorKey getColor(final int line, final Editor editor) {
        return myPresentation.getColor(line);
    }

    @Override
    public List<AnAction> getPopupActions(int line, final Editor editor) {
        return myPresentation.getActions(line);
    }

    @Override
    public void gutterClosed() {
        myPresentation.gutterClosed();
    }

    @Nullable
    @Override
    public ColorValue getBgColor(int line, Editor editor) {
        if (myColorScheme == null) {
            return null;
        }
        ColorMode type = ShowAnnotationColorsAction.getType();
        Map<VcsRevisionNumber, ColorValue> colorMap = type == ColorMode.AUTHOR ? myColorScheme.second : myColorScheme.first;
        if (colorMap == null || type == ColorMode.NONE) {
            return null;
        }
        final VcsRevisionNumber number = myAnnotation.getLineRevisionNumber(line);
        if (number == null) {
            return null;
        }
        return colorMap.get(number);
    }

    public boolean isShowByDefault() {
        return true;
    }

    public boolean isAvailable() {
        return VcsUtil.isAspectAvailableByDefault(getID(), isShowByDefault());
    }

    @Nullable
    public String getID() {
        return null;
    }

    @Override
    public int getLeftMargin() {
        return 0;
    }
}
