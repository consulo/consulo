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
package consulo.packageDependencies;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import consulo.psi.search.scope.ResourcesScope;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 18:05/17.06.13
 */
public class ResourceScopeProvider extends CustomScopesProviderEx {
  private ResourcesScope myResourcesScope = new ResourcesScope();

  public static ResourceScopeProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtensionOrFail(project, ResourceScopeProvider.class);
  }

  @Override
  @Nonnull
  public List<NamedScope> getCustomScopes() {
    return Collections.<NamedScope>singletonList(myResourcesScope);
  }
}
