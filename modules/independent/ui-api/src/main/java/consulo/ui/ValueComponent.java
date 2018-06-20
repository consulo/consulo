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

import com.intellij.openapi.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;

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
  Disposable addValueListener(@Nonnull ValueComponent.ValueListener<V> valueListener);

  @Nullable
  V getValue();

  @RequiredUIAccess
  default void setValue(V value) {
    setValue(value, true);
  }

  @RequiredUIAccess
  void setValue(V value, boolean fireEvents);
}
