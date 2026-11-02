/*
 * Copyright 2013-2021 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.externalService.impl.internal.pluginHistory;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.Storage;
import consulo.component.persist.State;
import consulo.container.plugin.PluginId;
import consulo.util.lang.VersionComparatorUtil;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author VISTALL
 * @since 2021-11-21
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "UpdateHistory", storages = @Storage("updateHistory"))
public class UpdateHistory implements PersistentStateComponent<UpdateHistory.State> {
    /**
     * Kept with the settings of the user, so a changelog read on one machine is not shown again on another.
     */
    public static class State {
        /**
         * Plugin id to the last version whose changelog the user was shown.
         */
        public Map<String, String> seenVersions = new TreeMap<>();

        /**
         * Plugin id to the version the last shown changelog started from.
         */
        public Map<String, String> changeLogFrom = new TreeMap<>();

        public boolean showExperimentalWarning = true;
    }

    private State myState = new State();

    /**
     * Moves what the user has seen up to the versions running now.
     *
     * @return {@code true} if anything running is newer than what was seen - the changelog of that is worth showing.
     * A version seen for the first time - a fresh install, a plugin just added - is only taken note of.
     */
    public boolean advanceSeenVersions(Map<String, String> running) {
        Map<String, String> seen = myState.seenVersions;

        boolean newer = false;
        Map<String, String> from = new TreeMap<>();
        for (Map.Entry<String, String> entry : running.entrySet()) {
            String seenVersion = seen.get(entry.getKey());
            from.put(entry.getKey(), seenVersion == null ? entry.getValue() : seenVersion);
            if (seenVersion != null && VersionComparatorUtil.compare(entry.getValue(), seenVersion) > 0) {
                newer = true;
            }
        }

        for (Map.Entry<String, String> entry : running.entrySet()) {
            String seenVersion = seen.get(entry.getKey());
            if (seenVersion == null || VersionComparatorUtil.compare(entry.getValue(), seenVersion) > 0) {
                seen.put(entry.getKey(), entry.getValue());
            }
        }

        if (newer) {
            myState.changeLogFrom = from;
        }
        return newer;
    }

    public void setShowExperimentalWarning(boolean value) {
        myState.showExperimentalWarning = value;
    }

    public boolean isShowExperimentalWarning() {
        return myState.showExperimentalWarning;
    }

    /**
     * The version the changelog of the plugin starts from - the one seen before the last changelog was shown.
     */
    public String getHistoryVersion(PluginId pluginId, String defaultVersion) {
        return myState.changeLogFrom.getOrDefault(pluginId.getIdString(), defaultVersion);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }
}
