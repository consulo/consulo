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
import consulo.util.lang.ref.SimpleReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 20-Sep-22
 */
public class CustomScopesProviders {
  @Nonnull
  public static List<NamedScope> getFilteredScopes(CustomScopesProvider provider) {
    List<NamedScope> result = new ArrayList<>();
    acceptFilteredScopes(provider, result::add);
    return result;
  }

  public static void acceptFilteredScopes(CustomScopesProvider provider, Consumer<NamedScope> consumer) {
    List<CustomScopesFilter> filters = CustomScopesFilter.EP_NAME.getExtensionList();

    provider.acceptScopes(namedScope -> {
      for (CustomScopesFilter filter : filters) {
        if (filter.excludeScope(namedScope)) return;
      }

      consumer.accept(Objects.requireNonNull(namedScope));
    });
  }

  @Nullable
  public static NamedScope findScopeByName(String name, CustomScopesProvider provider) {
    SimpleReference<NamedScope> ref = SimpleReference.create();
    provider.acceptScopes(namedScope -> {
      if (Objects.equals(namedScope.getName(), name)) {
        ref.setIfNull(namedScope);
      }
    });
    return ref.get();
  }
}
