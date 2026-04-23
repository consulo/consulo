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
package consulo.component.internal.inject;

import consulo.annotation.component.*;
import consulo.component.bind.InjectingBinding;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2026-04-23
 */
public class NewInjectingBindingCollector extends NewBindingCollector<InjectingBinding> {
    private final Map<ComponentScope, InjectingBindingHolder> myServices = new ConcurrentHashMap<>();
    private final Map<ComponentScope, InjectingBindingHolder> myExtensions = new ConcurrentHashMap<>();
    private final Map<ComponentScope, InjectingBindingHolder> myTopics = new ConcurrentHashMap<>();

    private AtomicBoolean myLocked = new AtomicBoolean();

    private final InjectingBindingHolder myActions = new InjectingBindingHolderImpl(myLocked);

    @Override
    protected Class<InjectingBinding> getBindingClass() {
        return InjectingBinding.class;
    }

    @Override
    protected void process(InjectingBinding binding) {
        InjectingBindingHolderImpl holder =
            (InjectingBindingHolderImpl) getHolder(binding.getComponentAnnotationClass(), binding.getComponentScope());

        holder.addBinding(binding);
    }

    public Map<ComponentScope, InjectingBindingHolder> getServices() {
        return myServices;
    }

    public Map<ComponentScope, InjectingBindingHolder> getExtensions() {
        return myExtensions;
    }

    public Map<ComponentScope, InjectingBindingHolder> getTopics() {
        return myTopics;
    }

    public InjectingBindingHolder getActions() {
        return myActions;
    }

    public InjectingBindingHolder getHolder(Class<?> annotationClass, ComponentScope componentScope) {
        if (annotationClass == ServiceAPI.class) {
            return myServices.computeIfAbsent(componentScope, c -> new InjectingBindingHolderImpl(myLocked));
        }
        else if (annotationClass == ExtensionAPI.class) {
            return myExtensions.computeIfAbsent(componentScope, c -> new InjectingBindingHolderImpl(myLocked));
        }
        else if (annotationClass == TopicAPI.class) {
            return myTopics.computeIfAbsent(componentScope, c -> new InjectingBindingHolderImpl(myLocked));
        }
        else if (annotationClass == ActionAPI.class) {
            return myActions;
        }

        throw new UnsupportedOperationException("Unknown annotation: " + annotationClass);
    }
}
