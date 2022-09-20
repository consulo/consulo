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

/*
 * User: anna
 * Date: 06-Apr-2007
 */
package consulo.content.internal.scope;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.content.scope.NamedScope;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

@ExtensionAPI(ComponentScope.PROJECT)
public interface CustomScopesProvider {
  ExtensionPointName<CustomScopesProvider> CUSTOM_SCOPES_PROVIDER = ExtensionPointName.create(CustomScopesProvider.class);

  void acceptScopes(@Nonnull Consumer<NamedScope> consumer);

  @Nullable
  default NamedScope getCustomScope(String name) {
    SimpleReference<NamedScope> ref = SimpleReference.create();
    acceptScopes(namedScope -> {
      if (Objects.equals(namedScope.getName(), name)) {
        ref.setIfNull(namedScope);
      }
    });
    return ref.get();
  }
}