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

package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.language.editor.LangDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.module.Module;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.dataContext.BaseDataManager;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 *         Date: Feb 10, 2004
 */
@ExtensionImpl
public class ModuleRule implements GetDataRule<Module> {
  @Nonnull
  @Override
  public Key<Module> getKey() {
    return Module.KEY;
  }

  @Override
  public Module getData(@Nonnull DataProvider dataProvider) {
    Module moduleContext = dataProvider.getDataUnchecked(LangDataKeys.MODULE_CONTEXT);
    if (moduleContext != null) {
      return moduleContext;
    }
    Project project = dataProvider.getDataUnchecked(Project.KEY);
    if (project == null) {
      PsiElement element = dataProvider.getDataUnchecked(PsiElement.KEY);
      if (element == null || !element.isValid()) return null;
      project = element.getProject();
    }

    VirtualFile virtualFile = dataProvider.getDataUnchecked(VirtualFile.KEY);
    if (virtualFile == null) {
      GetDataRule<VirtualFile> dataRule = ((BaseDataManager)DataManager.getInstance()).getDataRule(VirtualFile.KEY);
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
