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
package consulo.ide.impl.idea.packageDependencies;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.internal.scope.CustomScopesProvider;
import consulo.content.scope.NamedScope;
import consulo.ide.impl.idea.ide.scratch.ScratchesNamedScope;
import consulo.ide.impl.psi.search.scope.ProjectFilesScope;
import consulo.language.editor.scope.NonProjectFilesScope;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class DefaultScopesProvider implements CustomScopesProvider {
  private final List<NamedScope> myScopes;

  public static DefaultScopesProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtensionOrFail(project, DefaultScopesProvider.class);
  }

  @Inject
  public DefaultScopesProvider(Project project) {
      myScopes = List.of(new ProjectFilesScope(), getAllScope(), new NonProjectFilesScope(), new ScratchesNamedScope());
  }

  @Nullable
  @Override
  public NamedScope getCustomScope(String name) {
    for (NamedScope scope : myScopes) {
      if (Objects.equals(scope.getName(), name)) {
        return scope;
      }
    }
    return null;
  }

  @Override
  public void acceptScopes(@Nonnull Consumer<NamedScope> consumer) {
    myScopes.forEach(consumer);
  }

  public static NamedScope getAllScope() {
    return AllScopeHolderImpl.ALL;
  }
}
