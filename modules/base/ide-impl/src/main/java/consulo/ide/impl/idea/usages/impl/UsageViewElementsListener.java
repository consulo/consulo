// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.usages.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.usage.Usage;
import consulo.usage.UsageView;
import javax.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface UsageViewElementsListener {
  ExtensionPointName<UsageViewElementsListener> EP_NAME = ExtensionPointName.create(UsageViewElementsListener.class);

  default void beforeUsageAdded(@Nonnull UsageView view, @Nonnull Usage usage) {
  }

  default boolean isExcludedByDefault(@Nonnull UsageView view, @Nonnull Usage usage) {
    return false;
  }
}
