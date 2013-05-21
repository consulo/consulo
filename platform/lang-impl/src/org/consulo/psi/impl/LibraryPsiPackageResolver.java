package org.consulo.psi.impl;

import com.intellij.openapi.roots.FileIndexFacade;
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
 * @since 16:57/21.05.13
 */
public class LibraryPsiPackageResolver implements PsiPackageResolver {
  @Nullable
  @Override
  public PsiPackage resolvePackage(@NotNull PsiPackageManager packageManager, @NotNull VirtualFile virtualFile,
                                   @NotNull Class<? extends ModuleExtension> extensionClass,
                                   String qualifiedName) {
    FileIndexFacade fileIndexFacade = FileIndexFacade.getInstance(packageManager.getProject());
    if(fileIndexFacade.isInLibraryClasses(virtualFile)) {

      PsiManager psiManager = PsiManager.getInstance(packageManager.getProject());
      for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
        if (p.getSupportedModuleExtensionClass() == extensionClass) {
          return p.createPackage(psiManager, packageManager, extensionClass, qualifiedName);
        }
      }
    }
    return null;
  }
}
