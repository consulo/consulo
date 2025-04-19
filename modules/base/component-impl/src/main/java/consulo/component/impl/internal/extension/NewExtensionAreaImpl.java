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
package consulo.component.impl.internal.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.ComponentManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionPoint;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingBindingHolder;
import consulo.component.internal.inject.InjectingBindingLoader;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 17-Jun-22
 */
public class NewExtensionAreaImpl {
    private final Map<String, NewExtensionPointImpl> myExtensionPoints = new ConcurrentHashMap<>();

    private final ComponentManager myComponentManager;
    private final ComponentScope myComponentScope;
    private final Runnable myCheckCanceled;
    private final InjectingBindingLoader myInjectingBindingLoader;

    public NewExtensionAreaImpl(
        ComponentManager componentManager,
        ComponentBinding componentBinding,
        ComponentScope componentScope,
        Runnable checkCanceled
    ) {
        myComponentManager = componentManager;
        myComponentScope = componentScope;
        myCheckCanceled = checkCanceled;
        myInjectingBindingLoader = componentBinding.injectingBindingLoader();
    }

    public void registerFromInjectingBinding(ComponentScope componentScope) {
        InjectingBindingHolder holder = myInjectingBindingLoader.getHolder(ExtensionAPI.class, myComponentScope);

        for (Map.Entry<String, List<InjectingBinding>> entry : holder.getBindings().entrySet()) {
            myExtensionPoints.put(
                entry.getKey(),
                new NewExtensionPointImpl(entry.getKey(), entry.getValue(), myComponentManager, myCheckCanceled, componentScope)
            );
        }
    }

    @Nonnull
    public Collection<? extends ExtensionPoint> getExtensionPoints() {
        return myExtensionPoints.values();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <T> ExtensionPoint<T> getExtensionPoint(@Nonnull Class<T> extensionClass) {
        NewExtensionPointImpl point = myExtensionPoints.computeIfAbsent(extensionClass.getName(), e -> {
            if (!extensionClass.isAnnotationPresent(ExtensionAPI.class)) {
                throw new IllegalArgumentException(extensionClass.getName() + " is not annotated by @ExtensionAPI");
            }

            return new NewExtensionPointImpl(e, List.of(), myComponentManager, myCheckCanceled, myComponentScope);
        });

        point.initIfNeed(extensionClass);
        return point;
    }
}
