/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.application.Application;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.logging.Logger;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Denis Fokin
 */
public class ApplicationActivationStateManager {

  private static final Logger LOG = Logger.getInstance(ApplicationActivationStateManager.class);

  private static AtomicLong requestToDeactivateTime = new AtomicLong(System.currentTimeMillis());

  private static ApplicationActivationStateManager instance = new ApplicationActivationStateManager();

  public static ApplicationActivationStateManager get() {
    return instance;
  }

  private ApplicationActivationStateManager() {
  }

  public enum State {
    ACTIVE,
    DEACTIVATED,
    DEACTIVATING;

    public boolean isInactive() {
      return !this.equals(ACTIVE);
    }

    public boolean isActive() {
      return this.equals(ACTIVE);
    }
  }

  private static State state = State.DEACTIVATED;

  public static State getState() {
    return state;
  }

  public static boolean updateState(final WindowEvent windowEvent) {

    final Application application = ApplicationManager.getApplication();
    if (!(application instanceof ApplicationEx)) return false;

    if (windowEvent.getID() == WindowEvent.WINDOW_ACTIVATED || windowEvent.getID() == WindowEvent.WINDOW_GAINED_FOCUS) {

      if (state.isInactive()) {
        Window window = windowEvent.getWindow();

        return setActive(application, window);
      }
    }
    else if (windowEvent.getID() == WindowEvent.WINDOW_DEACTIVATED && windowEvent.getOppositeWindow() == null) {
      requestToDeactivateTime.getAndSet(System.currentTimeMillis());

      // For stuff that cannot wait windowEvent notify about deactivation immediately
      if (state.isActive()) {

        IdeFrame ideFrame = getIdeFrameFromWindow(windowEvent.getWindow());
        if (ideFrame != null) {
          application.getMessageBus().syncPublisher(ApplicationActivationListener.class).applicationDeactivated(ideFrame);
        }
      }

      // We do not know for sure that application is going to be inactive,
      // windowEvent could just be showing a popup or another transient window.
      // So let's postpone the application deactivation for a while
      state = State.DEACTIVATING;
      LOG.debug("The app is in the deactivating state");

      Timer timer = new Timer(1500, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
          if (state.equals(State.DEACTIVATING)) {

            state = State.DEACTIVATED;
            LOG.debug("The app is in the deactivated state");

            IdeFrame ideFrame = getIdeFrameFromWindow(windowEvent.getWindow());
            if (ideFrame != null) {
              application.getMessageBus().syncPublisher(ApplicationActivationListener.class).delayedApplicationDeactivated(ideFrame);
            }
          }

        }
      });

      timer.setRepeats(false);
      timer.start();
      return true;
    }
    return false;
  }

  private static boolean setActive(Application application, Window window) {
    IdeFrame ideFrame = getIdeFrameFromWindow(window);

    state = State.ACTIVE;
    LOG.debug("The app is in the active state");

    if (ideFrame != null) {
      application.getMessageBus().syncPublisher(ApplicationActivationListener.class).applicationActivated(ideFrame);
      return true;
    }
    return false;
  }

  public static void updateState(Window window) {
    Application application = ApplicationManager.getApplication();
    if (!(application instanceof ApplicationEx)) return;

    if (state.isInactive() && window != null) {
      setActive(application, window);
    }
  }

  @Nullable
  private static IdeFrame getIdeFrameFromWindow(Window window) {
    Component frame = UIUtil.findUltimateParent(window);
    if (!(frame instanceof Window)) {
      return null;
    }

    consulo.ui.Window uiWindow = TargetAWT.from((Window)frame);
    return uiWindow.getUserData(IdeFrame.KEY);
  }
}
