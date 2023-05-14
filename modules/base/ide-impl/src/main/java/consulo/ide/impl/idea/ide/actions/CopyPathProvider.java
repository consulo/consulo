/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.ui.tabs.impl.TabLabel;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.ide.IdeBundle;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;

// from kotlin
public class CopyPathProvider extends DumbAwareAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    DataContext dataContext = e.getDataContext();
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);

    DataContext customDataContext = createCustomDataContext(dataContext);

    List<PsiElement> elements = CopyReferenceUtil.getElementsToCopy(editor, customDataContext);
    if (project != null) {
      String copy = getQualifiedName(project, elements, editor, customDataContext);
      CopyPasteManager.getInstance().setContents(new StringSelection(copy));
      CopyReferenceUtil.setStatusBarText(project, IdeBundle.message("message.path.to.fqn.has.been.copied", copy));

      CopyReferenceUtil.highlight(editor, project, elements);
    }
  }

  private DataContext createCustomDataContext(DataContext dataContext) {
    Component component = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
    if (!(component instanceof TabLabel)) {
      return dataContext;
    }

    Object file = ((TabLabel)component).getInfo().getObject();
    if (!(file instanceof VirtualFile)) {
      return dataContext;
    }

    return SimpleDataContext.builder().setParent(dataContext).add(LangDataKeys.VIRTUAL_FILE, (VirtualFile)file).add(LangDataKeys.VIRTUAL_FILE_ARRAY, new VirtualFile[]{(VirtualFile)file}).build();
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();

    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);

    Project project = e.getData(CommonDataKeys.PROJECT);

    e.getPresentation().setEnabledAndVisible(project != null && getQualifiedName(project, CopyReferenceUtil.getElementsToCopy(editor, dataContext), editor, dataContext) != null);
  }

  @Nullable
  public String getQualifiedName(Project project, List<PsiElement> elements, Editor editor, DataContext dataContext) {
    if (elements.isEmpty()) {
      VirtualFile file = Optional.ofNullable(editor).map(Editor::getDocument).map(it -> FileDocumentManager.getInstance().getFile(it)).orElse(null);
      return getPathToElement(project, file, editor);
    }
    else {
      List<VirtualFile> files = ContainerUtil.mapNotNull(elements, it -> {
        if (it instanceof PsiFileSystemItem) {
          return ((PsiFileSystemItem)it).getVirtualFile();
        }
        else {
          PsiFile containingFile = it.getContainingFile();
          return containingFile == null ? null : containingFile.getVirtualFile();
        }
      });

      if (files.isEmpty()) {
        VirtualFile[] contextFiles = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if(contextFiles != null && contextFiles.length > 0) {
          files = List.of(contextFiles);
        }
      }

      if(files.isEmpty()) {
        return null;
      }

      List<String> paths = ContainerUtil.mapNotNull(files, file -> getPathToElement(project, file, editor));
      if(paths.isEmpty()) {
        return null;
      }
      return String.join("\n", paths);
    }
  }

  @Nullable
  public String getPathToElement(Project project, @Nullable VirtualFile virtualFile, @Nullable Editor editor) {
    return null;
  }
}
