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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface Layout extends Component {
  @RequiredUIAccess
  default void removeAll() {
    throw new AbstractMethodError(getClass().getName());
  }

  default void remove(@Nonnull Component component) {
    throw new AbstractMethodError(getClass().getName());
  }

  @RequiredUIAccess
  @Override
  default void setEnabledRecursive(boolean value) {
    setEnabled(value);

    forEachChild(component -> component.setEnabledRecursive(value));
  }

  default void forEachChild(@RequiredUIAccess @Nonnull Consumer<Component> consumer) {
    throw new AbstractMethodError(getClass().getName());
  }
}
