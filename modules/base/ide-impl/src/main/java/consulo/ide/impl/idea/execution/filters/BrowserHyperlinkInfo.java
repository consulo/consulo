/*
 * Copyright 2000-2006 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.filters;

import consulo.ide.impl.idea.ide.BrowserUtil;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author Kir
 */
public class BrowserHyperlinkInfo implements HyperlinkInfo {
  private final String myUrl;

  public BrowserHyperlinkInfo(String url) {
    myUrl = url;
  }

  @RequiredUIAccess
  public void navigate(Project project) {
    openUrl(myUrl);
  }

  public static void openUrl(String url) {
    BrowserUtil.launchBrowser(url);
  }
}
