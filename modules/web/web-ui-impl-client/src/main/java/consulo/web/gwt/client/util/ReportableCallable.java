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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * @author VISTALL
 * @since 19-May-16
 */
public abstract class ReportableCallable<T> implements AsyncCallback<T> {
  @Override
  public void onFailure(Throwable caught) {
    DecoratedPopupPanel popupPanel = new DecoratedPopupPanel(true);

    popupPanel.setWidget(new Label("Connection problem"));

    popupPanel.setPopupPosition(Window.getClientWidth() / 2, 0);
    popupPanel.show();
  }
}
