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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.colorScheme.EditorFontsConstants;
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

  protected ChangeEditorFontSizeAction(@Nullable String text, int increaseStep) {
    super(text);
    myStep = increaseStep;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final RealEditor editor = getEditor(e);
    if (editor != null) {
      final int size = editor.getFontSize() + myStep;
      if (size >= 8 && size <= EditorFontsConstants.getMaxEditorFontSize()) {
        editor.setFontSize(size);
      }
    }
  }

  @Nullable
  private static RealEditor getEditor(@Nonnull AnActionEvent e) {
    final Editor editor = e.getData(Editor.KEY);
    return editor instanceof RealEditor realEditor ? realEditor : null;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(getEditor(e) != null);
  }

  public static class IncreaseEditorFontSize extends ChangeEditorFontSizeAction {
    public IncreaseEditorFontSize() {
      super(CodeEditorLocalize.increaseEditorFont().get(), 1);
    }
  }

  public static class DecreaseEditorFontSize extends ChangeEditorFontSizeAction {
    public DecreaseEditorFontSize() {
      super(CodeEditorLocalize.decreaseEditorFont().get(), -1);
    }
  }
}
