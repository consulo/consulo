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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.codeEditor.EditorKeys;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.codeEditor.Editor;
import consulo.application.dumb.DumbAware;
import jakarta.annotation.Nonnull;

/**
 * @author max
 * @since 2002-05-14
 */
public abstract class EditorToggleDecorationAction extends ToggleAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public final void setSelected(@Nonnull AnActionEvent e, boolean state) {
    Editor editor = e.getRequiredData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
    setOption(editor, state);
    editor.getComponent().repaint();
  }

  @Override
  public final boolean isSelected(@Nonnull AnActionEvent e) {
    Editor editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
    return editor != null && getOption(editor);
  }

  @Override
  public final void update(@Nonnull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(e.hasData(EditorKeys.EDITOR_EVEN_IF_INACTIVE));
  }
  
  protected abstract void setOption(Editor editor, boolean state);
  protected abstract boolean getOption(Editor editor);
}
