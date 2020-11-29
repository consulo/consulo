/*
 * Copyright 2013-2016 consulo.io
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

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public interface ValueComponent<V> extends Component {
  class ValueEvent<V> {
    private Component myComponent;
    private V myValue;

    public ValueEvent(Component component, V value) {
      myComponent = component;
      myValue = value;
    }

    public V getValue() {
      return myValue;
    }

    public Component getComponent() {
      return myComponent;
    }
  }

  interface ValueListener<V> extends EventListener {
    @RequiredUIAccess
    void valueChanged(@Nonnull ValueEvent<V> event);
  }

  @Nonnull
  default Disposable addValueListener(@Nonnull ValueComponent.ValueListener<V> valueListener) {
    return addListener(ValueListener.class, valueListener);
  }

  @Nullable
  V getValue();

  @Nonnull
  default V getValueOrError() {
    return Objects.requireNonNull(getValue(), "value required");
  }

  @RequiredUIAccess
  default void setValue(V value) {
    setValue(value, true);
  }

  @RequiredUIAccess
  void setValue(V value, boolean fireListeners);

  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  default ValueComponent<V> withValue(@Nullable V value) {
    setValue(value);
    return this;
  }
}
