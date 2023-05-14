/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.language.editor.scratch.RootType;
import consulo.language.editor.scratch.ScratchFileService;
import consulo.language.psi.ResolveScopeEnlarger;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene Zhuravlev
 */
@ExtensionImpl
public class ScratchResolveScopeEnlarger extends ResolveScopeEnlarger {
  private final Provider<ScratchFileService> myScratchFileServiceProvider;

  @Inject
  public ScratchResolveScopeEnlarger(Provider<ScratchFileService> scratchFileServiceProvider) {
    myScratchFileServiceProvider = scratchFileServiceProvider;
  }

  @Nullable
  @Override
  public SearchScope getAdditionalResolveScope(@Nonnull VirtualFile file, Project project) {
    ScratchFileService scratchFileService = myScratchFileServiceProvider.get();

    RootType rootType = scratchFileService.getRootType(file);
    return rootType != null && !rootType.isHidden() ? GlobalSearchScope.fileScope(project, file) : null;
  }
}
