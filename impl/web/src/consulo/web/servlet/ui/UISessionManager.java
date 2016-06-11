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
import consulo.ui.internal.WGwtComponentImpl;
import consulo.web.gwtUI.shared.*;

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
  public static class UIContext {
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
  }

  public static final UISessionManager INSTANCE = new UISessionManager();

  private Map<String, UIContext> myUIs = new ConcurrentHashMap<String, UIContext>();

  public void registerSession(String id, UIRoot uiRoot) {
    myUIs.put(id, new UIContext(uiRoot, null));
  }

  public void onSessionOpen(Session session, UIClientEvent clientEvent, UIEventFactory factory) {
    UIContext uiContext = myUIs.get(clientEvent.getSessionId());
    if (uiContext == null) {
      return;
    }

    uiContext.setSession(session);

    WGwtComponentImpl component = (WGwtComponentImpl)uiContext.myRoot.create();
    component.registerComponent(uiContext.myComponents);

    uiContext.setComponent(component);

    AutoBean<UIServerEvent> bean = factory.serverEvent();
    UIServerEvent serverEvent = bean.as();
    serverEvent.setSessionId(clientEvent.getSessionId());
    serverEvent.setType(UIServerEventType.createRoot);
    serverEvent.setComponents(Arrays.asList(component.convert(factory)));

    try {
      final String json = AutoBeanJsonUtil.toJson(bean);
      session.getBasicRemote().sendText(json);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void onInvokeEvent(Session session, UIClientEvent clientEvent, UIEventFactory eventFactory) {
    UIContext uiContext = myUIs.get(clientEvent.getSessionId());
    if (uiContext == null) {
      return;
    }

    final String componentId = clientEvent.getVariables().get("componentId");

    final WGwtComponentImpl gwtComponent = uiContext.myComponents.get(componentId);
    if(gwtComponent != null) {
      gwtComponent.invokeListeners(clientEvent.getVariables());
    }
  }
}
