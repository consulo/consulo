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

import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ConcurrentHashMap;
import consulo.ui.internal.WBaseGwtComponent;
import consulo.ui.internal.WGwtWindowImpl;
import consulo.web.gwtUI.shared.UIClientEvent;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIServerEvent;
import consulo.web.gwtUI.shared.UIServerEventType;

import javax.websocket.Session;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class UISessionManager {

  public static final UISessionManager ourInstance = new UISessionManager();

  private Map<String, GwtUIAccess> myUIs = new ConcurrentHashMap<String, GwtUIAccess>();

  private Map<String, Class<? extends UIBuilder>> myTempSessions = new ConcurrentHashMap<String, Class<? extends UIBuilder>>();

  public void registerInitialSession(String id, Class<? extends UIBuilder> builderClass) {
    myTempSessions.put(id, builderClass);
  }

  public void onSessionOpen(final Session session, final UIClientEvent clientEvent) {
    // when websocket come to use - remove it from temp sessions, and register it as default
    final Class<? extends UIBuilder> builderClass = myTempSessions.remove(clientEvent.getSessionId());
    if (builderClass == null) {
      return;
    }

    final UIBuilder uiBuilder = ReflectionUtil.newInstance(builderClass);

    final GwtUIAccess uiAccess = new GwtUIAccess(clientEvent.getSessionId(), session);
    myUIs.put(session.getId(), uiAccess);

    uiAccess.setSession(session);

    UIAccessHelper.ourInstance.run(uiAccess, new Runnable() {
      @Override
      public void run() {
        WGwtWindowImpl window = new WGwtWindowImpl();

        uiBuilder.build(window);

        window.registerComponent(uiAccess.getComponents());

        uiAccess.setWindow(window);

        UIServerEvent serverEvent = new UIServerEvent();
        serverEvent.setSessionId(clientEvent.getSessionId());
        serverEvent.setType(UIServerEventType.createRoot);
        serverEvent.setComponents(Arrays.asList(window.convert()));

        // we don't interest in first states - because they will send anyway to client
        window.visitChanges(new ArrayList<UIComponent>());

        uiAccess.send(serverEvent);
      }
    });
  }

  public void onClose(Session session) {
    final GwtUIAccess uiAccess = myUIs.remove(session.getId());
    if (uiAccess == null) {
      return;
    }

    uiAccess.dispose();
  }

  public void onInvokeEvent(Session session, final UIClientEvent clientEvent) {
    final GwtUIAccess uiAccess = myUIs.get(session.getId());
    if (uiAccess == null) {
      return;
    }

    UIAccessHelper.ourInstance.run(uiAccess, new Runnable() {
      @Override
      public void run() {
        final Map<String, Serializable> variables = clientEvent.getVariables();

        final long componentId = (Long)variables.get("componentId");
        final String type = (String)variables.get("type");

        final WBaseGwtComponent gwtComponent = uiAccess.getComponents().get(componentId);
        if (gwtComponent != null) {
          gwtComponent.invokeListeners(type, variables);
        }
      }
    });
  }
}
