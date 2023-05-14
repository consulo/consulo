// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.application.util.registry.Registry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SearchTopHitProvider {
  ExtensionPointName<SearchTopHitProvider> EP_NAME = ExtensionPointName.create(SearchTopHitProvider.class);

  void consumeTopHits(@Nonnull String pattern, @Nonnull Consumer<Object> collector, @Nullable Project project);

  static String getTopHitAccelerator() {
    return Registry.is("new.search.everywhere") ? "/" : "#";
  }
}
