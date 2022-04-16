/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.awt.internal;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import consulo.ui.ex.awt.TabbedPaneWrapper;

import javax.swing.*;
import java.awt.*;

public class TabbedPaneHolder extends JPanel {
  private final TabbedPaneWrapper myWrapper;

  public TabbedPaneHolder(TabbedPaneWrapper wrapper) {
    super(new BorderLayout());
    myWrapper = wrapper;
  }

  @Override
  public boolean requestDefaultFocus() {
    final JComponent preferredFocusedComponent = IdeFocusTraversalPolicy.getPreferredFocusedComponent(myWrapper.getTabbedPane().getComponent());
    if (preferredFocusedComponent != null) {
      if (!preferredFocusedComponent.requestFocusInWindow()) {
        IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(preferredFocusedComponent);
      }
      return true;
    }
    else {
      return super.requestDefaultFocus();
    }
  }

  @Override
  public final void requestFocus() {
    requestDefaultFocus();
  }

  @Override
  public final boolean requestFocusInWindow() {
    return requestDefaultFocus();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (myWrapper != null) {
      myWrapper.getTabbedPane().updateUI();
    }
  }
}
