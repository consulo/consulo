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

import com.google.gwt.user.client.rpc.AsyncCallback;
import consulo.ui.shared.RGBColor;
import consulo.ui.shared.Size;
import consulo.web.gwtUI.shared.UIClientEvent;
import consulo.web.gwtUI.shared.UIServerEvent;

public interface HackServiceAsync {

  void clientEvent(UIClientEvent event, AsyncCallback<UIClientEvent> async);

  void serverEvent(UIServerEvent event, AsyncCallback<UIServerEvent> async);

  void size(Size size, AsyncCallback<Size> async);

  void rgbColor(RGBColor size, AsyncCallback<RGBColor> async);
}
