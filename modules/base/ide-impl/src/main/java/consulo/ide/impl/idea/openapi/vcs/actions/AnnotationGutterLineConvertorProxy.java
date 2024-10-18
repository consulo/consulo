/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorFontType;
import consulo.ide.impl.idea.openapi.localVcs.UpToDateLineNumberProvider;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.ui.color.ColorValue;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.List;

/**
 * @author Irina Chernushina
 * @author Konstantin Bulenkov
 */
public class AnnotationGutterLineConvertorProxy implements ActiveAnnotationGutter {
    private final UpToDateLineNumberProvider myGetUpToDateLineNumber;
    private final ActiveAnnotationGutter myDelegate;

    public AnnotationGutterLineConvertorProxy(UpToDateLineNumberProvider getUpToDateLineNumber, ActiveAnnotationGutter delegate) {
        myGetUpToDateLineNumber = getUpToDateLineNumber;
        myDelegate = delegate;
    }

    @Override
    public String getLineText(int line, Editor editor) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
        return currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER ? "" : myDelegate.getLineText(currentLine, editor);
    }

    @Nonnull
    @Override
    public LocalizeValue getToolTipValue(int line, Editor editor) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
        return currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER
            ? LocalizeValue.empty()
            : myDelegate.getToolTipValue(currentLine, editor);
    }

    @Override
    public EditorFontType getStyle(int line, Editor editor) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
        return currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER
            ? EditorFontType.PLAIN
            : myDelegate.getStyle(currentLine, editor);
    }

    @Override
    public EditorColorKey getColor(int line, Editor editor) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
        return currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER
            ? AnnotationSource.LOCAL.getColor()
            : myDelegate.getColor(currentLine, editor);
    }

    @Override
    public ColorValue getBgColor(int line, Editor editor) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
        return currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER ? null : myDelegate.getBgColor(currentLine, editor);
    }

    @Override
    public List<AnAction> getPopupActions(int line, Editor editor) {
        return myDelegate.getPopupActions(line, editor);
    }

    @Override
    public void gutterClosed() {
        myDelegate.gutterClosed();
    }

    @Override
    public void doAction(int lineNum) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(lineNum);
        if (currentLine != UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) {
            myDelegate.doAction(currentLine);
        }
    }

    @Override
    public Cursor getCursor(int lineNum) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(lineNum);
        return currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER
            ? Cursor.getDefaultCursor()
            : myDelegate.getCursor(currentLine);
    }
}
