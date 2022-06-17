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
import consulo.component.bind.InjectingBinding;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public class InjectingBindingHolder {
  private final Map<String, List<InjectingBinding>> myBindings = new HashMap<>();

  public InjectingBindingHolder(Class<?> componentAnnotation, ComponentScope componentScope) {
  }

  public void addBinding(InjectingBinding binding) {
    myBindings.computeIfAbsent(binding.getApiClassName(), s -> new LinkedList<>()).add(binding);
  }

  public Map<String, List<InjectingBinding>> getBindings() {
    return myBindings;
  }
}
