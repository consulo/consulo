package org.consulo.psi.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;
import org.consulo.psi.PsiPackageResolver;
import org.consulo.psi.PsiPackageSupportProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    ProjectFileIndex fileIndexFacade = ProjectFileIndex.SERVICE.getInstance(packageManager.getProject());
    PsiManager psiManager = PsiManager.getInstance(packageManager.getProject());
    if(fileIndexFacade.isInLibraryClasses(virtualFile)) {

      List<OrderEntry> orderEntriesForFile = fileIndexFacade.getOrderEntriesForFile(virtualFile);
      for (OrderEntry orderEntry : orderEntriesForFile) {
        Module ownerModule = orderEntry.getOwnerModule();
        ModuleExtension extension = ModuleUtilCore.getExtension(ownerModule, extensionClass);
        if(extension != null) {
          for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
            if (p.isSupported(extension)) {
              return p.createPackage(psiManager, packageManager, extensionClass, qualifiedName);
            }
          }
        }
      }
    }
    return null;
  }
}
