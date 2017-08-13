/*
 * Copyright 2013-2016 consulo.io
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
package consulo.psi;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import consulo.module.extension.ModuleExtension;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 8:21/20.05.13
 */
public interface PsiPackageSupportProvider {
  ExtensionPointName<PsiPackageSupportProvider> EP_NAME = ExtensionPointName.create("com.intellij.psi.packageSupportProvider");

  boolean isSupported(@NotNull ModuleExtension<?> moduleExtension);

  default boolean isValidPackageName(@NotNull Module module, @NotNull String packageName) {
    return true;
  }

  default boolean acceptVirtualFile(@NotNull Module module, @NotNull VirtualFile virtualFile) {
    ContentFolderTypeProvider type = ProjectFileIndex.getInstance(module.getProject()).getContentFolderTypeForFile(virtualFile);
    return ProductionContentFolderTypeProvider.getInstance().equals(type) || TestContentFolderTypeProvider.getInstance().equals(type);
  }

  @NotNull
  PsiPackage createPackage(@NotNull PsiManager psiManager,
                           @NotNull PsiPackageManager packageManager,
                           @NotNull Class<? extends ModuleExtension> extensionClass,
                           @NotNull String packageName);
}
