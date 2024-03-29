/*
 * Copyright 2013-2018 consulo.io
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
package consulo.web.internal.ui.base;

import consulo.ui.Component;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

/**
 * @author VISTALL
 * @since 2018-05-10
 */
public class TargetVaddin {
  @Contract("null -> null")
  public static com.vaadin.flow.component.Component to(@Nullable Component component) {
    if (component == null) {
      return null;
    }

    if (component instanceof ToVaddinComponentWrapper) {
      return ((ToVaddinComponentWrapper)component).toVaadinComponent();
    }

    throw new UnsupportedOperationException("Component " + component + " is not impl ToVaddinComponentWrapper");
  }

  @Contract("null -> null")
  public static Component from(@Nullable com.vaadin.flow.component.Component component) {
    if (component == null) {
      return null;
    }

    if (component instanceof FromVaadinComponentWrapper) {
      return ((FromVaadinComponentWrapper)component).toUIComponent();
    }

    throw new UnsupportedOperationException("Component " + component + " is not impl FromVaadinComponentWrapper");
  }
}
