/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.util.EventDispatcher;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.util.EventListener;

/**
 * @author pegov
 */
public class AltStateManager implements AWTEventListener {
  public interface AltListener extends EventListener {
    void altPressed();

    void altReleased();
  }

  private static final AltStateManager ourInstance;

  static {
    ourInstance = new AltStateManager();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      Toolkit.getDefaultToolkit().addAWTEventListener(ourInstance, AWTEvent.KEY_EVENT_MASK);
    }
  }

  @Nonnull
  public static AltStateManager getInstance() {
    return ourInstance;
  }

  private final EventDispatcher<AltListener> myDispatcher = EventDispatcher.create(AltListener.class);

  private AltStateManager() {
  }

  public void addListener(AltListener listener) {
    myDispatcher.addListener(listener);
  }

  public void removeListener(AltListener listener) {
    myDispatcher.removeListener(listener);
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    KeyEvent keyEvent = (KeyEvent)event;
    if ((keyEvent.getKeyCode() == KeyEvent.VK_ALT)) {
      if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
        firePressed();
      }
      else if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
        fireReleased();
      }
    }
  }

  private void fireReleased() {
    myDispatcher.getMulticaster().altReleased();
  }

  private void firePressed() {
    myDispatcher.getMulticaster().altPressed();
  }
}
