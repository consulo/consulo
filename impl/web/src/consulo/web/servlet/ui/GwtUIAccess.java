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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter;
import consulo.ui.RequiredUIThread;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.internal.WBaseGwtComponent;
import consulo.ui.internal.WGwtModalWindowImpl;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIServerEvent;
import consulo.web.gwtUI.shared.UIServerEventType;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TObjectProcedure;
import org.jetbrains.annotations.NotNull;

import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class GwtUIAccess extends UIAccess {
  @NotNull
  public static GwtUIAccess get() {
    return (GwtUIAccess)UIAccess.get();
  }

  private String myCookieId;
  private Session mySession;
  private Window myWindow;
  private boolean myDisposed;

  private TLongObjectHashMap<WBaseGwtComponent> myComponents = new TLongObjectHashMap<WBaseGwtComponent>();
  private List<WGwtModalWindowImpl> myModals = new ArrayList<WGwtModalWindowImpl>();

  public GwtUIAccess(String cookieId, Session session) {
    myCookieId = cookieId;
    mySession = session;
  }

  public String getCookieId() {
    return myCookieId;
  }

  public TLongObjectHashMap<WBaseGwtComponent> getComponents() {
    return myComponents;
  }

  public void setSession(Session session) {
    mySession = session;
  }

  public void setWindow(Window window) {
    myWindow = window;
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
    try {
      final ServerSerializationStreamWriter serverSerializationStreamWriter = new ServerSerializationStreamWriter(new SimpleSerializationPolicy());

      serverSerializationStreamWriter.writeObject(messageDto);
      return serverSerializationStreamWriter.toString();
    }
    catch (SerializationException e) {
      e.printStackTrace();
      throw new Error(e);
    }
  }

  public void repaint() {
    if (myDisposed) {
      return;
    }

    UIAccess.assertIsUIThread();

    final WBaseGwtComponent window = (WBaseGwtComponent)myWindow;

    List<UIComponent> components = new ArrayList<UIComponent>();

    window.visitChanges(components);

    if (!components.isEmpty()) {
      UIServerEvent serverEvent = new UIServerEvent();
      serverEvent.setSessionId(myCookieId);
      serverEvent.setType(UIServerEventType.stateChanged);
      serverEvent.setComponents(components);

      send(serverEvent);
    }
  }

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
    modalWindow.visitChanges(new ArrayList<UIComponent>());

    myModals.add(modalWindow);

    send(serverEvent);
  }

  public void dispose() {
    myDisposed = true;
    // free resources
    for (WGwtModalWindowImpl modal : myModals) {
      modal.disposeImpl();
    }

    myComponents.forEachValue(new TObjectProcedure<WBaseGwtComponent>() {
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

    myModals.clear();
    myComponents.clear();
  }
}
