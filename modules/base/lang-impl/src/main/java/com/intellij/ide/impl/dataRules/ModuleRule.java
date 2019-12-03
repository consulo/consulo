/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.impl.dataRules;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import consulo.ide.base.BaseDataManager;

import javax.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 *         Date: Feb 10, 2004
 */
public class ModuleRule implements GetDataRule<Module> {
  @Nonnull
  @Override
  public Key<Module> getKey() {
    return CommonDataKeys.MODULE;
  }

  @Override
  public Module getData(@Nonnull DataProvider dataProvider) {
    Module moduleContext = dataProvider.getDataUnchecked(LangDataKeys.MODULE_CONTEXT);
    if (moduleContext != null) {
      return moduleContext;
    }
    Project project = dataProvider.getDataUnchecked(CommonDataKeys.PROJECT);
    if (project == null) {
      PsiElement element = dataProvider.getDataUnchecked(LangDataKeys.PSI_ELEMENT);
      if (element == null || !element.isValid()) return null;
      project = element.getProject();
    }

    VirtualFile virtualFile = dataProvider.getDataUnchecked(PlatformDataKeys.VIRTUAL_FILE);
    if (virtualFile == null) {
      GetDataRule<VirtualFile> dataRule = ((BaseDataManager)DataManager.getInstance()).getDataRule(PlatformDataKeys.VIRTUAL_FILE);
      if (dataRule != null) {
        virtualFile = dataRule.getData(dataProvider);
      }
    }

    if (virtualFile == null) {
      return null;
    }

    return ModuleUtil.findModuleForFile(virtualFile, project);
  }
}
