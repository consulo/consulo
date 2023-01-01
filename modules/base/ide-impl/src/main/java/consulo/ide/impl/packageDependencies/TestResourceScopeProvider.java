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
package consulo.ide.impl.packageDependencies;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.internal.scope.SingleCustomScopeProvider;
import consulo.ide.impl.psi.search.scope.TestResourcesScope;
import consulo.project.Project;

/**
 * @author VISTALL
 * @since 23:43/19.09.13
 */
@ExtensionImpl
public class TestResourceScopeProvider extends SingleCustomScopeProvider {
  public static TestResourceScopeProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtensionOrFail(project, TestResourceScopeProvider.class);
  }

  public TestResourceScopeProvider() {
    super(new TestResourcesScope());
  }
}
