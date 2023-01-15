/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.documentation.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.CopyPasteManager;

import java.awt.datatransfer.StringSelection;

/**
 * @author Denis Zhdanov
 * @since 3/29/11 1:28 PM
 */
public class CopyQuickDocAction extends AnAction implements DumbAware, HintManagerImpl.ActionToIgnore {

  public CopyQuickDocAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    String selected = e.getData(DocumentationManagerHelper.SELECTED_QUICK_DOC_TEXT);
    if (selected == null || selected.isEmpty()) {
      return;
    }

    CopyPasteManager.getInstance().setContents(new StringSelection(selected));
  }

  @Override
  public void update(AnActionEvent e) {
    String selected = e.getData(DocumentationManagerHelper.SELECTED_QUICK_DOC_TEXT);
    e.getPresentation().setEnabled(selected != null && !selected.isEmpty());
  }
}
