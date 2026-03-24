// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Application-level persistent settings for the Code Vision feature.
 * <p>
 * Mirrors JB's {@code CodeVisionSettings.kt}.
 */
@State(name = "CodeVisionSettings", storages = @Storage("editor.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class CodeVisionSettings implements PersistentStateComponent<CodeVisionSettings.State> {

    public static CodeVisionSettings getInstance() {
        return Application.get().getInstance(CodeVisionSettings.class);
    }

    public static class State {
        public boolean isEnabled = true;

        /** Serialised name of the default {@link CodeVisionAnchorKind}. {@code null} → use {@link CodeVisionAnchorKind#Top}. */
        public @Nullable String defaultPosition = null;

        public int visibleMetricsAboveDeclarationCount = 5;
        public int visibleMetricsNextToDeclarationCount = 5;

        /**
         * Providers that have been explicitly disabled by the user.
         * Use {@link CodeVisionSettings#isProviderEnabled} rather than reading this field directly.
         */
        public TreeSet<String> disabledCodeVisionProviderIds = new TreeSet<>();

        /**
         * Providers that have been explicitly enabled by the user (relevant when a provider
         * defaults to disabled). Not used in Consulo yet (all providers default to enabled),
         * but kept for state-format compatibility with JB.
         */
        public TreeSet<String> enabledCodeVisionProviderIds = new TreeSet<>();

        public Map<String, String> codeVisionGroupToPosition = new HashMap<>();
    }

    private State myState = new State();

    // -------------------------------------------------------------------------
    // Global enable/disable
    // -------------------------------------------------------------------------

    public boolean isCodeVisionEnabled() {
        return myState.isEnabled;
    }

    public void setCodeVisionEnabled(boolean enabled) {
        myState.isEnabled = enabled;
        listener().globalEnabledChanged(enabled);
    }

    // -------------------------------------------------------------------------
    // Default position
    // -------------------------------------------------------------------------

    public CodeVisionAnchorKind getDefaultPosition() {
        if (myState.defaultPosition == null) return CodeVisionAnchorKind.Top;
        try {
            return CodeVisionAnchorKind.valueOf(myState.defaultPosition);
        }
        catch (IllegalArgumentException e) {
            return CodeVisionAnchorKind.Top;
        }
    }

    public void setDefaultPosition(CodeVisionAnchorKind position) {
        myState.defaultPosition = position.name();
        listener().defaultPositionChanged(position);
    }

    // -------------------------------------------------------------------------
    // Visible metrics counts
    // -------------------------------------------------------------------------

    public int getVisibleMetricsAboveDeclarationCount() {
        return myState.visibleMetricsAboveDeclarationCount;
    }

    public void setVisibleMetricsAboveDeclarationCount(int count) {
        myState.visibleMetricsAboveDeclarationCount = count;
        listener().visibleMetricsAboveDeclarationCountChanged(count);
    }

    public int getVisibleMetricsNextToDeclarationCount() {
        return myState.visibleMetricsNextToDeclarationCount;
    }

    public void setVisibleMetricsNextToDeclarationCount(int count) {
        myState.visibleMetricsNextToDeclarationCount = count;
        listener().visibleMetricsNextToDeclarationCountChanged(count);
    }

    // -------------------------------------------------------------------------
    // Per-group position
    // -------------------------------------------------------------------------

    public @Nullable CodeVisionAnchorKind getPositionForGroup(String groupName) {
        String pos = myState.codeVisionGroupToPosition.get(groupName);
        if (pos == null) return null;
        try {
            return CodeVisionAnchorKind.valueOf(pos);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setPositionForGroup(String groupName, CodeVisionAnchorKind position) {
        myState.codeVisionGroupToPosition.put(groupName, position.name());
        listener().groupPositionChanged(groupName, position);
    }

    // -------------------------------------------------------------------------
    // Per-provider enable/disable
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given provider is enabled.
     * All providers are considered enabled by default in Consulo
     * (no equivalent of JB's {@code CodeVisionSettingsDefaults}).
     */
    public boolean isProviderEnabled(String providerId) {
        return !myState.disabledCodeVisionProviderIds.contains(providerId);
    }

    public void setProviderEnabled(String providerId, boolean enabled) {
        if (enabled) {
            myState.disabledCodeVisionProviderIds.remove(providerId);
        }
        else {
            myState.disabledCodeVisionProviderIds.add(providerId);
        }
        listener().providerAvailabilityChanged(providerId, enabled);
    }

    // -------------------------------------------------------------------------
    // Anchor limit helpers (mirrors JB getAnchorLimit / setAnchorLimit)
    // -------------------------------------------------------------------------

    public int getAnchorLimit(CodeVisionAnchorKind position) {
        return switch (position) {
            case Top -> getVisibleMetricsAboveDeclarationCount();
            case Right -> getVisibleMetricsNextToDeclarationCount();
            default -> getAnchorLimit(getDefaultPosition());
        };
    }

    // -------------------------------------------------------------------------
    // PersistentStateComponent
    // -------------------------------------------------------------------------

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    // -------------------------------------------------------------------------
    // Listener
    // -------------------------------------------------------------------------

    private static CodeVisionSettingsListener listener() {
        return Application.get().getMessageBus().syncPublisher(CodeVisionSettingsListener.class);
    }

}
