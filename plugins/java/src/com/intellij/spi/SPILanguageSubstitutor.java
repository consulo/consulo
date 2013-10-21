/*
 * Copyright 2013 Consulo.org
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
package com.intellij.spi;

import com.intellij.lang.Language;
import com.intellij.lang.spi.SPILanguage;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import org.consulo.java.platform.roots.SpecialDirUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 19:49/05.07.13
 */
public class SPILanguageSubstitutor extends LanguageSubstitutor {
  private static final ExtensionPointName<Condition<VirtualFile>> VETO_EP_NAME =
    ExtensionPointName.create("org.consulo.java.vetoSPICondition");

  @Nullable
  @Override
  public Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
    final Module moduleForPsiElement = ModuleUtilCore.findModuleForFile(file, project);
    if (moduleForPsiElement == null) {
      return null;
    }

    final VirtualFile parent = file.getParent();
    if (parent != null && "services".equals(parent.getName())) {
      final VirtualFile gParent = parent.getParent();
      if (gParent != null && SpecialDirUtil.META_INF.equals(gParent.getName())) {
        final List<VirtualFile> virtualFiles = SpecialDirUtil.collectSpecialDirs(moduleForPsiElement, SpecialDirUtil.META_INF);
        if(!virtualFiles.contains(gParent)) {
          return null;
        }

        for (Condition<VirtualFile> condition : VETO_EP_NAME.getExtensions()) {
          if (condition.value(file)) {
            return null;
          }
        }
        return SPILanguage.INSTANCE;
      }
    }
    return null;
  }
}
