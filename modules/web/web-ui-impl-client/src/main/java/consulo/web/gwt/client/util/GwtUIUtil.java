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
package consulo.web.gwt.client.util;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ui.AbstractComponentContainerConnector;
import com.vaadin.shared.Connector;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 20-May-16
 */
public class GwtUIUtil {
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends Widget> T getParentOf(Widget widget, Class<T> type) {
    Widget target = widget;

    do {
      if (target.getClass() == type) {
        return (T)target;
      }

      target = target.getParent();
    }
    while (target != null);

    return null;
  }

  @Contract("null -> null")
  public static Widget connector2Widget(@Nullable Connector connector) {
    if (connector == null) {
      return null;
    }
    ComponentConnector componentConnector = (ComponentConnector)connector;
    return componentConnector.getWidget();
  }

  @Nonnull
  public static List<Widget> remapWidgets(AbstractComponentContainerConnector abstractLayout) {
    List<Widget> widgets = new ArrayList<>();
    for (ComponentConnector connector : abstractLayout.getChildComponents()) {
      widgets.add(connector.getWidget());
    }

    return widgets;
  }

  public static Widget getWidget(Element element) {
    EventListener listener = DOM.getEventListener(element);

    if (listener == null) {
      return null;
    }
    if (listener instanceof Widget) {
      return (Widget)listener;
    }
    return null;
  }

  public static <T extends UIObject> T fillAndReturn(T object) {
    object.setWidth("100%");
    object.setHeight("100%");
    return object;
  }

  public static void fill(UIObject object) {
    object.setWidth("100%");
    object.setHeight("100%");
  }
}
