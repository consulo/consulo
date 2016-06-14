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
package consulo.web.gwtUI.client;

import com.google.gwt.user.client.Window;
import consulo.web.gwtUI.client.ui.*;
import consulo.web.gwtUI.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIConverter {
  interface Factory {
    @NotNull
    InternalGwtComponent create();
  }

  private static Map<String, Factory> ourMap = new HashMap<String, Factory>();

  static {
    ourMap.put("consulo.ui.internal.WGwtCheckBoxImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtCheckBoxImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtDockLayoutImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtDockLayoutImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtVerticalLayoutImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtVerticalLayoutImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtComboBoxImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtComboBoxImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtHorizontalLayoutImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtHorizontalLayoutImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtLabelImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtLabelImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtHtmlLabelImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtHtmlLabelImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtImageImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtImageImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtLayeredImageImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtLayeredImageImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtHorizontalSplitLayoutImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtHorizontalSplitLayoutImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtTabbedLayoutImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtTabbedLayoutImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtWindowImpl", new Factory() {
      @NotNull
      @Override
      public InternalGwtComponent create() {
        return new GwtWindowImpl();
      }
    });
  }

  private static Map<Long, InternalGwtComponent> ourCache = new HashMap<Long, InternalGwtComponent>();

  public static InternalGwtComponent create(WebSocketProxy proxy, UIComponent uiComponent) {
    final String type = uiComponent.getType();
    Factory factory = ourMap.get(type);
    if (factory == null) {
      Window.alert("Type " + type + " is not resolved");
      return null;
    }

    final InternalGwtComponent widget = factory.create();

    ourCache.put(uiComponent.getId(), widget);

    final Map<String, String> variables = uiComponent.getVariables();

    if (widget instanceof InternalGwtComponentWithChildren) {
      final List<UIComponent.Child> children = uiComponent.getChildren();
      ((InternalGwtComponentWithChildren)widget).addChildren(proxy, children == null ? Collections.<UIComponent.Child>emptyList() : children);
    }

    widget.updateState(variables == null ? Collections.<String, String>emptyMap() : variables);
    if (widget instanceof InternalGwtComponentWithListeners) {
      ((InternalGwtComponentWithListeners)widget).setupListeners(proxy, uiComponent.getId());
    }
    return widget;
  }

  public static InternalGwtComponent get(long id) {
    return ourCache.get(id);
  }
}
