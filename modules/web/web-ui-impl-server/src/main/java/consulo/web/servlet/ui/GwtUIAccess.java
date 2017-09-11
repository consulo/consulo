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
package consulo.web.servlet.ui;

import com.intellij.openapi.diagnostic.Logger;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.internal.WGwtBaseComponent;
import consulo.ui.internal.WGwtModalWindowImpl;
import consulo.ui.internal.WGwtWindowImpl;
import consulo.web.gwt.shared.UIComponent;
import consulo.web.gwt.shared.UIServerEvent;
import consulo.web.gwt.shared.UIServerEventType;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;

import javax.websocket.Session;
import java.io.IOException;
import java.util.*;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class GwtUIAccess extends UIAccess {
  private static final Logger LOGGER = Logger.getInstance(GwtUIAccess.class);

  @NotNull
  @RequiredUIAccess
  public static GwtUIAccess get() {
    return (GwtUIAccess)UIAccess.get();
  }

  private String myCookieId;
  private Session mySession;
  private boolean myDisposed;

  private TLongObjectHashMap<WGwtBaseComponent> myComponents = new TLongObjectHashMap<>();

  private Deque<WGwtWindowImpl> myWindows = new ArrayDeque<>();

  public GwtUIAccess(String cookieId, Session session) {
    myCookieId = cookieId;
    mySession = session;
  }

  public String getCookieId() {
    return myCookieId;
  }

  public TLongObjectHashMap<WGwtBaseComponent> getComponents() {
    return myComponents;
  }

  public void setSession(Session session) {
    mySession = session;
  }

  public void setWindow(Window window) {
    myWindows.add((WGwtWindowImpl)window);
  }

  public Session getSession() {
    return mySession;
  }

  @Override
  public void give(@RequiredUIAccess @NotNull Runnable runnable) {
    UIAccessHelper.ourInstance.run(this, runnable);
  }

  /**
   * Must be called inside write executor
   */
  @RequiredUIAccess
  public void send(UIServerEvent bean) {
    if (myDisposed) {
      return;
    }

    UIAccess.assertIsUIThread();

    try {
      mySession.getBasicRemote().sendText(encode(bean));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String encode(final UIServerEvent messageDto) {
    return null;
  }

  @RequiredUIAccess
  public void repaint() {
    if (myDisposed) {
      return;
    }

    UIAccess.assertIsUIThread();


    List<UIComponent> components = new ArrayList<>();

    for (WGwtWindowImpl window : myWindows) {
      window.visitChanges(components);
    }

    if (!components.isEmpty()) {
      UIServerEvent serverEvent = new UIServerEvent();
      serverEvent.setSessionId(myCookieId);
      serverEvent.setType(UIServerEventType.stateChanged);
      serverEvent.setComponents(components);

      send(serverEvent);
    }
  }

  @RequiredUIAccess
  public void showModal(WGwtModalWindowImpl modalWindow) {
    if (myDisposed) {
      return;
    }

    modalWindow.registerComponent(myComponents);

    UIServerEvent serverEvent = new UIServerEvent();
    serverEvent.setSessionId(myCookieId);
    serverEvent.setType(UIServerEventType.showModal);
    serverEvent.setComponents(Arrays.asList(modalWindow.convert()));

    // we don't interest in first states - because they will send anyway to client
    modalWindow.visitChanges(new ArrayList<>());

    myWindows.addLast(modalWindow);

    send(serverEvent);
  }

  public void dispose() {
    myDisposed = true;
    // free resources
    for (Window modal : myWindows) {
      if (modal instanceof WGwtModalWindowImpl) {
        ((WGwtModalWindowImpl)modal).disposeImpl();
      }
    }

    myComponents.forEachValue(object -> {
      try {
        object.dispose();
      }
      catch (Exception e) {
        LOGGER.error(e);
      }

      return true;
    });

    myWindows.clear();
    myComponents.clear();
  }
}
