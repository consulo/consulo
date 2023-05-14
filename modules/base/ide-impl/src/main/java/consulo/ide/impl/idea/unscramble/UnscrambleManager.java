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

package consulo.ide.impl.idea.unscramble;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationPropertiesComponent;
import consulo.component.messagebus.MessageBusConnection;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 01-Nov-17
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class UnscrambleManager {
  private static final String KEY = "analyze.exceptions.on.the.fly";

  @Nonnull
  public static UnscrambleManager getInstance() {
    return ApplicationManager.getApplication().getComponent(UnscrambleManager.class);
  }

  @Nullable
  private MessageBusConnection myConnection;

  private final StacktraceAnalyzerListener myListener = new StacktraceAnalyzerListener();

  public UnscrambleManager() {
    updateConnection();
  }

  public boolean isEnabled() {
    return ApplicationPropertiesComponent.getInstance().getBoolean(KEY, false);
  }

  public void setEnabled(boolean enabled) {
    ApplicationPropertiesComponent.getInstance().setValue(KEY, enabled, false);

    updateConnection();
  }

  private void updateConnection() {
    boolean value = ApplicationPropertiesComponent.getInstance().getBoolean(KEY);
    if (value) {
      myConnection = Application.get().getMessageBus().connect();
      myConnection.subscribe(ApplicationActivationListener.class, myListener);
    }
    else {
      if (myConnection != null) {
        myConnection.disconnect();
      }
    }
  }
}
