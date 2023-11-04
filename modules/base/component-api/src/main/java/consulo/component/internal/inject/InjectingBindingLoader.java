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

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public class InjectingBindingLoader extends BindingLoader<InjectingBinding> {
  private final Map<ComponentScope, InjectingBindingHolder> myServices = new HashMap<>();
  private final Map<ComponentScope, InjectingBindingHolder> myExtensions = new HashMap<>();
  private final Map<ComponentScope, InjectingBindingHolder> myTopics = new HashMap<>();
  private final InjectingBindingHolder myActions = new InjectingBindingHolder(myLocked);

  public InjectingBindingLoader() {
  }

  @Nonnull
  @Override
  protected Class<InjectingBinding> getBindingClass() {
    return InjectingBinding.class;
  }

  @Override
  protected void process(InjectingBinding binding) {
    getHolder(binding.getComponentAnnotationClass(), binding.getComponentScope()).addBinding(binding);
  }

  @Nonnull
  public InjectingBindingHolder getHolder(@Nonnull Class<?> annotationClass, @Nonnull ComponentScope componentScope) {
    if (annotationClass == ServiceAPI.class) {
      return myServices.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(myLocked));
    }
    else if (annotationClass == ExtensionAPI.class) {
      return myExtensions.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(myLocked));
    }
    else if (annotationClass == TopicAPI.class) {
      return myTopics.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(myLocked));
    }
    else if (annotationClass == ActionAPI.class) {
      return myActions;
    }

    throw new UnsupportedOperationException("Unknown annotation: " + annotationClass);
  }
}
