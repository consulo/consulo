/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.editor.impl;

import consulo.application.Application;
import consulo.ui.UIAccessScheduler;
import jakarta.annotation.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class RepaintCursorCommand implements Runnable {
  private long mySleepTime = 500;
  private boolean myIsBlinkCaret = true;

  @Nullable
  protected DesktopEditorImpl myEditor;
  protected ScheduledFuture<?> mySchedulerHandle;

  public void start() {
    if (mySchedulerHandle != null) {
      mySchedulerHandle.cancel(false);
    }

    UIAccessScheduler scheduler = Application.get().getLastUIAccess().getScheduler();
    mySchedulerHandle = scheduler.scheduleWithFixedDelay(this, mySleepTime, mySleepTime, TimeUnit.MILLISECONDS);
  }

  protected void setBlinkPeriod(int blinkPeriod) {
    mySleepTime = Math.max(blinkPeriod, 10);
    start();
  }

  protected void setBlinkCaret(boolean value) {
    myIsBlinkCaret = value;
  }

  @Override
  public void run() {
    if (myEditor != null) {
      DesktopEditorImpl.CaretCursor activeCursor = myEditor.myCaretCursor;

      long time = System.currentTimeMillis();
      time -= activeCursor.myStartTime;

      if (time > mySleepTime) {
        boolean toRepaint = true;
        if (myIsBlinkCaret) {
          activeCursor.myIsShown = !activeCursor.myIsShown;
        }
        else {
          toRepaint = !activeCursor.myIsShown;
          activeCursor.myIsShown = true;
        }

        if (toRepaint) {
          activeCursor.repaint();
        }
      }
    }
  }
}
