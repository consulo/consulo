// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.search.scope.impl;

import consulo.content.internal.scope.CustomScopesProvider;
import consulo.content.internal.scope.CustomScopesProviders;
import consulo.content.scope.NamedScope;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CustomScopesAggregator {
  @Nonnull
  public static List<NamedScope> getAllCustomScopes(@Nonnull Project project) {
    Set<NamedScope> allScopes = new LinkedHashSet<>();
    for (CustomScopesProvider scopesProvider : CustomScopesProvider.CUSTOM_SCOPES_PROVIDER.getExtensionList(project)) {
      CustomScopesProviders.acceptFilteredScopes(scopesProvider, allScopes::add);
    }
    return new ArrayList<>(allScopes);
  }
}
