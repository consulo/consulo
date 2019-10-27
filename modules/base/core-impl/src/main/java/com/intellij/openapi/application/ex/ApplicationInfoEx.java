/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Sep 16, 2002
 * Time: 5:17:44 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.openapi.application.ex;

import com.intellij.openapi.application.ApplicationInfo;
import consulo.annotations.DeprecationInfo;
import consulo.application.ApplicationProperties;

import java.awt.*;

public abstract class ApplicationInfoEx extends ApplicationInfo {

  public static ApplicationInfoEx getInstanceEx() {
    return (ApplicationInfoEx)ApplicationInfo.getInstance();
  }

  @Deprecated
  @DeprecationInfo("Do not use this method. Use SandboxUtil.getAppIcon()")
  public String getIconUrl() {
    return getUrl("/icon32");
  }

  @Deprecated
  @DeprecationInfo("Do not use this method. Use SandboxUtil.getAppIcon()")
  public String getSmallIconUrl() {
    return getUrl("/icon16");
  }

  private static String getUrl(String prefix) {
    return (ApplicationProperties.isInSandbox() ? prefix + "-sandbox" : prefix) + ".png";
  }

  public abstract String getFullApplicationName();

  public abstract String getDocumentationUrl();

  public abstract String getSupportUrl();

  public abstract String getReleaseFeedbackUrl();

  public abstract String getStatisticsUrl();

  public abstract String getWebHelpUrl();

  public abstract String getWhatsNewUrl();

  public abstract String getWinKeymapUrl();

  public abstract String getMacKeymapUrl();
}
