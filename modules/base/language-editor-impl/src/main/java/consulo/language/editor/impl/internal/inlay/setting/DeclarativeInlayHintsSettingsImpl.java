// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.setting;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponentWithModificationTracker;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.editor.inlay.DeclarativeInlayHintsSettings;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@State(name = "DeclarativeInlayHintsSettings", storages = @Storage("editor.xml"))
@ServiceImpl
@Singleton
public class DeclarativeInlayHintsSettingsImpl
    implements DeclarativeInlayHintsSettings, PersistentStateComponentWithModificationTracker<DeclarativeInlayHintsSettingsState> {
    private long myModificationCount;

    private DeclarativeInlayHintsSettingsState myState = new DeclarativeInlayHintsSettingsState();

    @Override
    public long getStateModificationCount() {
        return myModificationCount;
    }

    @Override
    public DeclarativeInlayHintsSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(DeclarativeInlayHintsSettingsState state) {
        myState = state;
    }

    @Override
    @RequiredReadAction
    public @Nullable Boolean isOptionEnabled(String optionId, String providerId) {
        String serializedId = getSerializedId(providerId, optionId);
        return getState().getEnabledOptions().get(serializedId);
    }

    private static String getSerializedId(String providerId, String optionId) {
        return providerId + "#" + optionId;
    }

    @Override
    public void setOptionEnabled(String optionId, String providerId, boolean isEnabled) {
        String serializedId = getSerializedId(providerId, optionId);
        Boolean previous = getState().getEnabledOptions().put(serializedId, isEnabled);
        if (previous == null || previous != isEnabled) {
            myModificationCount++;
        }
    }

    @Override
    @RequiredReadAction
    public @Nullable Boolean isProviderEnabled(String providerId) {
        return getState().getProviderIdToEnabled().get(providerId);
    }

    @Override
    public void setProviderEnabled(String providerId, boolean isEnabled) {
        Boolean previous = getState().getProviderIdToEnabled().put(providerId, isEnabled);
        if (previous == null || !Objects.equals(previous, isEnabled)) {
            myModificationCount++;
        }
    }
}
