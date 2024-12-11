/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
 * User: yole
 * Date: 25.07.2006
 * Time: 14:26:00
 */
package consulo.ide.impl.idea.ide.actions;

import consulo.webBrowser.BrowserUtil;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.internal.ApplicationInfo;
import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

public class SendFeedbackAction extends AnAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    launchBrowser();
  }

  public static void launchBrowser() {
    final ApplicationInfo appInfo = ApplicationInfo.getInstance();
    String urlTemplate = appInfo.getSupportUrl();
    urlTemplate = urlTemplate
      .replace("$BUILD", appInfo.getBuild().asString())
      .replace("$TIMEZONE", System.getProperty("user.timezone"))
      .replace("$EVAL", "false");
    BrowserUtil.browse(urlTemplate);
  }
}
