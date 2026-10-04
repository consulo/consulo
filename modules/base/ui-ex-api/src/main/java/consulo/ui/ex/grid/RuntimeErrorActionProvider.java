// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface RuntimeErrorActionProvider {
    static List<RuntimeErrorActionProvider> getProviders() {
        return Application.get().getExtensionList(RuntimeErrorActionProvider.class);
    }

    ErrorInfo.@Nullable Fix createAction(ErrorInfo error);
}
