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

import consulo.annotation.component.*;
import consulo.component.bind.InjectingBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public record InjectingBindingLoader(Map<ComponentScope, InjectingBindingHolder> services,
                                     Map<ComponentScope, InjectingBindingHolder> extensions,
                                     Map<ComponentScope, InjectingBindingHolder> topics,
                                     InjectingBindingHolder actions) {

    public InjectingBindingHolder getHolder(Class<?> annotationClass, ComponentScope componentScope) {
        if (annotationClass == ServiceAPI.class) {
            return services().getOrDefault(componentScope, InjectingBindingHolder.EMPTY);
        }
        else if (annotationClass == ExtensionAPI.class) {
            return extensions().getOrDefault(componentScope, InjectingBindingHolder.EMPTY);
        }
        else if (annotationClass == TopicAPI.class) {
            return topics().getOrDefault(componentScope, InjectingBindingHolder.EMPTY);
        }
        else if (annotationClass == ActionAPI.class) {
            return actions();
        }
        throw new UnsupportedOperationException("Unknown annotation: " + annotationClass);
    }

    public void addBindings(NewInjectingBindingCollector collector) {
        addBindings(services(), collector.getServices());
        addBindings(extensions(), collector.getExtensions());
        addBindings(topics(), collector.getTopics());

        for (List<InjectingBinding> bindings : collector.getActions().getBindings().values()) {
            for (InjectingBinding binding : bindings) {
                ((InjectingBindingHolderImpl) actions()).addBinding(binding);
            }
        }
    }

    /**
     * Removes the bindings of a plugin unloaded at runtime.
     *
     * @return the removed bindings
     */
    public InjectingBindingLoader removeBindings(ClassLoader classLoader) {
        InjectingBindingHolderImpl removedActions = new InjectingBindingHolderImpl(new AtomicBoolean());
        for (List<InjectingBinding> bindings : ((InjectingBindingHolderImpl) actions()).removeBindings(classLoader).values()) {
            bindings.forEach(removedActions::addBinding);
        }

        return new InjectingBindingLoader(
            removeBindings(services(), classLoader),
            removeBindings(extensions(), classLoader),
            removeBindings(topics(), classLoader),
            removedActions
        );
    }

    private static Map<ComponentScope, InjectingBindingHolder> removeBindings(Map<ComponentScope, InjectingBindingHolder> source, ClassLoader classLoader) {
        Map<ComponentScope, InjectingBindingHolder> removed = new HashMap<>();
        for (Map.Entry<ComponentScope, InjectingBindingHolder> entry : source.entrySet()) {
            InjectingBindingHolderImpl holder = new InjectingBindingHolderImpl(new AtomicBoolean());
            for (List<InjectingBinding> bindings : ((InjectingBindingHolderImpl) entry.getValue()).removeBindings(classLoader).values()) {
                bindings.forEach(holder::addBinding);
            }
            if (!holder.getBindings().isEmpty()) {
                removed.put(entry.getKey(), holder);
            }
        }
        return removed;
    }

    private static void addBindings(Map<ComponentScope, InjectingBindingHolder> target, Map<ComponentScope, InjectingBindingHolder> source) {
        for (Map.Entry<ComponentScope, InjectingBindingHolder> entry : source.entrySet()) {
            InjectingBindingHolderImpl holder = (InjectingBindingHolderImpl) target.computeIfAbsent(
                entry.getKey(),
                scope -> new InjectingBindingHolderImpl(new AtomicBoolean())
            );

            for (List<InjectingBinding> bindings : entry.getValue().getBindings().values()) {
                for (InjectingBinding binding : bindings) {
                    holder.addBinding(binding);
                }
            }
        }
    }
}
