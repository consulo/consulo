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

import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.impl.BorderInfo;
import consulo.ui.impl.SomeUIWrapper;
import consulo.ui.impl.UIDataObject;
import consulo.awt.internal.SwingComponentWrapper;
import consulo.ui.shared.Size;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public interface SwingWrapper extends SomeUIWrapper, SwingComponentWrapper {
  @Nullable
  @Override
  default Component getParentComponent() {
    Container container = (Container)this;
    return (Component)container.getParent();
  }

  @RequiredUIAccess
  @Override
  default void setSize(@Nonnull Size size) {
    Container container = (Container)this;
    container.setPreferredSize(TargetAWT.to(size));
  }

  @Nonnull
  @Override
  default java.awt.Component toAWTComponent() {
    return (java.awt.Component)this;
  }

  @Nonnull
  @Override
  default UIDataObject dataObject() {
    javax.swing.JComponent component = (javax.swing.JComponent)toAWTComponent();
    UIDataObject dataObject = (UIDataObject)component.getClientProperty(UIDataObject.class);
    if (dataObject == null) {
      component.putClientProperty(UIDataObject.class, dataObject = new UIDataObject());
    }
    return dataObject;
  }

  @Override
  default void bordersChanged() {
    JComponent component = (JComponent)toAWTComponent();

    component.setBorder(null);

    Collection<BorderInfo> borders = dataObject().getBorders();

    Map<BorderPosition, Integer> emptyBorders = new LinkedHashMap<>();
    for (BorderInfo border : borders) {
      if (border.getBorderStyle() == BorderStyle.EMPTY) {
        emptyBorders.put(border.getBorderPosition(), border.getWidth());
      }
    }

    if (!emptyBorders.isEmpty()) {
      component.setBorder(new EmptyBorder(getBorderSize(emptyBorders, BorderPosition.TOP), getBorderSize(emptyBorders, BorderPosition.LEFT), getBorderSize(emptyBorders, BorderPosition.BOTTOM),
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

  @Override
  default void dispose() {
  }
}
