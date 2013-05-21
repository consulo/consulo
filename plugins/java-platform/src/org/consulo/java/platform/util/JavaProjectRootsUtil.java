package org.consulo.java.platform.util;

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiFile;
import org.consulo.java.platform.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 13:10/21.05.13
 */
public class JavaProjectRootsUtil extends ProjectRootsUtil {
  public static boolean isJavaSourceFile(@NotNull Project project, @NotNull VirtualFile file, boolean withLibrary) {
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    if (file.isDirectory()) {
      return false;
    }
    if (file.getFileType() != StdFileTypes.JAVA) {
      return false;
    }
    if (fileTypeManager.isFileIgnored(file)) {
      return false;
    }

    Module module = ModuleUtilCore.findModuleForFile(file, project);
    if (module == null) {
      return false;
    }

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    if (moduleRootManager.getExtension(JavaModuleExtension.class) == null) {
      return false;
    }

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return fileIndex.isInSource(file) || withLibrary && fileIndex.isInLibraryClasses(file);
  }

  public static boolean isOutsideSourceRoot(@Nullable PsiFile psiFile) {
    if (psiFile == null) {
      return false;
    }
    if (psiFile instanceof PsiCodeFragment) {
      return false;
    }
    final VirtualFile file = psiFile.getVirtualFile();
    if (file == null) {
      return false;
    }

    Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
    if (module == null) {
      return false;
    }

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    if (moduleRootManager.getExtension(JavaModuleExtension.class) == null) {
      return true;
    }
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex();
    return !projectFileIndex.isInSource(file) && !projectFileIndex.isInLibraryClasses(file);
  }
}
