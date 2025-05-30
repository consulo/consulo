// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponentWithModificationTracker;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@State(
    name = "DeclarativeInlayHintsSettings",
    storages = {@Storage("editor.xml")}
)
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class DeclarativeInlayHintsSettings implements PersistentStateComponentWithModificationTracker<DeclarativeInlayHintsSettings.HintsState> {

    public static class HintsState {
        private Map<String, Boolean> enabledOptions = new HashMap<>();
        private Map<String, Boolean> providerIdToEnabled = new HashMap<>();

        @Nonnull
        public Map<String, Boolean> getEnabledOptions() {
            return enabledOptions;
        }

        public void setEnabledOptions(@Nonnull Map<String, Boolean> options) {
            this.enabledOptions = options;
        }

        @Nonnull
        public Map<String, Boolean> getProviderIdToEnabled() {
            return providerIdToEnabled;
        }

        public void setProviderIdToEnabled(@Nonnull Map<String, Boolean> mapping) {
            this.providerIdToEnabled = mapping;
        }
    }

    public static DeclarativeInlayHintsSettings getInstance() {
        return ApplicationManager.getApplication()
            .getService(DeclarativeInlayHintsSettings.class);
    }

    private long myModificationCount;

    private HintsState myHintsState = new HintsState();


    @Override
    public long getStateModificationCount() {
        return myModificationCount;
    }

    @Nullable
    @Override
    public HintsState getState() {
        return myHintsState;
    }

    @Override
    public void loadState(HintsState state) {
        XmlSerializerUtil.copyBean(state, myHintsState);
    }

    @RequiredReadAction
    public Boolean isOptionEnabled(@Nonnull String optionId, @Nonnull String providerId) {
        String serializedId = getSerializedId(providerId, optionId);
        return getState().getEnabledOptions().get(serializedId);
    }

    private static String getSerializedId(@Nonnull String providerId, @Nonnull String optionId) {
        return providerId + "#" + optionId;
    }

    @RequiredWriteAction
    public void setOptionEnabled(@Nonnull String optionId,
                                 @Nonnull String providerId,
                                 boolean isEnabled) {
        String serializedId = getSerializedId(providerId, optionId);
        Boolean previous = getState().getEnabledOptions().put(serializedId, isEnabled);
        if (previous == null || previous != isEnabled) {
            myModificationCount++;
        }
    }

    @RequiredReadAction
    public Boolean isProviderEnabled(@Nonnull String providerId) {
        return getState().getProviderIdToEnabled().getOrDefault(providerId, false);
    }

    @RequiredWriteAction
    public void setProviderEnabled(@Nonnull String providerId, boolean isEnabled) {
        Boolean previous = getState().getProviderIdToEnabled().put(providerId, isEnabled);
        if (previous == null || previous != isEnabled) {
            myModificationCount++;
        }
    }
}
