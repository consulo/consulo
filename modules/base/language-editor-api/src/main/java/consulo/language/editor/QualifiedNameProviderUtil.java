// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor;

import consulo.codeEditor.Editor;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;

public final class QualifiedNameProviderUtil {
  private QualifiedNameProviderUtil() {
  }

  @Nullable
  public static PsiElement adjustElementToCopy(@Nonnull PsiElement element) {
    return QualifiedNameProvider.EP_NAME.computeSafeIfAny(it -> it.adjustElementToCopy(element));
  }

  @Nullable
  public static String getQualifiedName(@Nonnull PsiElement element) {
    return QualifiedNameProvider.EP_NAME.computeSafeIfAny(it -> it.getQualifiedName(element));
  }

  @Nullable
  public static PsiElement qualifiedNameToElement(@Nonnull String qualifiedName, @Nonnull Project project) {
    return QualifiedNameProvider.EP_NAME.computeSafeIfAny(it -> it.qualifiedNameToElement(qualifiedName, project));
  }

  @Nullable
  public static String getQualifiedNameDumbAware(@Nullable PsiElement element) {
    if (element == null) return null;
    return DumbService.getInstance(element.getProject()).computeWithAlternativeResolveEnabled(() -> getQualifiedName(element));
  }

  @Nullable
  public static String elementToFqn(@Nullable final PsiElement element, @Nullable Editor editor) {
    String result = getQualifiedNameDumbAware(element);
    if (result != null) return result;

    //IDEA-70346
    if (editor != null) {
      PsiReference reference = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
      if (reference != null) {
        result = getQualifiedNameDumbAware(reference.resolve());
        if (result != null) return result;
      }
    }

    if (element instanceof PsiFile) {
      return FileUtil.toSystemIndependentName(getFileFqn((PsiFile)element));
    }
    if (element instanceof PsiDirectory) {
      return FileUtil.toSystemIndependentName(getVirtualFileFqn(((PsiDirectory)element).getVirtualFile(), element.getProject()));
    }

    return null;
  }

  @Nonnull
  public static String getFileFqn(final PsiFile file) {
    final VirtualFile virtualFile = file.getVirtualFile();
    return virtualFile == null ? file.getName() : getVirtualFileFqn(virtualFile, file.getProject());
  }

  @Nonnull
  public static String getVirtualFileFqn(@Nonnull VirtualFile virtualFile, @Nonnull Project project) {
    for (VirtualFileQualifiedNameProvider provider : VirtualFileQualifiedNameProvider.EP_NAME.getExtensionList()) {
      String qualifiedName = provider.getQualifiedName(project, virtualFile);
      if (qualifiedName != null) {
        return qualifiedName;
      }
    }

    Module module = ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile, false);
    if (module != null) {
      for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
        String relativePath = VirtualFileUtil.getRelativePath(virtualFile, root);
        if (relativePath != null) {
          return relativePath;
        }
      }
    }

    VirtualFile dir = project.getBaseDir();
    if (dir == null) {
      return virtualFile.getPath();
    }
    String relativePath = VirtualFileUtil.getRelativePath(virtualFile, dir);
    if (relativePath != null) {
      return relativePath;
    }

    RootType rootType = RootType.forFile(virtualFile);
    if (rootType != null) {                                     
      VirtualFile scratchRootVirtualFile = VirtualFileUtil.findFileByIoFile(new File(ScratchFileService.getInstance().getRootPath(rootType)), false);
      if (scratchRootVirtualFile != null) {
        String scratchRelativePath = VirtualFileUtil.getRelativePath(virtualFile, scratchRootVirtualFile);
        if (scratchRelativePath != null) {
          return scratchRelativePath;
        }
      }
    }

    return virtualFile.getPath();
  }
}
