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
import consulo.ui.impl.UIDataObject;
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

  @NotNull
  @Override
  default <T extends EventListener> Runnable addListener(@NotNull Class<T> eventClass, @NotNull T listener) {
    AbstractComponent component = (AbstractComponent)toVaadin();

    UIDataObject data = (UIDataObject)component.getData();
    if (data == null) {
      component.setData(data = new UIDataObject());
    }
    return data.addListener(eventClass, listener);
  }

  @NotNull
  @Override
  default <T extends EventListener> T getListenerDispatcher(@NotNull Class<T> eventClass) {
    AbstractComponent component = (AbstractComponent)toVaadin();
    UIDataObject data = (UIDataObject)component.getData();
    if (data == null) {
      component.setData(data = new UIDataObject());
    }
    return data.getDispatcher(eventClass);
  }
}
