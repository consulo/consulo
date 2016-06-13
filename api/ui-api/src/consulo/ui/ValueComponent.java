/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ui;

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public interface ValueComponent<V> extends Component {
  class ValueEvent<C, V> {
    private C myComponent;
    private V myValue;

    public ValueEvent(C component, V value) {
      myComponent = component;
      myValue = value;
    }

    public V getValue() {
      return myValue;
    }

    public C getComponent() {
      return myComponent;
    }
  }

  interface ValueListener<C, V> {
    @RequiredUIThread
    void valueChanged(@NotNull ValueEvent<C, V> event);
  }

  void addValueListener(@NotNull ValueComponent.ValueListener<CheckBox, V> valueListener);

  void removeValueListener(@NotNull ValueComponent.ValueListener<CheckBox, V> valueListener);

  @NotNull
  V getValue();

  @RequiredUIThread
  void setValue(@NotNull V value);
}
