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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author VISTALL
 * @since 2026-11-17
 */
public class CompilerModuleStateRegistryTest {
    private final Set<UUID> myLoadedIds = new HashSet<>();

    private final CompilerModuleStateRegistry myRegistry = new CompilerModuleStateRegistry(myLoadedIds::contains);

    @Test
    public void renameKeepsState() {
        TestModule module = module("core");

        myRegistry.getOrCreate(module.get()).inherit = false;

        module.rename("kernel");

        CompilerManagerModuleState state = myRegistry.get(module.get());
        assertNotNull(state);
        assertFalse(state.inherit);
        assertNull(state.name);
        assertEquals(module.get().getId().toString(), state.id);
        assertEquals(1, myRegistry.getAll().size());
    }

    @Test
    public void swappedNamesKeepTheirStates() {
        TestModule first = module("a");
        TestModule second = module("b");

        myRegistry.getOrCreate(first.get()).exclude = false;

        first.rename("b");
        second.rename("a");

        assertFalse(myRegistry.getOrCreate(first.get()).exclude);
        assertTrue(myRegistry.getOrCreate(second.get()).exclude);
    }

    @Test
    public void legacyStateIsMatchedByNameAndKeepsOnlyTheId() {
        myRegistry.load(List.of(state("core", null)));

        TestModule module = module("core");

        CompilerManagerModuleState state = myRegistry.get(module.get());
        assertNotNull(state);
        assertEquals(module.get().getId().toString(), state.id);
        assertNull(state.name);
        assertSame(state, myRegistry.get(module.get()));
        assertEquals(1, myRegistry.getAll().size());
    }

    @Test
    public void stateIsMatchedByIdBeforeName() {
        TestModule module = module("renamed-while-closed");
        TestModule other = module("core");

        myRegistry.load(List.of(state("core", module.get().getId().toString())));

        assertNull(myRegistry.get(other.get()));
        CompilerManagerModuleState state = myRegistry.get(module.get());
        assertNotNull(state);
        assertNull(state.name);
    }

    @Test
    public void staleIdFallsBackToName() {
        myRegistry.load(List.of(state("core", UUID.randomUUID().toString())));

        TestModule module = module("core");

        CompilerManagerModuleState state = myRegistry.get(module.get());
        assertNotNull(state);
        assertEquals(module.get().getId().toString(), state.id);
    }

    @Test
    public void unmatchedStatesAreKeptAndSorted() {
        String storedId = "00000000-0000-0000-0000-000000000001";
        myRegistry.load(List.of(state(null, storedId), state("zeta", null), state("alpha", null)));

        TestModule module = module("beta");
        CompilerManagerModuleState created = myRegistry.getOrCreate(module.get());
        assertNull(created.name);
        assertEquals(module.get().getId().toString(), created.id);

        List<String> keys = myRegistry.getAll().stream().map(it -> it.name != null ? it.name : it.id).toList();
        List<String> expected = new ArrayList<>(List.of("alpha", "zeta"));
        expected.addAll(Stream.of(storedId, created.id).sorted().toList());
        assertEquals(expected, keys);
    }

    @Test
    public void removedModuleDropsState() {
        TestModule module = module("core");
        myRegistry.getOrCreate(module.get());

        myRegistry.moduleRemoved(module.get());

        assertNull(myRegistry.get(module.get()));
        assertTrue(myRegistry.getAll().isEmpty());
    }

    private TestModule module(String name) {
        TestModule module = new TestModule(name);
        myLoadedIds.add(module.get().getId());
        return module;
    }

    private static CompilerManagerModuleState state(@Nullable String name, @Nullable String id) {
        CompilerManagerModuleState state = new CompilerManagerModuleState();
        state.name = name;
        state.id = id;
        return state;
    }

    private static final class TestModule {
        private final AtomicReference<String> myName;
        private final Module myModule;

        private TestModule(String name) {
            myName = new AtomicReference<>(name);
            UUID id = UUID.randomUUID();
            Class<?>[] interfaces = {Module.class};
            myModule = (Module) Proxy.newProxyInstance(Module.class.getClassLoader(), interfaces, (proxy, method, args) ->
                switch (method.getName()) {
                    case "getId" -> id;
                    case "getName" -> myName.get();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Module: '" + myName.get() + "'";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        }

        private Module get() {
            return myModule;
        }

        private void rename(String name) {
            myName.set(name);
        }
    }
}
