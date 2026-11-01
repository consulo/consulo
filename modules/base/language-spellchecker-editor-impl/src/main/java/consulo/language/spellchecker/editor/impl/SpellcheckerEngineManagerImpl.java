/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.spellchecker.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Optional;


/**
 * @author VISTALL
 * @since 2023-03-26
 */
@Singleton
@ServiceImpl
@State(name = "SpellcheckerEngineManager", storages = @Storage("spellchecker"))
public class SpellcheckerEngineManagerImpl implements SpellcheckerEngineManager, PersistentStateComponent<SpellcheckerEngineManagerState> {
    private static final ExtensionPointCacheKey<SpellcheckerEngine, Optional<SpellcheckerEngine>> AUTO_SELECTED_ENGINE =
        ExtensionPointCacheKey.create("AutoSelectedSpellcheckerEngine", walker -> {
            SpellcheckerEngine[] best = new SpellcheckerEngine[1];
            walker.walk(engine -> {
                if (best[0] == null || isMorePowerful(engine, best[0])) {
                    best[0] = engine;
                }
            });
            return Optional.ofNullable(best[0]);
        });

    private final Application myApplication;

    private SpellcheckerEngineManagerState myState = new SpellcheckerEngineManagerState();

    @Inject
    public SpellcheckerEngineManagerImpl(Application application) {
        myApplication = application;
    }

    @Override
    public @Nullable String getSelectedEngineId() {
        return myState.engineId;
    }

    @Override
    public void setSelectedEngineId(@Nullable String engineId) {
        myState.engineId = engineId;
    }

    @Override
    public @Nullable SpellcheckerEngine getActiveEngine() {
        ExtensionPoint<SpellcheckerEngine> point = myApplication.getExtensionPoint(SpellcheckerEngine.class);
        String engineId = myState.engineId;
        if (engineId != null) {
            SpellcheckerEngine engine = point.findFirstSafe(it -> engineId.equals(it.getId()));
            if (engine != null) {
                return engine;
            }
        }
        return getAutoSelectedEngine();
    }

    @Override
    public @Nullable SpellcheckerEngine getAutoSelectedEngine() {
        return myApplication.getExtensionPoint(SpellcheckerEngine.class).getOrBuildCache(AUTO_SELECTED_ENGINE).orElse(null);
    }

    private static boolean isMorePowerful(SpellcheckerEngine engine, SpellcheckerEngine than) {
        int features = Integer.compare(engine.getFeatures().size(), than.getFeatures().size());
        if (features != 0) {
            return features > 0;
        }
        return engine.getLocales().size() > than.getLocales().size();
    }

    @Override
    public SpellcheckerEngineManagerState getState() {
        return myState;
    }

    @Override
    public void loadState(SpellcheckerEngineManagerState state) {
        myState = state;
    }
}
