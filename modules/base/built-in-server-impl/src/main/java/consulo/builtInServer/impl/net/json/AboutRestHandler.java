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
package consulo.builtInServer.impl.net.json;

import com.intellij.openapi.application.ApplicationInfo;
import consulo.builtInServer.json.JsonGetRequestHandler;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;

import javax.annotation.Nonnull;

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

  @Nonnull
  @Override
  public JsonResponse handle() {
    ApplicationInfo info = ApplicationInfo.getInstance();
    AboutInfo data = new AboutInfo();
    data.name = info.getName();
    data.build = info.getBuild().getBuildNumber();
    data.channel = UpdateSettings.getInstance().getChannel();
    return JsonResponse.asSuccess(data);
  }
}
