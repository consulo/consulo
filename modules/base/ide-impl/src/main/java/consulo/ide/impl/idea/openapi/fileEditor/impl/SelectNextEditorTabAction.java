/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;

/**
 * The only purpose of this action is to serve as placeholder for assigning keyboard shortcuts.
 * For actual tab switching code, see EditorComposite constructor.
 *
 * @author max
 */
public class SelectNextEditorTabAction extends AnAction implements DumbAware {
  public void actionPerformed(final AnActionEvent e) {
  }

  @Override
  public void update(AnActionEvent e) {
    // allow plugins to use the same keyboard shortcut
    e.getPresentation().setEnabled(false);
  }
}