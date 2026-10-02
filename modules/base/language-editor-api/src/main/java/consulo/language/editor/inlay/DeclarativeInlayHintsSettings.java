// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.APPLICATION)
public interface DeclarativeInlayHintsSettings {
    static DeclarativeInlayHintsSettings getInstance() {
        return Application.get().getInstance(DeclarativeInlayHintsSettings.class);
    }

    /**
     * Note that it may return true even if the provider is enabled!
     */
    @RequiredReadAction
    @Nullable Boolean isOptionEnabled(String optionId, String providerId);

    void setOptionEnabled(String optionId, String providerId, boolean isEnabled);

    @RequiredReadAction
    @Nullable Boolean isProviderEnabled(String providerId);

    @RequiredReadAction
    default boolean isProviderEnabled(String providerId, boolean defaultValue) {
        Boolean enabled = isProviderEnabled(providerId);
        return enabled != null ? enabled : defaultValue;
    }

    void setProviderEnabled(String providerId, boolean isEnabled);
}
