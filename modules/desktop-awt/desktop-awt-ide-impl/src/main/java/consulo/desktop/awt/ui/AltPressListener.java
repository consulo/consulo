/*
 * Copyright 2013-2024 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.concurrent.QueueProcessor;
import consulo.disposer.Disposable;
import consulo.ui.ex.awt.util.ComponentUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;

/**
 * @author VISTALL
 * @since 19-Jul-24
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class AltPressListener implements Disposable {
  private record RepaintMnemonicRequest(WeakReference<Component> focusOwnerRef, boolean pressed) {
  }

  private static volatile boolean ourAltPressed;

  public static boolean isAltPressed() {
    return ourAltPressed;
  }

  private QueueProcessor<RepaintMnemonicRequest> myRepaintRequests;

  @Inject
  public AltPressListener(Application application) {
    myRepaintRequests =
      new QueueProcessor<>((repaintMnemonicRequest, runnable) -> {
        process(repaintMnemonicRequest);
        runnable.run();
      }, true, QueueProcessor.ThreadToUse.UI, () -> application.isDisposeInProgress() || application.isDisposed());

    IdeEventQueue.getInstance().addPostEventListener(e -> {
      if (!(e instanceof KeyEvent keyEvent) || keyEvent.getKeyCode() != KeyEvent.VK_ALT) {
        return false;
      }

      boolean altPressed = e.getID() == KeyEvent.KEY_PRESSED;
      ourAltPressed = altPressed;

      if (application.isDisposeInProgress() || application.isDisposed()) {
        return false;
      }

      AltPressListener altPressListener = application.getInstance(AltPressListener.class);
      Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      altPressListener.myRepaintRequests.addFirst(new RepaintMnemonicRequest(new WeakReference<>(focusOwner), altPressed));

      return false;
    }, this);
  }

  private void process(RepaintMnemonicRequest request) {
    myRepaintRequests.clear();

    if (request.pressed() != ourAltPressed) {
      return;
    }

    Component focusOwner = request.focusOwnerRef().get();
    if (focusOwner == null) {
      return;
    }

    Window window = SwingUtilities.windowForComponent(focusOwner);
    if (window == null) {
      return;
    }

    for (Component component : window.getComponents()) {
      if (component instanceof JComponent jComponent) {
        for (Component c : ComponentUtil.findComponentsOfType(jComponent, JComponent.class)) {
          if (c instanceof JLabel label && label.getDisplayedMnemonicIndex() != -1 || c instanceof AbstractButton button && button.getDisplayedMnemonicIndex() != -1) {
            c.repaint();
          }
        }
      }
    }
  }

  @Override
  public void dispose() {
  }
}
