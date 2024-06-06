/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.psi.path;

import consulo.application.ApplicationManager;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.language.editor.impl.intention.CreateFileFix;
import consulo.language.editor.impl.intention.RenameFileFix;
import consulo.language.editor.impl.internal.intention.RenameFileReferenceIntentionAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.template.EditorFileTemplateUtil;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.*;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.path.FileReferenceSet;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Maxim.Mossienko
 */
public class FileReferenceQuickFixProvider {
  private FileReferenceQuickFixProvider() {
  }

  @Nonnull
  public static List<? extends LocalQuickFix> registerQuickFix(@Nonnull FileReference reference) {
    final FileReferenceSet fileReferenceSet = reference.getFileReferenceSet();
    int index = reference.getIndex();

    if (index < 0) return Collections.emptyList();
    final String newFileName = reference.getFileNameToCreate();

    // check if we could create file
    if (newFileName.isEmpty() || newFileName.indexOf('\\') != -1 || newFileName.indexOf('*') != -1 || newFileName.indexOf('?') != -1 || Platform.current().os().isWindows() && newFileName.indexOf(':') != -1) {
      return Collections.emptyList();
    }

    PsiFileSystemItem context = null;
    PsiElement element = reference.getElement();
    PsiFile containingFile = element == null ? null : element.getContainingFile();

    if (index > 0) {
      context = fileReferenceSet.getReference(index - 1).resolve();
    }
    else { // index == 0
      final Collection<PsiFileSystemItem> defaultContexts = fileReferenceSet.getDefaultContexts();
      if (defaultContexts.isEmpty()) {
        return Collections.emptyList();
      }

      Module module = containingFile == null ? null : ModuleUtilCore.findModuleForPsiElement(containingFile);

      for (PsiFileSystemItem defaultContext : defaultContexts) {
        if (defaultContext != null) {
          final VirtualFile virtualFile = defaultContext.getVirtualFile();
          if (virtualFile != null && defaultContext.isDirectory() && virtualFile.isInLocalFileSystem()) {
            if (context == null) {
              context = defaultContext;
            }
            if (module != null && module == getModuleForContext(defaultContext)) {
              // fixes IDEA-64156
              // todo: fix it on PsiFileReferenceHelper level in 10.X
              context = defaultContext;
              break;
            }
          }
        }
      }
      if (context == null && ApplicationManager.getApplication().isUnitTestMode()) {
        context = defaultContexts.iterator().next();
      }
    }
    if (context == null) return Collections.emptyList();

    final VirtualFile virtualFile = context.getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return Collections.emptyList();

    final PsiDirectory directory = context.getManager().findDirectory(virtualFile);
    if (directory == null) return Collections.emptyList();

    if (fileReferenceSet.isCaseSensitive()) {
      final PsiElement psiElement = containingFile == null ? null : reference.innerSingleResolve(false, containingFile);

      if (psiElement != null) {
        final String existingElementName = ((PsiNamedElement)psiElement).getName();

        final RenameFileReferenceIntentionAction renameRefAction = new RenameFileReferenceIntentionAction(existingElementName, reference);
        final RenameFileFix renameFileFix = new RenameFileFix(newFileName);
        return Arrays.asList(renameRefAction, renameFileFix);
      }
    }

    final boolean isdirectory;

    if (!reference.isLast()) {
      // directory
      try {
        directory.checkCreateSubdirectory(newFileName);
      }
      catch (IncorrectOperationException ex) {
        return Collections.emptyList();
      }
      isdirectory = true;
    }
    else {
      FileType ft = FileTypeManager.getInstance().getFileTypeByFileName(newFileName);
      if (ft instanceof UnknownFileType) return Collections.emptyList();

      try {
        directory.checkCreateFile(newFileName);
      }
      catch (IncorrectOperationException ex) {
        return Collections.emptyList();
      }

      isdirectory = false;
    }

    final CreateFileFix action = new MyCreateFileFix(isdirectory, newFileName, directory, reference);
    return Collections.singletonList(action);
  }


  @Nullable
  private static Module getModuleForContext(@Nonnull PsiFileSystemItem context) {
    VirtualFile file = context.getVirtualFile();
    return file != null ? ModuleUtilCore.findModuleForFile(file, context.getProject()) : null;
  }

  private static class MyCreateFileFix extends CreateFileFix {
    private final boolean isDirectory;
    private final String myNewFileTemplateName;

    public MyCreateFileFix(boolean isdirectory, String newFileName, PsiDirectory directory, FileReference reference) {
      super(isdirectory, newFileName, directory);
      isDirectory = isdirectory;
      myNewFileTemplateName = isDirectory ? null : reference.getNewFileTemplateName();
    }

    @Override
    protected String getFileText() {
      if (!isDirectory && myNewFileTemplateName != null) {
        Project project = getStartElement().getProject();
        FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
        FileTemplate template = findTemplate(fileTemplateManager);

        if (template != null) {
          try {
            return template.getText(fileTemplateManager.getDefaultProperties());
          }
          catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
      return super.getFileText();
    }

    private FileTemplate findTemplate(FileTemplateManager fileTemplateManager) {
      FileTemplate template = fileTemplateManager.getTemplate(myNewFileTemplateName);
      if (template == null) template = fileTemplateManager.findInternalTemplate(myNewFileTemplateName);
      if (template == null) {
        for (FileTemplate fileTemplate : fileTemplateManager.getAllJ2eeTemplates()) {
          final String fileTemplateWithExtension = fileTemplate.getName() + '.' + fileTemplate.getExtension();
          if (fileTemplateWithExtension.equals(myNewFileTemplateName)) {
            return fileTemplate;
          }
        }
      }
      return template;
    }

    @Override
    protected void openFile(@Nonnull Project project, PsiDirectory directory, PsiFile newFile, String text) {
      super.openFile(project, directory, newFile, text);
      if (!isDirectory && myNewFileTemplateName != null) {
        FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
        FileTemplate template = findTemplate(fileTemplateManager);

        if (template != null && template.isLiveTemplateEnabled()) {
          EditorFileTemplateUtil.startLiveTemplate(newFile);
        }
      }
    }
  }
}
