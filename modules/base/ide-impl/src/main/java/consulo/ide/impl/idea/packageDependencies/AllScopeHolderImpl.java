/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.packageDependencies;

import consulo.annotation.component.ServiceImpl;
import consulo.content.internal.scope.AllScopeHolder;
import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.ide.impl.psi.search.scope.packageSet.FilePatternPackageSet;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

@Singleton
@ServiceImpl
public class AllScopeHolderImpl implements AllScopeHolder {
  @Nonnull
  private static final String TEXT = FilePatternPackageSet.SCOPE_FILE + ":*//*";

  @Nonnull
  public static final NamedScope ALL = new NamedScope("All", LocalizeValue.localizeTODO("All"), new AbstractPackageSet(TEXT, 0) {
    @Override
    public boolean contains(final VirtualFile file, Project project, NamedScopesHolder scopesHolder) {
      return true;
    }
  });

  @Nonnull
  @Override
  public NamedScope getAllScope() {
    return ALL;
  }
}
