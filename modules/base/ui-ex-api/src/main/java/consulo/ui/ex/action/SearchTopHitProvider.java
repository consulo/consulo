// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionPointName;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SearchTopHitProvider {
    ExtensionPointName<SearchTopHitProvider> EP_NAME = ExtensionPointName.create(SearchTopHitProvider.class);

    void consumeTopHits(String pattern, Consumer<Object> collector, @Nullable ComponentManager project);

    static String getTopHitAccelerator() {
        return "/";
    }
}
