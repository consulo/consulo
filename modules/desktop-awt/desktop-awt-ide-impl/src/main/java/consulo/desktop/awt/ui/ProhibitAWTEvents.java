/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.ui;

import consulo.application.AccessToken;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

/**
 * Use to assert that no AWT events are pumped during some activity (e.g. action update, write operations, etc)
 *
 * @author peter
 */
public class ProhibitAWTEvents implements Predicate<AWTEvent> {
  private static final Logger LOG = Logger.getInstance(ProhibitAWTEvents.class);

  private final String myActivityName;
  private boolean myReported;

  private ProhibitAWTEvents(@Nonnull String activityName) {
    myActivityName = activityName;
  }

  @Override
  public boolean test(@Nonnull AWTEvent e) {
    if (!myReported) {
      myReported = true;
      LOG.error("AWT events are prohibited inside " + myActivityName + "; got " + e);
    }
    return true;
  }

  @Nonnull
  public static AccessToken start(@Nonnull String activityName) {
    if (!SwingUtilities.isEventDispatchThread()) {
      // some crazy highlighting queries getData outside EDT: https://youtrack.jetbrains.com/issue/IDEA-162970
      return AccessToken.EMPTY_ACCESS_TOKEN;
    }
    ProhibitAWTEvents dispatcher = new ProhibitAWTEvents(activityName);
    IdeEventQueue.getInstance().addPostprocessor(dispatcher, null);
    return new AccessToken() {
      @Override
      public void finish() {
        IdeEventQueue.getInstance().removePostprocessor(dispatcher);
      }
    };
  }
}
