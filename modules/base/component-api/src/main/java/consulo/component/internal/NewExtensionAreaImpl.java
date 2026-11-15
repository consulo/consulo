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
package consulo.component.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.ComponentManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionExtender;
import consulo.component.extension.ExtensionPoint;
import consulo.component.internal.inject.InjectingBindingHolder;
import consulo.component.internal.inject.InjectingBindingHolderImpl;
import consulo.component.internal.inject.InjectingBindingLoader;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2022-06-17
 */
public class NewExtensionAreaImpl {
    private final Map<String, NewExtensionPointImpl> myExtensionPoints = new ConcurrentHashMap<>();

    private final ComponentManager myComponentManager;
    private final ComponentScope myComponentScope;
    private final Runnable myCheckCanceled;
    private final Supplier<ComponentManager> myApplicationGetter;
    private final InjectingBindingLoader myInjectingBindingLoader;

    public NewExtensionAreaImpl(
        ComponentManager componentManager,
        ComponentBinding componentBinding,
        ComponentScope componentScope,
        Runnable checkCanceled,
        Supplier<ComponentManager> applicationGetter
    ) {
        myComponentManager = componentManager;
        myComponentScope = componentScope;
        myCheckCanceled = checkCanceled;
        myApplicationGetter = applicationGetter;
        myInjectingBindingLoader = componentBinding.injectingBindingLoader();
    }

    public void registerFromInjectingBinding(ComponentScope componentScope) {
        InjectingBindingHolder holder = myInjectingBindingLoader.getHolder(ExtensionAPI.class, myComponentScope);

        for (Map.Entry<String, List<InjectingBinding>> entry : holder.getBindings().entrySet()) {
            myExtensionPoints.put(
                entry.getKey(),
                new NewExtensionPointImpl(entry.getKey(), entry.getValue(), myComponentManager, myCheckCanceled, componentScope, myApplicationGetter)
            );
        }
    }

    
    /**
     * Rebuilds the extension points of the given api classes from the current bindings, after a plugin added or removed
     * its bindings. Extension points of api classes from the unloaded class loader are dropped.
     */
    public void bindingsChanged(Set<String> apiClassNames, @Nullable ClassLoader unloadedClassLoader) {
        if (unloadedClassLoader != null) {
            myExtensionPoints.values().removeIf(point -> point.isApiClassFrom(unloadedClassLoader));
            myExtensionPoints.values().forEach(NewExtensionPointImpl::dropCaches);
        }

        if (apiClassNames.isEmpty()) {
            return;
        }

        InjectingBindingHolder holder = myInjectingBindingLoader.getHolder(ExtensionAPI.class, myComponentScope);
        Map<String, List<InjectingBinding>> bindings = holder.getBindings();

        Set<String> names = apiClassNames;
        if (apiClassNames.contains(ExtensionExtender.class.getCanonicalName())) {
            names = new HashSet<>(bindings.keySet());
            names.addAll(myExtensionPoints.keySet());
        }
        else {
            // extenders build values from other extensions, which may be the changed ones
            Set<String> extended = new HashSet<>(names);
            myApplicationGetter.get().getExtensionPoint(ExtensionExtender.class)
                .forEachExtensionSafe(extender -> extended.add(extender.getExtensionClass().getCanonicalName()));
            names = extended;
        }

        for (String apiClassName : names) {
            List<InjectingBinding> list = bindings.getOrDefault(apiClassName, List.of());

            NewExtensionPointImpl point = myExtensionPoints.get(apiClassName);
            if (point != null) {
                if (list.isEmpty() && unloadedClassLoader != null && point.isApiClassFrom(unloadedClassLoader)) {
                    myExtensionPoints.remove(apiClassName, point);
                }
                else {
                    point.reset(list);
                }
            }
            else if (!list.isEmpty()) {
                myExtensionPoints.put(
                    apiClassName,
                    new NewExtensionPointImpl(apiClassName, list, myComponentManager, myCheckCanceled, myComponentScope, myApplicationGetter)
                );
            }
        }
    }

    public Collection<? extends ExtensionPoint> getExtensionPoints() {
        return myExtensionPoints.values();
    }

    
    @SuppressWarnings("unchecked")
    public <T> ExtensionPoint<T> getExtensionPoint(Class<T> extensionClass) {
        NewExtensionPointImpl point = myExtensionPoints.computeIfAbsent(extensionClass.getCanonicalName(), e -> {
            if (!extensionClass.isAnnotationPresent(ExtensionAPI.class)) {
                throw new IllegalArgumentException(extensionClass.getName() + " is not annotated by @ExtensionAPI");
            }

            return new NewExtensionPointImpl(e, List.of(), myComponentManager, myCheckCanceled, myComponentScope, myApplicationGetter);
        });

        point.initIfNeed(extensionClass);
        return point;
    }
}
