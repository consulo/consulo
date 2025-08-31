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

package consulo.ide.impl.idea.ui.popup;

import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.BalloonLayout;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author max
 */
public class NotificationPopup {
  public NotificationPopup(JComponent owner, JComponent content, Color background) {
    this(owner, content, background, true);
  }

  public NotificationPopup(JComponent owner, JComponent content, Color background, boolean useDefaultPreferredSize) {
    this(owner, content, background, useDefaultPreferredSize, null, false);
  }

  public NotificationPopup(JComponent owner, final JComponent content, Color background, final boolean useDefaultPreferredSize, ActionListener clickHandler, boolean closeOnClick) {
    IdeFrame frame = findFrame(owner);
    if (frame == null || !TargetAWT.to(frame.getWindow()).isShowing() || frame.getBalloonLayout() == null) {
      new FramelessNotificationPopup(owner, content, background, useDefaultPreferredSize, clickHandler);
    }
    else {
      Wrapper wrapper = new NonOpaquePanel(content) {
        @Override
        public Dimension getPreferredSize() {
          Dimension size = super.getPreferredSize();
          if (useDefaultPreferredSize) {
            if (size.width > 400 || size.height > 200) {
              size.width = 400;
              size.height = 200;
            }
          }
          return size;
        }
      };

      Balloon balloon = JBPopupFactory.getInstance().createBalloonBuilder(wrapper).setFadeoutTime(5000).setHideOnClickOutside(false).setHideOnFrameResize(false).setHideOnKeyOutside(false)
              .setCloseButtonEnabled(true).setFillColor(background).setShowCallout(false).setClickHandler(clickHandler, closeOnClick).createBalloon();

      BalloonLayout layout = frame.getBalloonLayout();
      assert layout != null;

      layout.add(balloon);
    }
  }

  @Nullable
  private static IdeFrame findFrame(JComponent owner) {
    Window frame = SwingUtilities.getWindowAncestor(owner);
    if(frame == null) {
      return null;
    }

    consulo.ui.Window uiWindow = TargetAWT.from(frame);
    return uiWindow.getUserData(IdeFrame.KEY);
  }

  public JBPopup getPopup() {
    return null;
  }
}
