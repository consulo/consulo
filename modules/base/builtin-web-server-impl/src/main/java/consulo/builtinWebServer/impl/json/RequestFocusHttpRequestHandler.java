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
package consulo.builtinWebServer.impl.json;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.json.JsonGetRequestHandler;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
@ExtensionImpl
public class RequestFocusHttpRequestHandler extends JsonGetRequestHandler {
  public RequestFocusHttpRequestHandler() {
    super("requestFocus");
  }

  @Nonnull
  @Override
  public JsonResponse handle(HttpRequest request) {
    final FocusableFrame frame = IdeFocusManager.findInstance().getLastFocusedFrame();
    if (frame != null) {
      frame.activate();
      return JsonResponse.asSuccess(null);
    }
    else {
      return JsonResponse.asError("No Frame");
    }
  }
}
