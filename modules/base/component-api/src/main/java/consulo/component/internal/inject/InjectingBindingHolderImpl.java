/*
 * Copyright 2013-2022 consulo.io
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
package consulo.component.internal.inject;

import consulo.component.bind.InjectingBinding;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public class InjectingBindingHolderImpl implements InjectingBindingHolder {
    private final Map<String, List<InjectingBinding>> myBindings = new ConcurrentHashMap<>();
    private final AtomicBoolean myLocked;

    public InjectingBindingHolderImpl(AtomicBoolean locked) {
        myLocked = locked;
    }

    public void clear() {
        myBindings.clear();
    }

    public void addBinding(InjectingBinding binding) {
        if (myLocked.get()) {
            throw new IllegalArgumentException("locked");
        }

        myBindings.computeIfAbsent(binding.getApiClassName(), s -> Lists.newLockFreeCopyOnWriteList()).add(binding);
    }

    /**
     * Removes the bindings of a plugin unloaded at runtime.
     *
     * @return removed bindings by api class name
     */
    public Map<String, List<InjectingBinding>> removeBindings(ClassLoader classLoader) {
        Map<String, List<InjectingBinding>> removed = new HashMap<>();
        for (Map.Entry<String, List<InjectingBinding>> entry : myBindings.entrySet()) {
            for (InjectingBinding binding : entry.getValue()) {
                if (binding.getImplClass().getClassLoader() == classLoader) {
                    removed.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(binding);
                }
            }
        }

        for (Map.Entry<String, List<InjectingBinding>> entry : removed.entrySet()) {
            List<InjectingBinding> bindings = myBindings.get(entry.getKey());
            if (bindings != null) {
                bindings.removeAll(entry.getValue());
                if (bindings.isEmpty()) {
                    myBindings.remove(entry.getKey(), bindings);
                }
            }
        }
        return removed;
    }

    @Override
    public Map<String, List<InjectingBinding>> getBindings() {
        return Collections.unmodifiableMap(myBindings);
    }

    public static boolean isValid(InjectingBinding binding, int componentProfiles) {
        int bindingComponentProfiles = binding.getComponentProfiles();
        if (bindingComponentProfiles == 0) {
            return true;
        }
        return (componentProfiles & bindingComponentProfiles) == bindingComponentProfiles;
    }

    public static @Nullable InjectingBinding findValid(Collection<InjectingBinding> bindings, int componentProfiles) {
        InjectingBinding result = null;
        for (InjectingBinding binding : bindings) {
            if (isValid(binding, componentProfiles)) {
                // do not allow override services
                if (result != null) {
                    return null;
                }
                result = binding;
            }
        }

        return result;
    }
}
