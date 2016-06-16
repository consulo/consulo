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
package consulo.web.gwtUI.client.ui;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.UIObject;
import consulo.ui.shared.Size;

import java.io.Serializable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DefaultVariables {
  public static void updateState(Map<String, Serializable> map, InternalGwtComponent component) {
    UIObject widget = component.asWidget();
    if (widget == null) {
      if (component instanceof UIObject) {
        widget = (UIObject)component;
      }
    }

    if (widget == null) {
      Window.alert("UIObject is null: " + component.getClass().getName());
      return;
    }

    if (widget instanceof HasEnabled) {
      ((HasEnabled)widget).setEnabled(parseBoolAsTrue(map, "enabled"));
    }

    if(!(widget instanceof GwtModalWindowImpl)) {
      widget.setVisible(parseBoolAsTrue(map, "visible"));
    }

    final Size size = (Size)map.get("size");
    if(size != null) {
      final int height = size.getHeight();
      if(height != -1) {
        widget.setHeight(height + "px");
      }

      final int width = size.getWidth();
      if(width != -1) {
        widget.setWidth(width + "px");
      }
    }
  }

  public static boolean parseBoolAsTrue(Map<String, Serializable> map, String key) {
    final Boolean temp = (Boolean)map.get(key);
    return temp == null || temp;
  }
}
