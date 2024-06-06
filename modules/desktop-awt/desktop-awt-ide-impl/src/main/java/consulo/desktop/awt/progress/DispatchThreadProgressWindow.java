/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.desktop.awt.progress;

import consulo.application.ApplicationManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;

public class DispatchThreadProgressWindow extends ProgressWindow {
  private static final Logger LOG = Logger.getInstance(DispatchThreadProgressWindow.class);

  private long myLastPumpEventsTime = 0;
  private static final int PUMP_INTERVAL = Platform.current().os().isWindows() ? 100 : 500;
  private Runnable myRunnable;

  public DispatchThreadProgressWindow(boolean shouldShowCancel, Project project) {
    super(shouldShowCancel, project);
  }

  @Override
  public void setTextValue(LocalizeValue text) {
    super.setText2Value(text);
    pumpEvents();
  }

  @Override
  public void setFraction(double fraction) {
    super.setFraction(fraction);
    pumpEvents();
  }

  @Override
  public void setText2Value(LocalizeValue text) {
    super.setText2Value(text);
    pumpEvents();
  }

  private void pumpEvents() {
    long time = System.currentTimeMillis();
    if (time - myLastPumpEventsTime < PUMP_INTERVAL) return;
    myLastPumpEventsTime = time;

    IdeEventQueue.getInstance().flushQueue();
  }

  @Override
  protected void prepareShowDialog() {
    if (myRunnable != null) {
      ApplicationManager.getApplication().invokeLater(myRunnable, getModalityState());
    }
    showDialog();
  }

  public void setRunnable(final Runnable runnable) {
    myRunnable = runnable;
  }
}
