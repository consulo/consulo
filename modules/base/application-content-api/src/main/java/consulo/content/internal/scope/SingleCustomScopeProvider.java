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
package consulo.content.internal.scope;

import consulo.content.scope.NamedScope;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 20-Sep-22
 */
public abstract class SingleCustomScopeProvider implements CustomScopesProvider {
  private final NamedScope myScope;

  public SingleCustomScopeProvider(NamedScope scope) {
    myScope = scope;
  }

  @Override
  public void acceptScopes(@Nonnull Consumer<NamedScope> consumer) {
    consumer.accept(myScope);
  }

  @Nullable
  @Override
  public NamedScope getCustomScope(String name) {
    if (Objects.equals(myScope.getName(), name)) {
      return myScope;
    }
    return null;
  }
}
