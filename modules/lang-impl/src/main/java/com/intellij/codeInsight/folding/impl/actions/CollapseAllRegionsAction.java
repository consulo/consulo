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

package com.intellij.codeInsight.folding.impl.actions;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;

public class CollapseAllRegionsAction extends EditorAction {
  public CollapseAllRegionsAction() {
    super(new EditorActionHandler() {
      @Override
      public void execute(final Editor editor, DataContext dataContext) {
        Project project = editor.getProject();
        assert project != null;
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
        editor.getFoldingModel().runBatchFoldingOperation(new Runnable() {
          @Override
          public void run() {
            for (FoldRegion region : editor.getFoldingModel().getAllFoldRegions()) {
              region.setExpanded(false);
            }
          }
        });
      }

      @Override
      public boolean isEnabled(Editor editor, DataContext dataContext) {
        return super.isEnabled(editor, dataContext) && editor.getProject() != null;
      }
    });
  }

}
