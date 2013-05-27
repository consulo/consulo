package com.intellij.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 11:52/27.05.13
 */
public abstract class CorePsiPackageManager extends PsiPackageManager {
  private final List<VirtualFile> myClasspath = new ArrayList<VirtualFile>();
  private final Project myProject;
  private final PsiManager myPsiManager;

  public CorePsiPackageManager(Project project, PsiManager psiManager) {
    myProject = project;
    myPsiManager = psiManager;
  }

  public abstract PsiPackage createPackage(@NotNull PsiManager psiManager,
                                           @NotNull PsiPackageManager psiPackageManager,
                                           @NotNull Class<? extends ModuleExtension> extensionClass,
                                           @NotNull String qualifiedName);

  @Override
  public void dropCache(@NotNull Class<? extends ModuleExtension> extensionClass) {

  }

  @Nullable
  @Override
  public PsiPackage findPackage(@NotNull String qualifiedName, @NotNull Class<? extends ModuleExtension> extensionClass) {
    return createPackage(myPsiManager, this, extensionClass, qualifiedName);
  }

  @Nullable
  @Override
  public PsiPackage findPackage(@NotNull PsiDirectory directory, @NotNull Class<? extends ModuleExtension> extensionClass) {
    final VirtualFile file = directory.getVirtualFile();
    for (VirtualFile root : myClasspath) {
      if (VfsUtilCore.isAncestor(root, file, false)) {
        String relativePath = FileUtil.getRelativePath(root.getPath(), file.getPath(), '/');
        if (relativePath == null) {
          continue;
        }
        return createPackage(myPsiManager, this, extensionClass, relativePath.replace('/', '.'));
      }
    }
    return null;
  }

  @Nullable
  @Override
  public PsiPackage findAnyPackage(@NotNull PsiDirectory directory) {
    return findPackage(directory, CoreModuleExtension.class);
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProject;
  }

  private List<VirtualFile> roots() {
    return myClasspath;
  }

  private List<VirtualFile> findDirectoriesByPackageName(String packageName) {
    List<VirtualFile> result = new ArrayList<VirtualFile>();
    String dirName = packageName.replace(".", "/");
    for (VirtualFile root : roots()) {
      VirtualFile classDir = root.findFileByRelativePath(dirName);
      if (classDir != null) {
        result.add(classDir);
      }
    }
    return result;
  }

  public void addToClasspath(VirtualFile root) {
    myClasspath.add(root);
  }
}
