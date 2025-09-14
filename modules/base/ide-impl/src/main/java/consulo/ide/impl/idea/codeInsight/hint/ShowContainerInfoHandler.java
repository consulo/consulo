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
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.document.util.TextRange;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.hint.DeclarationRangeUtil;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.editor.ui.internal.EditorFragmentComponent;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.lang.ref.WeakReference;

public class ShowContainerInfoHandler implements CodeInsightActionHandler {
  private static final Key<WeakReference<LightweightHint>> MY_LAST_HINT_KEY = Key.create("MY_LAST_HINT_KEY");
  private static final Key<PsiElement> CONTAINER_KEY = Key.create("CONTAINER_KEY");

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    PsiElement container = null;
    WeakReference<LightweightHint> ref = editor.getUserData(MY_LAST_HINT_KEY);
    LightweightHint hint = SoftReference.dereference(ref);
    if (hint != null && hint.isVisible()){
      hint.hide();
      container = hint.getUserData(CONTAINER_KEY);
      if (container != null && !container.isValid()){
        container = null;
      }
    }

    StructureViewBuilder builder = PsiStructureViewFactory.createBuilderForFile(file);
    if (builder instanceof TreeBasedStructureViewBuilder) {
      StructureViewModel model = ((TreeBasedStructureViewBuilder) builder).createStructureViewModel(editor);
      boolean goOneLevelUp = true;
      try {
        if (container == null) {
          goOneLevelUp = false;
          Object element = model.getCurrentEditorElement();
          if (element instanceof PsiElement) {
            container = (PsiElement) element;
          }
        }
      }
      finally {
        model.dispose();
      }
      while(true) {
        if (container == null || container instanceof PsiFile) {
          return;
        }
        if (goOneLevelUp) {
          goOneLevelUp = false;
        }
        else {
          if (!isDeclarationVisible(container, editor)) {
            break;
          }
        }

        container = container.getParent();
        while(container != null && DeclarationRangeUtil.getPossibleDeclarationAtRange(container) == null) {
          container = container.getParent();
          if (container instanceof PsiFile) return;
        }
      }
    }
    if (container == null) {
      return;
    }

    TextRange range = DeclarationRangeUtil.getPossibleDeclarationAtRange(container);
    if (range == null) {
      return;
    }
    PsiElement _container = container;
    ApplicationManager.getApplication().invokeLater(() -> {
      LightweightHint hint1 = EditorFragmentComponent.showEditorFragmentHint(editor, range, true, true);
      if (hint1 != null) {
        hint1.putUserData(CONTAINER_KEY, _container);
        editor.putUserData(MY_LAST_HINT_KEY, new WeakReference<>(hint1));
      }
    });
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private static boolean isDeclarationVisible(PsiElement container, Editor editor) {
    Rectangle viewRect = editor.getScrollingModel().getVisibleArea();
    TextRange range = DeclarationRangeUtil.getPossibleDeclarationAtRange(container);
    if (range == null) {
      return false;
    }

    LogicalPosition pos = editor.offsetToLogicalPosition(range.getStartOffset());
    Point loc = editor.logicalPositionToXY(pos);
    return loc.y >= viewRect.y;
  }
}
