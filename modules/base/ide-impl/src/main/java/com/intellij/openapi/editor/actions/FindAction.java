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

package com.intellij.openapi.editor.actions;

import com.intellij.find.FindUtil;
import consulo.dataContext.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import consulo.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FindAction extends EditorAction {
  private static class Handler extends EditorActionHandler {
    @Override
    public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
      Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(CommonDataKeys.PROJECT);
      FindUtil.find(project, editor);
    }

    @Override
    public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
      Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(CommonDataKeys.PROJECT);
      return project != null;
    }
  }

  public FindAction() {
    super(new Handler());
  }
}
