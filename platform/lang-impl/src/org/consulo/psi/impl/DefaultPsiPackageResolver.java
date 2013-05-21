package org.consulo.psi.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;
import org.consulo.psi.PsiPackageResolver;
import org.consulo.psi.PsiPackageSupportProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 16:54/21.05.13
 */
public class DefaultPsiPackageResolver implements PsiPackageResolver {
  @Nullable
  @Override
  public PsiPackage resolvePackage(@NotNull PsiPackageManager packageManager, @NotNull VirtualFile virtualFile,
                                   @NotNull Class<? extends ModuleExtension> extensionClass,
                                   String qualifiedName) {

    final Module moduleForFile = ModuleUtil.findModuleForFile(virtualFile, packageManager.getProject());
    if (moduleForFile == null) {
      return null;
    }

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForFile);

    final ModuleExtension extension = moduleRootManager.getExtension(extensionClass);
    if (extension == null) {
      return null;
    }

    PsiManager psiManager = PsiManager.getInstance(packageManager.getProject());
    for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
      if (p.getSupportedModuleExtensionClass() == extensionClass) {
        return p.createPackage(psiManager, packageManager, extensionClass, qualifiedName);
      }
    }
    return null;
  }
}
