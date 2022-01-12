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
package consulo.web.application;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
public interface WebApplication extends Application {
  @Nullable
  static WebApplication getInstance() {
    return (WebApplication)ApplicationManager.getApplication();
  }

  static void invokeOnCurrentSession(@RequiredUIAccess Runnable runnable) {
    WebApplication webApplication = WebApplication.getInstance();
    assert webApplication != null;
    WebSession currentSession = webApplication.getCurrentSession();
    UIAccess access = currentSession == null ? null : currentSession.getAccess();

    if (access == null) {
      return;
    }

    access.give(runnable);
  }

  @Nullable
  WebSession getCurrentSession();

  void setCurrentSession(@Nullable WebSession session);
}
