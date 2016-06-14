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
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import com.intellij.openapi.util.Factory;
import com.intellij.util.containers.ConcurrentHashMap;
import consulo.ui.Component;
import consulo.ui.RequiredUIThread;
import consulo.ui.UIAccess;
import consulo.ui.internal.WBaseGwtComponent;
import consulo.web.gwtUI.shared.*;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TObjectProcedure;
import org.jetbrains.annotations.NotNull;

import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class UISessionManager {
  public static class UIContext extends UIAccess {
    private String myId;
    private Factory<Component> myUIFactory;
    private Session mySession;
    private WBaseGwtComponent myRootComponent;

    private TLongObjectHashMap<WBaseGwtComponent> myComponents = new TLongObjectHashMap<WBaseGwtComponent>();

    public UIContext(String id, Factory<Component> UIFactory, Session session) {
      myId = id;
      myUIFactory = UIFactory;
      mySession = session;
    }

    public String getId() {
      return myId;
    }

    public void setSession(Session session) {
      mySession = session;
    }

    public void setRootComponent(WBaseGwtComponent rootComponent) {
      myRootComponent = rootComponent;
    }

    public Session getSession() {
      return mySession;
    }

    @Override
    public void give(@RequiredUIThread @NotNull Runnable runnable) {
      UIAccessHelper.ourInstance.run(this, runnable);
    }

    /**
     * Must be called inside write executor
     */
    public void send(AutoBean<UIServerEvent> bean) {
      UIAccess.assertIsUIThread();

      final String json = AutoBeanJsonUtil.toJson(bean);
      try {
        mySession.getBasicRemote().sendText(json);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void repaint() {
      UIAccess.assertIsUIThread();

      final WBaseGwtComponent rootComponent = myRootComponent;

      List<UIComponent> components = new ArrayList<UIComponent>();

      rootComponent.visitChanges(components);

      if (!components.isEmpty()) {
        AutoBean<UIServerEvent> bean = ourEventFactory.serverEvent();
        UIServerEvent serverEvent = bean.as();
        serverEvent.setSessionId(myId);
        serverEvent.setType(UIServerEventType.stateChanged);
        serverEvent.setComponents(components);

        send(bean);
      }
    }
  }

  public static UIEventFactory ourEventFactory = AutoBeanFactorySource.create(UIEventFactory.class);
  public static final UISessionManager ourInstance = new UISessionManager();

  private Map<String, UIContext> myUIs = new ConcurrentHashMap<String, UIContext>();

  private Map<String, UIContext> myTempSessions = new ConcurrentHashMap<String, UIContext>();

  public void registerInitialSession(String id, Factory<Component> uiRoot) {
    myTempSessions.put(id, new UIContext(id, uiRoot, null));
  }

  public void onSessionOpen(final Session session, final UIClientEvent clientEvent) {
    // when websocket come to use - remove it from temp sessions, and register it as default
    final UIContext context = myTempSessions.remove(clientEvent.getSessionId());
    if (context == null) {
      return;
    }

    myUIs.put(session.getId(), context);

    context.setSession(session);

    UIAccessHelper.ourInstance.run(context, new Runnable() {
      @Override
      public void run() {
        final WBaseGwtComponent component = (WBaseGwtComponent)context.myUIFactory.create();
        component.registerComponent(context.myComponents);

        context.setRootComponent(component);

        AutoBean<UIServerEvent> bean = ourEventFactory.serverEvent();
        UIServerEvent serverEvent = bean.as();
        serverEvent.setSessionId(clientEvent.getSessionId());
        serverEvent.setType(UIServerEventType.createRoot);
        serverEvent.setComponents(Arrays.asList(component.convert(ourEventFactory)));

        // we don't interest in first states - because they will send anyway to client
        component.visitChanges(new ArrayList<UIComponent>());

        context.send(bean);
      }
    });
  }

  public void onClose(Session session) {
    final UIContext uiContext = myUIs.remove(session.getId());
    if (uiContext == null) {
      return;
    }

    uiContext.myComponents.forEachValue(new TObjectProcedure<WBaseGwtComponent>() {
      @Override
      public boolean execute(WBaseGwtComponent object) {
        try {
          object.dispose();
        }
        catch (Exception e) {
          e.printStackTrace();
        }

        return true;
      }
    });
  }

  public void onInvokeEvent(Session session, final UIClientEvent clientEvent) {
    final UIContext uiContext = myUIs.get(session.getId());
    if (uiContext == null) {
      return;
    }

    UIAccessHelper.ourInstance.run(uiContext, new Runnable() {
      @Override
      public void run() {
        final Map<String, String> variables = clientEvent.getVariables();

        final long componentId = Long.parseLong(variables.get("componentId"));
        final String type = variables.get("type");

        final WBaseGwtComponent gwtComponent = uiContext.myComponents.get(componentId);
        if (gwtComponent != null) {
          gwtComponent.invokeListeners(type, variables);
        }
      }
    });
  }
}
