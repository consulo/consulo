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

package consulo.ide.impl.idea.codeInsight;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.editor.CodeInsightUtilCore;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.util.LanguageEditorUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Singleton
@ServiceImpl
public class CodeInsightUtilBase extends CodeInsightUtilCore {
  @Override
  public boolean prepareFileForWrite(@Nullable PsiFile psiFile) {
    if (psiFile == null) return false;
    final VirtualFile file = psiFile.getVirtualFile();
    final Project project = psiFile.getProject();

    if (ReadonlyStatusHandler.ensureFilesWritable(project, file)) {
      return true;
    }
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptorImpl(project, file), true);
        if (editor != null && editor.getComponent().isDisplayable()) {
          HintManager.getInstance().showErrorHint(editor, CodeInsightLocalize.errorHintFileIsReadonly(file.getPresentableUrl()));
        }
      }
    }, project.getDisposed());

    return false;
  }

  @Override
  public boolean preparePsiElementForWrite(@Nullable PsiElement element) {
    PsiFile file = element == null ? null : element.getContainingFile();
    return prepareFileForWrite(file);
  }

  @Override
  public boolean preparePsiElementsForWrite(@Nonnull PsiElement... elements) {
    return preparePsiElementsForWrite(Arrays.asList(elements));
  }

  @Override
  public boolean preparePsiElementsForWrite(@Nonnull Collection<? extends PsiElement> elements) {
    if (elements.isEmpty()) return true;
    Set<VirtualFile> files = new HashSet<VirtualFile>();
    Project project = null;
    for (PsiElement element : elements) {
      if (element == null) continue;
      PsiFile file = element.getContainingFile();
      if (file == null || !file.isPhysical()) continue;
      project = file.getProject();
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile == null) continue;
      files.add(virtualFile);
    }
    if (!files.isEmpty()) {
      VirtualFile[] virtualFiles = VfsUtilCore.toVirtualFileArray(files);
      ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFiles);
      return !status.hasReadonlyFiles();
    }
    return true;
  }

  @Override
  public boolean prepareVirtualFilesForWrite(@Nonnull Project project, @Nonnull Collection<VirtualFile> files) {
    ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files);
    return !status.hasReadonlyFiles();
  }

  @Deprecated
  @DeprecationInfo("See LanguageEditorUtil#checkModificationAllowed")
  public static boolean prepareEditorForWrite(@Nonnull Editor editor) {
    return LanguageEditorUtil.checkModificationAllowed(editor);
  }
}
