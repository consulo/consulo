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
package consulo.sandboxPlugin.ide.module;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.impl.psi.PsiPackageBase;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.language.psi.PsiPackageSupportProvider;
import consulo.module.extension.ModuleExtension;
import consulo.sandboxPlugin.ide.module.extension.Sand2ModuleExtension;
import consulo.sandboxPlugin.lang.SandLanguage;

import jakarta.annotation.Nonnull;
import java.util.function.IntFunction;

/**
 * @author VISTALL
 * @since 30.08.14
 */
@ExtensionImpl
public class Sand2PackageProvider implements PsiPackageSupportProvider {
  @Override
  public boolean isSupported(@Nonnull ModuleExtension<?> moduleExtension) {
    return moduleExtension instanceof Sand2ModuleExtension;
  }

  @Nonnull
  @Override
  public PsiPackage createPackage(@Nonnull PsiManager psiManager,
                                  @Nonnull PsiPackageManager packageManager,
                                  @Nonnull Class<? extends ModuleExtension> extensionClass,
                                  @Nonnull String packageName) {
    return new PsiPackageBase(psiManager, packageManager, extensionClass, packageName) {
      @Override
      protected IntFunction<? extends PsiPackage[]> getPackageArrayFactory() {
        return PsiPackage.ARRAY_FACTORY;
      }

      @RequiredReadAction
      @Nonnull
      @Override
      public Language getLanguage() {
        return SandLanguage.INSTANCE;
      }
    };
  }
}
