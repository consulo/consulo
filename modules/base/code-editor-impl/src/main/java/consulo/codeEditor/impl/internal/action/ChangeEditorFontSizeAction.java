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
package consulo.codeEditor.impl.internal.action;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.colorScheme.EditorFontsConstants;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class ChangeEditorFontSizeAction extends AnAction implements DumbAware {
    private final int myStep;

    protected ChangeEditorFontSizeAction(@Nonnull LocalizeValue text, int increaseStep) {
        super(text);
        myStep = increaseStep;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        RealEditor editor = getEditor(e);
        if (editor != null) {
            int size = editor.getFontSize() + myStep;
            if (size >= 8 && size <= EditorFontsConstants.getMaxEditorFontSize()) {
                editor.setFontSize(size);
            }
        }
    }

    @Nullable
    private static RealEditor getEditor(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        return editor instanceof RealEditor realEditor ? realEditor : null;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(getEditor(e) != null);
    }

}
