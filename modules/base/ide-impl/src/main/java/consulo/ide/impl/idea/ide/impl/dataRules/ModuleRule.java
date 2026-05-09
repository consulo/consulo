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

import consulo.dataContext.DataSnapshot;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 * @since 2004-02-10
 */
public final class ModuleRule {
  static @Nullable Module getData(DataSnapshot dataProvider) {
    Module moduleContext = dataProvider.get(LangDataKeys.MODULE_CONTEXT);
    if (moduleContext != null) {
      return moduleContext;
    }
    Project project = dataProvider.get(Project.KEY);
    if (project == null) {
      PsiElement element = dataProvider.get(PsiElement.KEY);
      if (element == null || !element.isValid()) return null;
      project = element.getProject();
    }

    VirtualFile virtualFile = dataProvider.get(VirtualFile.KEY);
    if (virtualFile == null) {
      virtualFile = VirtualFileRule.getData(dataProvider);
    }

    if (virtualFile == null) {
      return null;
    }

    return ModuleUtilCore.findModuleForFile(virtualFile, project);
  }
}
