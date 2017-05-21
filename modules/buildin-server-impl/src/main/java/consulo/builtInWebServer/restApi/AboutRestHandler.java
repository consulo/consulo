/*
 * Copyright 2013-2017 consulo.io
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
package consulo.builtInWebServer.restApi;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import consulo.buildInWebServer.api.JsonGetRequestHandler;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 06-May-17
 */
public class AboutRestHandler extends JsonGetRequestHandler {
  private static class AboutInfo {
    String name;
    int build;
    UpdateChannel channel;
  }

  public AboutRestHandler() {
    super("about");
  }

  @NotNull
  @Override
  public JsonResponse handle() {
    ApplicationInfoEx applicationInfoEx = ApplicationInfoEx.getInstanceEx();
    AboutInfo data = new AboutInfo();
    data.name = ApplicationNamesInfo.getInstance().getFullProductName();
    data.build = applicationInfoEx.getBuild().getBuildNumber();
    data.build = applicationInfoEx.getBuild().getBuildNumber();
    data.channel = UpdateSettings.getInstance().getChannel();
    return JsonResponse.asSuccess(data);
  }
}
