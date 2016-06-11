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
package consulo.web.servlet.ui;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.intellij.util.containers.ConcurrentHashMap;
import consulo.ui.UIAccess;
import consulo.ui.internal.WGwtComponentImpl;
import consulo.web.gwtUI.shared.*;
import org.jetbrains.annotations.NotNull;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class UISessionManager {
  public static class UIContext extends UIAccess {
    private UIRoot myRoot;
    private Session mySession;
    private WGwtComponentImpl myComponent;

    private Map<String, WGwtComponentImpl> myComponents = new HashMap<String, WGwtComponentImpl>();

    public UIContext(UIRoot root, Session session) {
      myRoot = root;
      mySession = session;
    }

    public void setSession(Session session) {
      mySession = session;
    }

    public void setComponent(WGwtComponentImpl component) {
      myComponent = component;
    }

    public Session getSession() {
      return mySession;
    }

    @Override
    public void give(@NotNull Runnable runnable) {
      UIAccessHelper.ourInstance.run(this, runnable);
    }

    public void send(AutoBean<UIServerEvent> bean) {
      if (!UIAccessHelper.ourInstance.isUIThread()) {
        throw new IllegalArgumentException("Call must be wrapped inside UI thread");
      }

      final String json = AutoBeanJsonUtil.toJson(bean);
      try {
        mySession.getBasicRemote().sendText(json);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static final UISessionManager ourInstance = new UISessionManager();

  private Map<String, UIContext> myUIs = new ConcurrentHashMap<String, UIContext>();

  public void registerSession(String id, UIRoot uiRoot) {
    myUIs.put(id, new UIContext(uiRoot, null));
  }

  public void onSessionOpen(final Session session, final UIClientEvent clientEvent, final UIEventFactory factory) {
    final UIContext context = myUIs.get(clientEvent.getSessionId());
    if (context == null) {
      return;
    }

    context.setSession(session);

    UIAccessHelper.ourInstance.run(context, new Runnable() {
      @Override
      public void run() {
        WGwtComponentImpl component = (WGwtComponentImpl)context.myRoot.create(context);
        component.registerComponent(context.myComponents);

        context.setComponent(component);

        AutoBean<UIServerEvent> bean = factory.serverEvent();
        UIServerEvent serverEvent = bean.as();
        serverEvent.setSessionId(clientEvent.getSessionId());
        serverEvent.setType(UIServerEventType.createRoot);
        serverEvent.setComponents(Arrays.asList(component.convert(factory)));

        context.send(bean);
      }
    });
  }

  public void onInvokeEvent(Session session, final UIClientEvent clientEvent, UIEventFactory eventFactory) {
    final UIContext uiContext = myUIs.get(clientEvent.getSessionId());
    if (uiContext == null) {
      return;
    }

    UIAccessHelper.ourInstance.run(uiContext, new Runnable() {
      @Override
      public void run() {
        final String componentId = clientEvent.getVariables().get("componentId");

        final WGwtComponentImpl gwtComponent = uiContext.myComponents.get(componentId);
        if (gwtComponent != null) {
          gwtComponent.invokeListeners(clientEvent.getVariables());
        }
      }
    });
  }
}
