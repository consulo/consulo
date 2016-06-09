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
package consulo.web.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListener;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.client.util.Log;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class UIEntryPoint implements EntryPoint {
  @Override
  public void onModuleLoad() {
    Widget widget = GwtUIUtil.loadingPanel();

    RootPanel rootPanel = RootPanel.get();

    rootPanel.add(widget);

    final String consuloSessionId = Cookies.getCookie("ConsuloSessionId");
    if(consuloSessionId == null) {
      return;
    }

    String url = "ws://" + Window.Location.getHost() + "/ui";
    Websocket websocket = new Websocket(url);
    websocket.addListener(new WebsocketListener() {
      @Override
      public void onClose() {

      }

      @Override
      public void onMessage(String msg) {
        Log.log(consuloSessionId);
      }

      @Override
      public void onOpen() {

      }
    });
    websocket.open();
  }
}
