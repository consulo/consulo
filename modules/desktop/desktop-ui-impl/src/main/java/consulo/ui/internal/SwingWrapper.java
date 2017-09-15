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
package consulo.ui.internal;

import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.impl.UIDataObject;
import consulo.ui.migration.ToSwingWrapper;
import consulo.ui.style.ColorKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.EventListener;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public interface SwingWrapper extends Component, ToSwingWrapper {
  @Nullable
  @Override
  default Component getParentComponent() {
    Container container = (Container)this;
    return (Component)container.getParent();
  }

  @RequiredUIAccess
  @Override
  default void setSize(@NotNull Size size) {
    Container container = (Container)this;
    container.setPreferredSize(new Dimension(size.getWidth(), size.getHeight()));
  }

  @NotNull
  @Override
  default java.awt.Component toAWT() {
    return (java.awt.Component)this;
  }

  default UIDataObject dataObject() {
    javax.swing.JComponent component = (javax.swing.JComponent)toAWT();
    UIDataObject dataObject = (UIDataObject)component.getClientProperty(UIDataObject.class);
    if (dataObject == null) {
      component.putClientProperty(UIDataObject.class, dataObject = new UIDataObject());
    }
    return dataObject;
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
  
  @Override
  default void dispose() {
  }
}
