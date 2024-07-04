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

package consulo.language.editor.refactoring.copy;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.structureView.StructureViewFactoryEx;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.TwoPaneIdeView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;

public class CopyHandler {
  private CopyHandler() {
  }

  public static boolean canCopy(PsiElement[] elements) {
    if (elements.length > 0) {
      for (CopyHandlerDelegate delegate : CopyHandlerDelegate.EP_NAME.getExtensionList()) {
        if (delegate instanceof CopyHandlerDelegateBase copyHandlerDelegate
          ? copyHandlerDelegate.canCopy(elements, true) : delegate.canCopy(elements)) {
          return true;
        }
      }
    }
    return false;
  }


  public static void doCopy(PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
    if (elements.length == 0) return;
    for (CopyHandlerDelegate delegate : CopyHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canCopy(elements)) {
        delegate.doCopy(elements, defaultTargetDirectory);
        break;
      }
    }
  }

  public static boolean canClone(PsiElement[] elements) {
    if (elements.length > 0) {
      for (CopyHandlerDelegate delegate : CopyHandlerDelegate.EP_NAME.getExtensionList()) {
        if (delegate instanceof CopyHandlerDelegateBase copyDelegate
          ? copyDelegate.canCopy(elements, true) : delegate.canCopy(elements)) {
          return !(delegate instanceof CopyHandlerDelegateBase copyDelegate && copyDelegate.forbidToClone(elements, true));
        }
      }
    }
    return false;
  }

  public static void doClone(PsiElement element) {
    PsiElement[] elements = new PsiElement[]{element};
    for (CopyHandlerDelegate delegate : CopyHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canCopy(elements)) {
        if (delegate instanceof CopyHandlerDelegateBase && ((CopyHandlerDelegateBase)delegate).forbidToClone(elements, false)) {
          return;
        }
        delegate.doClone(element);
        break;
      }
    }
  }

  public static void updateSelectionInActiveProjectView(PsiElement newElement, Project project, boolean selectInActivePanel) {
    String id = ToolWindowManager.getInstance(project).getActiveToolWindowId();
    if (id != null) {
      ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(id);
      Content selectedContent = window.getContentManager().getSelectedContent();
      if (selectedContent != null) {
        JComponent component = selectedContent.getComponent();
        if (component instanceof TwoPaneIdeView twoPaneIdeView) {
          twoPaneIdeView.selectElement(newElement, selectInActivePanel);
          return;
        }
      }
    }
    if (ToolWindowId.PROJECT_VIEW.equals(id)) {
      ProjectView.getInstance(project).selectPsiElement(newElement, true);
    }
    else if (ToolWindowId.STRUCTURE_VIEW.equals(id)) {
      VirtualFile virtualFile = newElement.getContainingFile().getVirtualFile();
      FileEditor editor = FileEditorManager.getInstance(newElement.getProject()).getSelectedEditor(virtualFile);
      StructureViewFactoryEx.getInstanceEx(project).getStructureViewWrapper().selectCurrentElement(editor, virtualFile, true);
    }
  }
}
