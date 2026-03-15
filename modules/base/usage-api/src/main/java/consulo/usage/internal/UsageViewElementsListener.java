// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.usage.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.usage.Usage;
import consulo.usage.UsageView;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface UsageViewElementsListener {
    ExtensionPointName<UsageViewElementsListener> EP_NAME = ExtensionPointName.create(UsageViewElementsListener.class);

    default void beforeUsageAdded(UsageView view, Usage usage) {
    }

    default boolean isExcludedByDefault(UsageView view, Usage usage) {
        return false;
    }
}
