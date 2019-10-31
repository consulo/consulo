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
package consulo.ui.desktop.internal.base;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import com.intellij.util.ui.JBUI;
import consulo.annotations.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.awt.impl.ToSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.event.KeyListener;
import consulo.ui.impl.BorderInfo;
import consulo.ui.impl.UIDataObject;
import consulo.ui.shared.Size;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;
import consulo.ui.style.ColorKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
@SuppressWarnings("deprecation")
public class SwingComponentDelegate<T extends java.awt.Component> implements Component, ToSwingComponentWrapper {
  @Deprecated
  @DeprecationInfo("Use #initialize() method")
  protected T myComponent;

  protected void initialize(T component) {
    myComponent = component;

    myComponent.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        getListenerDispatcher(KeyListener.class).keyPressed(new consulo.ui.event.KeyEvent(SwingComponentDelegate.this, e.getKeyCode()));
      }
    });
  }

  @Nonnull
  @Override
  public T toAWTComponent() {
    return myComponent;
  }

  @Override
  public boolean isVisible() {
    return myComponent.isVisible();
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    myComponent.setVisible(value);
  }

  @Override
  public boolean isEnabled() {
    return myComponent.isEnabled();
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    myComponent.setEnabled(value);
  }

  @Nullable
  @Override
  public Component getParent() {
    return TargetAWT.from(myComponent.getParent());
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    myComponent.setPreferredSize(TargetAWT.to(size));
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @javax.annotation.Nullable T value) {
    dataObject().putUserData(key, value);
  }

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return dataObject().getUserData(key);
  }

  @Override
  @Nonnull
  public Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
    return dataObject().addUserDataProvider(function);
  }

  @Nonnull
  @Override
  public <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @Nonnull T listener) {
    return dataObject().addListener(eventClass, listener);
  }

  @Nonnull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass) {
    return dataObject().getDispatcher(eventClass);
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, ColorKey colorKey, int width) {
    dataObject().addBorder(borderPosition, borderStyle, colorKey, width);

    bordersChanged();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@Nonnull BorderPosition borderPosition) {
    dataObject().removeBorder(borderPosition);

    bordersChanged();
  }

  private void bordersChanged() {
    JComponent component = (JComponent)toAWTComponent();

    component.setBorder(JBUI.Borders.empty());

    Collection<BorderInfo> borders = dataObject().getBorders();

    Map<BorderPosition, Integer> emptyBorders = new LinkedHashMap<>();
    for (BorderInfo border : borders) {
      if (border.getBorderStyle() == BorderStyle.EMPTY) {
        emptyBorders.put(border.getBorderPosition(), border.getWidth());
      }
    }

    if (!emptyBorders.isEmpty()) {
      component.setBorder(JBUI.Borders.empty(getBorderSize(emptyBorders, BorderPosition.TOP), getBorderSize(emptyBorders, BorderPosition.LEFT), getBorderSize(emptyBorders, BorderPosition.BOTTOM),
                                             getBorderSize(emptyBorders, BorderPosition.RIGHT)));

      return;
    }

    // FIXME [VISTALL] support other borders?
  }

  static int getBorderSize(Map<BorderPosition, Integer> map, BorderPosition position) {
    Integer width = map.get(position);
    if (width == null) {
      return 0;
    }
    return JBUI.scale(width);
  }

  @Nonnull
  protected UIDataObject dataObject() {
    javax.swing.JComponent component = (javax.swing.JComponent)toAWTComponent();
    UIDataObject dataObject = (UIDataObject)component.getClientProperty(UIDataObject.class);
    if (dataObject == null) {
      component.putClientProperty(UIDataObject.class, dataObject = new UIDataObject());
    }
    return dataObject;
  }
}
