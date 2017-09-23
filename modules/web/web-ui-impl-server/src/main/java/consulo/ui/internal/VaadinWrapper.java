/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.internal;

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.impl.UIDataObject;
import consulo.ui.style.ColorKey;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * @author VISTALL
 * @since 14-Sep-17
 */
public interface VaadinWrapper extends Component {
  default com.vaadin.ui.Component toVaadin() {
    return (com.vaadin.ui.Component)this;
  }

  @Override
  default public Component getParentComponent() {
    return (Component)toVaadin().getParent();
  }

  default UIDataObject dataObject() {
    AbstractComponent component = (AbstractComponent)toVaadin();
    UIDataObject data = (UIDataObject)component.getData();
    if (data == null) {
      component.setData(data = new UIDataObject());
    }
    return data;
  }

  @NotNull
  @Override
  default <T extends EventListener> Runnable addListener(@NotNull Class<T> eventClass, @NotNull T listener) {
    return dataObject().addListener(eventClass, listener);
  }

  @NotNull
  @Override
  default <T extends EventListener> T getListenerDispatcher(@NotNull Class<T> eventClass) {
    return dataObject().getDispatcher(eventClass);
  }

  @RequiredUIAccess
  @Override
  default void addBorder(@NotNull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    dataObject().addBorder(borderPosition, borderStyle, colorKey, width);
  }

  @RequiredUIAccess
  @Override
  default void removeBorder(@NotNull BorderPosition borderPosition) {
    dataObject().removeBorder(borderPosition);
  }
}
