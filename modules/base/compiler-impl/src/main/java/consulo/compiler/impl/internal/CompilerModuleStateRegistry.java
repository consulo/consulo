/*
 * Copyright 2013-2026 consulo.io
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
package consulo.compiler.impl.internal;

import consulo.module.Module;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Per-module compiler states keyed by {@link Module#getId()}. Stored states are matched to modules lazily, since they can
 * be loaded before the modules: by id, then by name for legacy states stored without an id. A matched state keeps only
 * the id.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public class CompilerModuleStateRegistry {
    private final Map<UUID, CompilerManagerModuleState> myStates = new ConcurrentHashMap<>();

    private final Queue<CompilerManagerModuleState> myUnresolvedStates = new ConcurrentLinkedQueue<>();

    private final Predicate<UUID> myLoadedModuleId;

    public CompilerModuleStateRegistry(Predicate<UUID> loadedModuleId) {
        myLoadedModuleId = loadedModuleId;
    }

    public @Nullable CompilerManagerModuleState get(Module module) {
        CompilerManagerModuleState state = myStates.get(module.getId());
        if (state != null || myUnresolvedStates.isEmpty()) {
            return state;
        }
        return resolve(module);
    }

    public CompilerManagerModuleState getOrCreate(Module module) {
        CompilerManagerModuleState state = get(module);
        return state == null ? create(module) : state;
    }

    public void moduleRemoved(Module module) {
        myStates.remove(module.getId());
    }

    public synchronized void load(List<CompilerManagerModuleState> states) {
        myStates.clear();
        myUnresolvedStates.clear();

        for (CompilerManagerModuleState state : states) {
            if (state.id != null || state.name != null) {
                myUnresolvedStates.add(state);
            }
        }
    }

    public List<CompilerManagerModuleState> getAll() {
        List<CompilerManagerModuleState> states = new ArrayList<>(myStates.values());
        states.addAll(myUnresolvedStates);
        states.sort(Comparator.comparing((CompilerManagerModuleState it) -> it.name, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(it -> it.id, Comparator.nullsFirst(Comparator.naturalOrder())));
        return states;
    }

    private synchronized CompilerManagerModuleState create(Module module) {
        return myStates.computeIfAbsent(module.getId(), id -> {
            CompilerManagerModuleState state = new CompilerManagerModuleState();
            state.id = id.toString();
            return state;
        });
    }

    private synchronized @Nullable CompilerManagerModuleState resolve(Module module) {
        UUID moduleId = module.getId();
        CompilerManagerModuleState state = myStates.get(moduleId);
        if (state != null) {
            return state;
        }

        String id = moduleId.toString();
        CompilerManagerModuleState match = null;
        for (CompilerManagerModuleState candidate : myUnresolvedStates) {
            if (id.equals(candidate.id)) {
                match = candidate;
                break;
            }
        }

        if (match == null) {
            for (CompilerManagerModuleState candidate : myUnresolvedStates) {
                if (candidate.name != null && candidate.name.equals(module.getName()) && !isLoadedModuleId(candidate.id)) {
                    match = candidate;
                    break;
                }
            }
        }

        if (match == null) {
            return null;
        }

        myUnresolvedStates.remove(match);
        match.id = id;
        match.name = null;
        myStates.put(moduleId, match);
        return match;
    }

    private boolean isLoadedModuleId(@Nullable String id) {
        if (id == null) {
            return false;
        }

        try {
            return myLoadedModuleId.test(UUID.fromString(id));
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }
}
