package org.consulo.psi;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 16:53/21.05.13
 */
public interface PsiPackageResolver {
  ExtensionPointName<PsiPackageResolver> EP_NAME = ExtensionPointName.create("com.intellij.psi.packageResolver");

  @Nullable
  PsiPackage resolvePackage(@NotNull PsiPackageManager packageManager, @NotNull VirtualFile virtualFile,
                            @NotNull Class<? extends ModuleExtension> extensionClass,
                            String qualifiedName);
}
