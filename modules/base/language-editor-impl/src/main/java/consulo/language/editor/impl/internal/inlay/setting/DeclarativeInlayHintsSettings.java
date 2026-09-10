// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponentWithModificationTracker;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.Objects;

@State(name = "DeclarativeInlayHintsSettings", storages = @Storage("editor.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class DeclarativeInlayHintsSettings implements PersistentStateComponentWithModificationTracker<DeclarativeInlayHintsSettingsState> {

    public static DeclarativeInlayHintsSettings getInstance() {
        return ApplicationManager.getApplication()
            .getInstance(DeclarativeInlayHintsSettings.class);
    }

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

    public Boolean isOptionEnabled(String optionId, String providerId) {
        String serializedId = getSerializedId(providerId, optionId);
        return getState().getEnabledOptions().get(serializedId);
    }

    private static String getSerializedId(String providerId, String optionId) {
        return providerId + "#" + optionId;
    }

    public void setOptionEnabled(String optionId,
                                 String providerId,
                                 boolean isEnabled) {
        String serializedId = getSerializedId(providerId, optionId);
        Boolean previous = getState().getEnabledOptions().put(serializedId, isEnabled);
        if (previous == null || previous != isEnabled) {
            myModificationCount++;
        }
    }

    public @Nullable Boolean isProviderEnabled(String providerId) {
        return getState().getProviderIdToEnabled().get(providerId);
    }

    public void setProviderEnabled(String providerId, boolean isEnabled) {
        Boolean previous = getState().getProviderIdToEnabled().put(providerId, isEnabled);
        if (previous == null || !Objects.equals(previous, isEnabled)) {
            myModificationCount++;
        }
    }
}
