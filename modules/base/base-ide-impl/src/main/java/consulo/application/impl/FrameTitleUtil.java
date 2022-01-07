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
package consulo.application.impl;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.text.StringUtil;
import consulo.application.ApplicationProperties;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.platform.Platform;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Sep-16
 */
public class FrameTitleUtil {
  @Nonnull
  public static String buildTitle() {
    ApplicationInfo applicationInfo = ApplicationInfo.getInstance();

    StringBuilder builder = new StringBuilder(applicationInfo.getName());
    builder.append(' ');
    builder.append(applicationInfo.getMajorVersion());
    builder.append('.');
    builder.append(applicationInfo.getMinorVersion());

    UpdateChannel channel = UpdateSettings.getInstance().getChannel();
    if (channel != UpdateChannel.release) {
      BuildNumber buildNumber = applicationInfo.getBuild();

      builder.append(" #");
      builder.append(buildNumber);
      builder.append(' ');
      builder.append(StringUtil.capitalize(channel.name()));
    }

    if (Platform.current().user().superUser()) {
      builder.append(" (Administrator)");
    }

    if(ApplicationProperties.isInSandbox()) {
      String jdkModuleMain = Platform.current().jvm().getRuntimeProperty("jdk.module.main");
      if (!StringUtil.isEmptyOrSpaces(jdkModuleMain)) {
        builder.append(" [Jigsaw]");
      }
    }

    return builder.toString();
  }
}
